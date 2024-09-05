package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.adminOnly
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.masterOnly
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.buildMailContent
import com.tiedan.buildMailSession
import com.tiedan.config.BotConfig
import com.tiedan.config.MailConfig
import com.tiedan.plugindata.AdminListData
import com.tiedan.plugindata.RankData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.warning
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
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        val sortedData = RankData.rankData.entries.sortedByDescending { (_, innerMap)->
            innerMap["points"]?.take(3)?.sum() ?: 0
        }.associateBy { it.key }

        try {
            when (args.getOrElse(0) { "rank" }.toString()) {
                "help" -> {
                    var reply = " ·比赛排行指令帮助：\n" +
                                "-> 查看实时排行\n" +
                                "${commandPrefix}rank [rank] [desc]\n" +
                                "-> 查询个人数据\n" +
                                "${commandPrefix}rank mine"
                    if (AdminListData.AdminList.contains(sender.user?.id) || sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n ·admin管理指令：\n" +
                                "-> 导出排行记录并发送邮件\n" +
                                "${commandPrefix}rank export <address>"
                    }
                    if (sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n ·master管理指令：\n" +
                                "-> 设置记录功能\n" +
                                "${commandPrefix}rank record <on/off>\n" +
                                "-> 清除全部数据\n" +
                                "${commandPrefix}rank clear"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助" -> {
                    var reply = " ·比赛排行指令帮助：\n" +
                                "-> 查看实时排行\n" +
                                "${commandPrefix}排行 [排名] [详情]\n" +
                                "-> 查询个人数据\n" +
                                "${commandPrefix}排行 查询"
                    if (AdminListData.AdminList.contains(sender.user?.id) || sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n ·admin管理指令：\n" +
                                "-> 导出排行记录\n" +
                                "${commandPrefix}排行 导出 [邮件地址]"
                    }
                    if (sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n ·master管理指令：\n" +
                                "-> 设置记录功能\n" +
                                "${commandPrefix}排行 记录 <开启/关闭>\n" +
                                "-> 清空全部数据\n" +
                                "${commandPrefix}排行 清空"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "rank", "info", "排名", "信息" -> {
                    val showDesc = args.getOrNull(1)?.content?.let { it == "desc" || it == "详情" } ?: false
                    var message =
                        "→ 活动详情和完整排行：${RankData.URL}\n→ 前10名实时排行：\n"
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
                    sendQuoteReply(sender, originalMessage, message)
                }

                "mine", "我", "查询" -> {
                    var message =
                        "→ 活动详情和完整排行：${RankData.URL}\n→ 您的当前个人数据：\n"
                    if (sortedData.containsKey(sender.user?.id)) {
                        sortedData.keys.forEachIndexed { index, key ->
                            if (key == sender.user?.id) {
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
                        message += "[未找到您的排行数据] ${sender.user?.id}"
                    }
                    sendQuoteReply(sender, originalMessage, message)
                }

                "clear", "清空"-> {
                    masterOnly(sender)
                    RankData.rankData = mutableMapOf()
                    sendQuoteReply(sender, originalMessage, "已清空全部数据")
                }

                "record", "记录"-> {
                    adminOnly(sender)
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    when {
                        enable.contains(args[1].content) -> {
                            RankData.enable_record = true
                            sendQuoteReply(sender, originalMessage, "已启用分数记录功能")
                        }
                        disable.contains(args[1].content) -> {
                            RankData.enable_record = false
                            sendQuoteReply(sender, originalMessage, "已关闭分数记录功能")
                        }
                    }
                    RankData.save()
                }

                "export", "导出"-> {
                    adminOnly(sender)
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
                    } catch (ex: IOException) {
                        logger.warning(ex)
                        sendQuoteReply(sender, originalMessage, "导出数据失败：${ex.message}")
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
                        sendQuoteReply(sender, originalMessage, "数据导出成功，且邮件成功发送")
                    } catch (cause: jakarta.mail.MessagingException) {
                        sendQuoteReply(sender, originalMessage, "数据导出成功，但邮件发送失败, cause: ${cause.message}")
                    } finally {
                        current.contextClassLoader = oc
                    }
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「${CommandManager.commandPrefix}rank help」来查看指令帮助")
                }
            }
        } catch (ex: PermissionDeniedException) {
            sendQuoteReply(sender, originalMessage, "[操作无效] ${ex.message}")
        } catch (ex: Exception) {
            logger.warning {"error: ${ex.message}"}
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${CommandManager.commandPrefix}rank help」来查看指令帮助")
        }
    }
}
