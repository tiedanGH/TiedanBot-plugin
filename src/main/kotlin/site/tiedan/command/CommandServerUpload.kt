package site.tiedan.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.*
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.masterOnly
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.ServerUploadConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.ZipException
import java.util.zip.ZipFile

object CommandServerUpload : RawCommand(
    owner = TiedanGame,
    primaryName = "server",
    description = "服务器文件上传指令",
    usage = "${commandPrefix}server help"
){
    private val commandList = listOf(
        Command("server lgtbot [force] [文件夹]", "server lgt [force] [文件夹]", "上传群文件至lgtbot", 1),
    )

    override suspend fun CommandSender.onCommand(args: MessageChain) {

        try {
            masterOnly(this)
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
            return
        }

        try {
            when (args[0].content) {

                "help", "帮助"-> {   // 查看文件上传可用帮助
                    val reply = " · 文件上传指令帮助（当前支持的上传目标：lgtbot）：\n" +
                            commandList.joinToString("") { "-> ${it.desc}\n${commandPrefix}${it.usage}\n" } +
                            "\n" +
                            "🔸上传需引用一条群文件消息，仅支持zip压缩包\n" +
                            "🔸压缩包：解压至lgtbot路径下的同名文件夹，存在重名文件夹时需添加force（删除旧文件夹后重新解压）\n" +
                            "🔸单文件：需添加force并指定文件夹名称，存在重名文件时直接替换"
                    sendQuoteReply(reply)
                }

                "lgtbot", "lgt"-> {   // 上传群文件至lgtbot路径
                    handleUpload(args)
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${commandPrefix}server help」来查看指令帮助")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}server help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    private suspend fun CommandSender.handleUpload(args: MessageChain) {
        // 检查lgtbot路径配置
        val basePath = ServerUploadConfig.lgtbot_path
        if (basePath.isEmpty()) {
            sendQuoteReply("[上传失败] 未配置lgtbot路径，请先在ServerUploadConfig中配置 lgtbot_path")
            return
        }
        val baseDir = File(basePath)
        if (!baseDir.isDirectory) {
            sendQuoteReply("[上传失败] lgtbot路径不存在或不是文件夹：$basePath")
            return
        }
        // 解析参数
        val force = args.getOrNull(1)?.content?.let { it == "force" || it == "f" } == true
        val folderName = args.getOrNull(2)?.content
        // 获取引用的群文件消息
        val quote = (this as? CommandSenderOnMessage<*>)?.fromEvent?.message?.get(QuoteReply.Key)
        if (quote == null) {
            sendQuoteReply("[上传失败] 未检测到引用消息，请引用一条群文件消息后执行本指令")
            return
        }
        val fileMessage = quote.source.originalMessage.firstIsInstanceOrNull<FileMessage>()
        if (fileMessage == null) {
            sendQuoteReply("[上传失败] 引用的消息不是群文件消息")
            return
        }
        val group = subject as? Group
        if (group == null) {
            sendQuoteReply("[上传失败] 仅支持在群聊中引用群文件")
            return
        }
        val fileName = fileMessage.name
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            sendQuoteReply("[上传失败] 文件名不合法：$fileName")
            return
        }

        // 上传前预校验
        val isZip = fileName.endsWith(".zip", ignoreCase = true)
        val targetDir: File
        if (isZip) {
            val baseName = fileName.dropLast(4)
            if (baseName.isEmpty()) {
                sendQuoteReply("[上传失败] 压缩包文件名不合法")
                return
            }
            targetDir = File(baseDir, baseName)
            if (targetDir.exists() && !force) {
                sendQuoteReply("[上传失败] lgtbot路径中已存在重名文件夹「$baseName」，如需覆盖请添加force参数")
                return
            }
        } else {
            if (!force) {
                sendQuoteReply("[上传失败] 仅支持上传zip压缩包。如需上传单文件，请添加force参数并指定目标文件夹名称")
                return
            }
            if (folderName.isNullOrEmpty()) {
                sendQuoteReply("[上传失败] 上传单文件时，force后需指定目标文件夹名称")
                return
            }
            if (folderName.contains("/") || folderName.contains("\\") || folderName.contains("..")) {
                sendQuoteReply("[上传失败] 文件夹名称不合法：$folderName")
                return
            }
            targetDir = File(baseDir, folderName)
            if (!targetDir.isDirectory) {
                sendQuoteReply("[上传失败] lgtbot路径中不存在文件夹「$folderName」，请检查文件夹名称")
                return
            }
        }

        // 获取群文件下载链接
        val url = fileMessage.toAbsoluteFile(group)?.getUrl()
        if (url == null) {
            sendQuoteReply("[上传失败] 无法获取群文件下载链接，文件可能已过期或被删除")
            return
        }
        // 下载至临时文件
        val tempFile = try {
            downloadToTemp(url)
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[上传失败] 下载群文件时发生错误：${e.message}")
            return
        }
        try {
            var replacedNote = ""
            if (isZip) {
                var structureNote = ""
                withContext(Dispatchers.IO) {
                    if (targetDir.exists()) {
                        targetDir.deleteRecursively()
                        replacedNote = "\n（已删除重名文件夹后重新解压）"
                    }
                    try {
                        extractZip(tempFile, targetDir)
                        structureNote = flattenSameNameFolder(targetDir)
                    } catch (e: Exception) {
                        targetDir.deleteRecursively()   // 清理解压失败的残留文件
                        throw e
                    }
                }
                sendQuoteReply("上传成功！压缩包「$fileName」已解压至：\n${targetDir.path}$replacedNote$structureNote")
            } else {
                val targetFile = File(targetDir, fileName)
                withContext(Dispatchers.IO) {
                    if (targetFile.exists()) {
                        replacedNote = "\n（已替换重名文件）"
                    }
                    tempFile.copyTo(targetFile, overwrite = true)
                }
                sendQuoteReply("上传成功！文件「$fileName」已保存至：\n${targetFile.path}$replacedNote")
            }
        } catch (e: SecurityException) {
            sendQuoteReply("[上传失败] ${e.message}")
        } catch (_: ZipException) {
            sendQuoteReply("[上传失败] 解压失败，文件可能已损坏或不是有效的zip压缩包")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[上传失败] 处理文件时发生错误：${e::class.simpleName}(${e.message})")
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun downloadToTemp(url: String): File = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"
        connection.connect()
        val file = File.createTempFile("lgtbot_upload_", ".tmp")
        try {
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            connection.disconnect()
        }
        file
    }

    /**
     * 解压zip文件（文件名优先UTF-8解码，失败时回退GBK）
     */
    private fun extractZip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        try {
            extractZipWithCharset(zipFile, targetDir, Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            // 文件名非UTF-8编码（常见于Windows创建的压缩包），使用GBK重试
            targetDir.deleteRecursively()
            targetDir.mkdirs()
            extractZipWithCharset(zipFile, targetDir, Charset.forName("GBK"))
        }
    }

    /**
     * 调整解压后的目录结构：
     * - 若解压结果中仅有一个与目标目录同名的文件夹，则将其内容提升至目标目录
     * - 若除同名文件夹外还包含其他文件或文件夹，保持原结构并返回警告信息
     */
    private fun flattenSameNameFolder(targetDir: File): String {
        val children = targetDir.listFiles() ?: return ""
        val sameNameDir = children.firstOrNull { it.isDirectory && it.name == targetDir.name } ?: return ""
        if (children.size > 1) {
            return "\n⚠ 警告：压缩包内除同名文件夹外还包含其他文件，已按原结构解压"
        }
        val tempDir = File(targetDir.parentFile, "${targetDir.name}.tmp_flatten")
        tempDir.deleteRecursively()
        if (!sameNameDir.renameTo(tempDir)) {
            return "\n⚠ 警告：同名文件夹结构调整失败，已按原结构解压"
        }
        if (targetDir.delete() && tempDir.renameTo(targetDir)) {
            return ""
        }
        // 调整失败，尝试恢复原结构
        targetDir.mkdirs()
        tempDir.renameTo(File(targetDir, targetDir.name))
        return "\n⚠ 警告：同名文件夹结构调整失败，已按原结构解压"
    }

    private fun extractZipWithCharset(zipFile: File, targetDir: File, charset: Charset) {
        ZipFile(zipFile, charset).use { zip ->
            val canonicalTarget = targetDir.canonicalPath
            for (entry in zip.entries()) {
                val outFile = File(targetDir, entry.name)
                // 防止路径穿越
                if (outFile.canonicalPath != canonicalTarget &&
                    !outFile.canonicalPath.startsWith(canonicalTarget + File.separator)
                ) {
                    throw SecurityException("压缩包中存在非法路径：${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}
