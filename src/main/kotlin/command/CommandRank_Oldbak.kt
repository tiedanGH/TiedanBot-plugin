package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.masterOnly
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.config.BotConfig
import com.tiedan.plugindata.RankData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import java.io.FileOutputStream
import java.io.IOException

@Deprecated("大海战BOSS挑战赛专用，可能不再使用")
object CommandRank_Oldbak : RawCommand(
    owner = TiedanGame,
    primaryName = "rank",
    secondaryNames = arrayOf("r", "排行"),
    description = "比赛排行指令",
    usage = "${commandPrefix}rank help"
) {
    override suspend fun CommandSender.onCommand(args: MessageChain) {

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
                    if (user?.id == BotConfig.master || isConsole()) {
                        reply += "\n ·master管理指令：\n" +
                                "-> 清除全部数据\n" +
                                "${commandPrefix}rank clear\n" +
                                "-> 设置记录功能\n" +
                                "${commandPrefix}rank record <on/off>\n" +
                                "-> 导出排行记录\n" +
                                "${commandPrefix}rank save"
                    }
                    sendQuoteReply(reply)
                }

                "帮助" -> {
                    var reply = " ·比赛排行指令帮助：\n" +
                                "-> 查看实时排行\n" +
                                "${commandPrefix}排行 [排名] [详情]\n" +
                                "-> 查询个人数据\n" +
                                "${commandPrefix}排行 查询"
                    if (user?.id == BotConfig.master || isConsole()) {
                        reply += "\n ·master管理指令：\n" +
                                "-> 清空全部数据\n" +
                                "${commandPrefix}排行 清空\n" +
                                "-> 设置记录功能\n" +
                                "${commandPrefix}排行 记录 <开启/关闭>\n" +
                                "-> 导出排行记录\n" +
                                "${commandPrefix}排行 保存"
                    }
                    sendQuoteReply(reply)
                }

                "rank", "info", "排名", "信息" -> {
                    val showDesc = args.getOrNull(1)?.content?.let { it == "desc" || it == "详情" } ?: false
                    var message =
                        "→ 活动详情和完整排行：https://docs.qq.com/sheet/DY01Sc0xjbkJBV05u\n→ 前10名实时排行：\n"
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
                        "→ 活动详情和完整排行：https://docs.qq.com/sheet/DY01Sc0xjbkJBV05u\n→ 您的当前个人数据：\n"
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
                    masterOnly(this)
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

                "save", "保存"-> {
                    masterOnly(this)
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
                            FileOutputStream("RecordSave.txt").use { outputStream ->
                                outputStream.write(fileContent.toByteArray())
                            }
                        }
                        sendQuoteReply("导出txt文件成功")
                    } catch (e: IOException) {
                        logger.warning(e)
                        sendQuoteReply("导出数据失败：${e.message}")
                    }
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${CommandManager.commandPrefix}rank help」来查看指令帮助")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}rank help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }
}

//        Event部分代码
//        if (RankData.enable_record && message.content.startsWith("大海战BOSS挑战赛 ") && message.contains(Image) &&
//            message.content.contains("新游戏") && message.content.contains("私信").not()) {
//            try {
//                val para = message.content.split(" ")
//                val qq = para[1].toLong()
//                val pt = para[2]
//                if (RankData.rankData.containsKey(qq).not()) {
//                    RankData.rankData[qq] = mutableMapOf()
//                    RankData.rankData[qq] = mutableMapOf("count" to mutableListOf(0, 0), "points" to mutableListOf())
//                }
//                val innerMap = RankData.rankData[qq] ?: return
//                val innerCountList = innerMap["count"] ?: return
//                val innerPointsList = innerMap["points"] ?: return
//                innerCountList[0]++
//                if (pt != "失败") {
//                    innerCountList[1]++
//                    innerPointsList.add(pt.toInt())
//                    val sortedInnerPointsList = innerPointsList.toMutableList().sortedByDescending { it }
//                    innerMap["points"] = sortedInnerPointsList.toMutableList()
//                }
//                RankData.save()
//            } catch (e: Exception) {
//                logger.warning(e)
//            }
//        }
