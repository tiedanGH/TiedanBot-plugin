package site.tiedan.command

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isNotConsole
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import site.tiedan.MessageRecorder.quoteMessage
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.logger
import site.tiedan.config.BotConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * ### 此部分不再使用，已迁移至 [imgloc-uploader](https://github.com/tiedanGH/imgloc-uploader)
 */
@Deprecated("此部分不再使用，已迁移")
object CommandUpload : RawCommand(
    owner = TiedanGame,
    primaryName = "upload",
    secondaryNames = arrayOf("上传", "图床"),
    description = "上传图片至图床，获取图片链接"
) {
    private var Image_API: String = "属性已弃用"
    private val uploadLock = Mutex()
    private const val HELP =
        "\uD83D\uDDBC\uFE0F 本功能用于将自定义图片上传至图床。上传成功时，您将获得图片链接。\n" +
        "使用 https://imgloc.com/ 提供的上传接口，使用方法如下：\n" +
        "#upload <引用图片>\n" +
        "#upload <图片> [图片] [图片]...\n" +
        "#upload <链接> [链接] [链接]...\n" +
        "\n" +
        "【禁止内容】请勿上传：儿童色情内容、严重血腥内容、对未成年人的性暴力。允许NSFW图片，但只能私用，禁止添加至bot任何公共图库！\n" +
        "日志会记录所有上传行为，如果您违反了上述任何被禁止的内容，一经发现将会被bot永久拉黑并禁止使用本bot所有功能！"

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        // 先尝试引用获取图片
        var quoteImage: Image? = null
        if (this is CommandSenderOnMessage<*>) {
            if (fromEvent.message[QuoteReply.Key] != null) {
                val messages = quoteMessage(event = fromEvent)
                if (messages != null) {
                    if (messages.contains(Image)) {
                        quoteImage = messages.firstIsInstanceOrNull<Image>()
                    }
                }
                if (quoteImage == null) {
                    val message = "[获取失败] 请回复一条近期包含图片的消息，或尝试保存图片后发图上传"
                    if (user != null && subject is Group) sendMessage(At(user!!) + "\n" + message)
                    else sendMessage(message)
                    return
                }
            }
        }
        if (quoteImage != null) {
            uploadLock.withLock {
                val (success, result) = uploadImageFromUrlImgLoc(quoteImage.queryUrl(), subject)
                val message = if (success) "[上传成功] 图片链接为：\n$result" else result
                safeSendMessage(user, subject, message)
            }
            return
        }
        // 无引用遍历消息链
        if (args.isEmpty()) {
            safeSendMessage(user, subject, HELP)
            return
        }
        uploadLock.withLock {
            try {
                var message = "【操作成功】上传结果如下："
                var total = 0
                var successCount = 0
                for (arg in args) {
                    val imageUrl = try {
                        (arg as Image).queryUrl()
                    } catch (_: ClassCastException) {
                        if (!arg.content.startsWith("http")) {
                            if (args.size == 1) message = HELP
                            continue
                        }
                        arg.content
                    }
                    val (success, result) = uploadImageFromUrlImgLoc(imageUrl, subject)
                    message += "\n$result"
                    total++
                    if (success) successCount++
                }
                message += "\n·上传总数：$total（成功：$successCount）"
                safeSendMessage(user, subject, message)
            } catch (e: Exception) {
                logger.warning(e)
                safeSendMessage(user, subject, "[发生未知错误] 请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
            }
        }
    }

    private suspend fun CommandSender.safeSendMessage(user: User?, subject: Contact?, message: String) {
        while (true) {
            try {
                if (user != null && subject is Group) {
                    sendMessage(At(user) + "\n" + message)
                } else {
                    sendMessage(message)
                }
                return
            } catch (e: Exception) {
                if (isNotConsole() && Bot.getInstanceOrNull(BotConfig.BotId) == null) {
                    logger.warning("Bot不存在，停止重试。原因：${e::class.simpleName}(${e.message})")
                    return
                }
                logger.warning("发送消息失败，5秒后重试。原因：${e::class.simpleName}(${e.message})")
                delay(5000)
            }
        }
    }

    private fun uploadImageFromUrlImgLoc(imageUrl: String, subject: Contact?): Pair<Boolean, String> {
        val fixedUrl = if (subject !is Group) {
            imageUrl.replace("download?appid=1407", "download?appid=1406")
        } else imageUrl
        return uploadImageFromUrlImgLoc(fixedUrl)
    }

    private fun uploadImageFromUrlImgLoc(imageUrl: String): Pair<Boolean, String> {
        val tempFile = try {
            val urlObj = URL(imageUrl)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()
            val file = File.createTempFile("upload_", "")
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            return Pair(false, "[下载失败] 下载图片时发生错误：${e.message}")
        }
        logger.info("Uploading image: $imageUrl")
        val command = listOf(
            "curl",
            "--fail-with-body",
            "-s",
            "-X", "POST",
            "-H", "X-API-Key: $Image_API",
            "-H", "Content-Type: multipart/form-data",
            "-F", "source=@${tempFile.absolutePath}",
            "https://imgloc.com/api/1/upload"
        )
        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                tempFile.delete()
                return Pair(false, "[请求超时] 30秒内未返回结果")
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (output == "") {
                return Pair(false, "[上传失败] 返回内容为空")
            }
            val regex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val match = regex.find(output)
            val result = if (match != null) {
                val url = match.groupValues[1].replace("\\/", "/")
                if (url.startsWith("http")) Pair(true, url)
                else Pair(false, "[上传失败] 返回内容异常：$output")
            } else {
                Pair(false, "[上传失败] 返回内容异常：$output")
            }
            tempFile.delete()
            return result
        } catch (e: Exception) {
            tempFile.delete()
            return Pair(false, "[上传失败] 请求发生错误：${e.message}")
        }
    }
}