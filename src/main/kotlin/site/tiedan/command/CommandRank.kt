package site.tiedan.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.adminOnly
import site.tiedan.TiedanGame.isAdmin
import site.tiedan.TiedanGame.isMaster
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.masterOnly
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.MailConfig
import site.tiedan.data.RankData
import site.tiedan.module.buildMailContent
import site.tiedan.module.buildMailSession
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.io.path.inputStream

object CommandRank : RawCommand(
    owner = TiedanGame,
    primaryName = "rank",
    secondaryNames = arrayOf("r", "排行"),
    description = "比赛排行指令",
    usage = "${commandPrefix}rank help"
) {
    private val commandList = listOf(
        Command("rank [rank] [desc]", "排行 [排名] [详情]", "📊 查看实时排行", 1),
        Command("rank mine", "排行 查询", "-> 查询个人数据", 1),

        Command("rank export <address>", "排行 导出 [邮件地址]", "📤 导出排行记录并发送邮件", 2),

        Command("rank record <on/off>", "排行 记录 <开启/关闭>", "⚙️ 设置记录功能", 3),
        Command("rank clear", "排行 清空", "🗑️ 清除全部数据", 3),
    )


    override suspend fun CommandSender.onCommand(args: MessageChain) {

        val sortedData = RankData.rankData.entries.sortedByDescending { (_, innerMap)->
            innerMap["points"]?.take(3)?.sum() ?: 0
        }.associateBy { it.key }

        try {
            when (args.getOrElse(0) { "rank" }.toString()) {

                "help" -> {
                    var reply = " ·🏆 比赛排行指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usage}\n" }
                    if (isAdmin(this)) {
                        reply += "\n ·🛠️ admin管理指令：\n" +
                                commandList.filter { it.type == 2 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usage}\n" }
                    }
                    if (isMaster(this)) {
                        reply += "\n ·👑 master管理指令：\n"+
                                commandList.filter { it.type == 3 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usage}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "帮助" -> {
                    var reply = " ·🏆 比赛排行指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usageCN}\n" }
                    if (isAdmin(this)) {
                        reply += "\n ·🛠️ admin管理指令：\n" +
                                commandList.filter { it.type == 2 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usageCN}\n" }
                    }
                    if (isMaster(this)) {
                        reply += "\n ·👑 master管理指令：\n" +
                                commandList.filter { it.type == 3 }.joinToString("") { "${it.desc}\n${commandPrefix}${it.usageCN}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "rank", "info", "排名", "信息" -> {
                    val showDesc = args.getOrNull(1)?.content?.let { it == "desc" || it == "详情" } == true
                    var message =
                        "→ 活动详情和完整排行：${RankData.URL}\n🏆 前10名实时排行：\n"
                    sortedData.keys.forEachIndexed { index, key ->
                        if (index < 10) {
                            message += " ·No.${index + 1} $key\n"
                            val innerMap = RankData.rankData[key] ?: return
                            val innerPointsList = innerMap["points"] ?: return
                            message += "前三总分：${innerPointsList.take(3).sum()}\n"
                            if (showDesc) {
                                val totalPoints = innerPointsList.sum()
                                val count = RankData.rankData[key]?.get("count")
                                val gamesPlayed = count?.get(0)
                                val gamesWon = count?.get(1)
                                val topThreePointsString = innerPointsList.take(3).joinToString(" ")
                                message += "总积分：$totalPoints\n" +
                                           "局数：$gamesPlayed     获胜：$gamesWon\n" +
                                           "成绩：$topThreePointsString\n"
                            }
                        }
                    }
                    sendQuoteReply(message)
                }

                "mine", "我", "查询" -> {
                    var message =
                        "→ 活动详情和完整排行：${RankData.URL}\n👤 您的当前个人数据：\n"
                    if (sortedData.containsKey(user?.id)) {
                        sortedData.keys.forEachIndexed { index, key ->
                            if (key == user?.id) {
                                val innerMap = RankData.rankData[key] ?: return
                                val innerPointsList = innerMap["points"] ?: return
                                message += " ·ID：$key\n" +
                                           "当前排名：${index + 1}\n" +
                                           "前三总分：${innerPointsList.take(3).sum()}\n" +
                                           "总积分：${innerPointsList.sum()}\n" +
                                           "局数：${RankData.rankData[key]?.get("count")?.get(0)}     获胜：${RankData.rankData[key]?.get("count")?.get(1)}\n" +
                                           "全部成绩：${innerPointsList.joinToString(" ")}"
                            }
                        }
                    } else {
                        message += "[未找到您的排行数据] ${user?.id}"
                    }
                    sendQuoteReply(message)
                }

                "clear", "清空"-> {
                    masterOnly(this)
                    RankData.rankData = mutableMapOf()
                    sendQuoteReply("已清空全部数据")
                }

                "record", "记录"-> {
                    adminOnly(this)
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    when {
                        enable.contains(args[1].content) -> {
                            RankData.enable_record = true
                            sendQuoteReply("已启用分数记录功能")
                        }
                        disable.contains(args[1].content) -> {
                            RankData.enable_record = false
                            sendQuoteReply("已关闭分数记录功能")
                        }
                    }
                    RankData.save()
                }

                "export", "导出"-> {
                    adminOnly(this)
                    var fileContent = ""
                    sortedData.keys.forEachIndexed { _, key ->
                        fileContent += "${key}\t${RankData.rankData[key]?.get("count")?.get(0)}\t${RankData.rankData[key]?.get("count")?.get(1)}"
                        val innerMap = RankData.rankData[key] ?: return
                        val innerPointsList = innerMap["points"] ?: return
                        for (element in innerPointsList) {
                            fileContent += "\t$element"
                        }
                        fileContent += "\n"
                    }
                    try {
                        withContext(Dispatchers.IO) {
                            FileOutputStream("RankData.txt").use { outputStream ->
                                outputStream.write(fileContent.toByteArray())
                            }
                        }
                    } catch (e: IOException) {
                        logger.warning(e)
                        sendQuoteReply("导出数据失败：${e.message}")
                        return
                    }
                    val address: String = args[1].content
                    val session = buildMailSession {
                        MailConfig.properties.inputStream().use {
                            load(it)
                        }
                    }
                    val mail = buildMailContent(session) {
                        to = address
                        title = "数据导出"
                        text {
                            append("比赛数据已导出到附件")
                        }
                        file("RankData.txt") {
                            File("RankData.txt")
                        }
                    }
                    val current = Thread.currentThread()
                    val oc = current.contextClassLoader
                    try {
                        current.contextClassLoader = MailConfig::class.java.classLoader
                        jakarta.mail.Transport.send(mail)
                        sendQuoteReply("数据导出成功，且邮件成功发送")
                    } catch (cause: jakarta.mail.MessagingException) {
                        sendQuoteReply("数据导出成功，但邮件发送失败, 原因: ${cause.message}")
                    } finally {
                        current.contextClassLoader = oc
                    }
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${commandPrefix}rank help」来查看指令帮助")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}rank help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }
}
