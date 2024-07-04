package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.TiedanGame.thread
import com.tiedan.config.BotConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.warning
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object CommandTime : RawCommand(
    owner = TiedanGame,
    primaryName = "t",
    secondaryNames = arrayOf("time", "时间"),
    description = "计时器相关指令",
    usage = "${commandPrefix}time help"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        try {
            when (args[0].content) {

                "help"-> {   // 查看time帮助（help）
                    val reply = " ·计时器指令帮助：\n" +
                                "-> 启动一个计时器\n" +
                                "${commandPrefix}t count <秒> [名称]\n" +
                                "-> 查看铁蛋的时间\n" +
                                "${commandPrefix}t tiedan\n" +
                                "-> 查看星星的时间\n" +
                                "${commandPrefix}t star"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助"-> {   // 查看time帮助（帮助）
                    val reply = " ·计时器指令帮助：\n" +
                                "-> 启动一个计时器\n" +
                                "${commandPrefix}时间 倒计时 <秒> [名称]\n" +
                                "-> 查看铁蛋的时间\n" +
                                "${commandPrefix}时间 铁蛋\n" +
                                "-> 查看星星的时间\n" +
                                "${commandPrefix}时间 星星"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "count", "倒计时"-> {
                    val second = try {
                        args[1].content.toInt()
                    } catch (ex: Exception) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，时间必须为Int型整数")
                        return
                    }
                    var name = args.getOrElse(2) { "" }.toString()
                    if (name.isNotEmpty()) name = " $name "
                    if (second < 1 || second > 3600) {
                        sendQuoteReply(sender, originalMessage, "倒计时仅支持1 ~ 3600")
                        return
                    }
                    if (thread >= 3) {
                        sendQuoteReply(sender, originalMessage, "计时器无法启动，因为已经有 $thread 个进程正在运行")
                        return
                    }
                    sendQuoteReply(sender, originalMessage, "倒计时开始")
                    var remainingTime = second
                    thread++
                    CoroutineScope(Dispatchers.IO).launch {
                        while (remainingTime >= 0) {
                            when (remainingTime) {
                                1800 -> {
                                    if (second != 1800) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩30分钟")
                                }
                                600 -> {
                                    if (second != 600) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩10分钟")
                                }
                                180 -> {
                                    if (second != 180) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩3分钟")
                                }
                                120 -> {
                                    if (second != 120) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩2分钟")
                                }
                                60 -> {
                                    if (second != 60) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩1分钟")
                                }
                                30 -> {
                                    if (second != 30)sendQuoteReply(sender, originalMessage, "倒计时${name}还剩30秒")
                                }
                                10 -> {
                                    if (second != 10) sendQuoteReply(sender, originalMessage, "倒计时${name}还剩10秒")
                                }
                                0 -> {
                                    sendQuoteReply(sender, originalMessage, "${name}时间到！")
                                    thread--
                                }
                            }
                            remainingTime--
                            delay(1000)
                        }
                    }
                }

                "star", "星星"-> {
                    val zoneId = ZoneId.of("America/Los_Angeles")
                    val now = ZonedDateTime.now(zoneId)
                    val formatted = now.format(DateTimeFormatter.ofPattern("HH:mm:ss   Z"))
                    sendQuoteReply(sender, originalMessage, "星星现在的时间为：\n$formatted\n（太平洋标准时间）")
                }

                "tiedan", "铁蛋"-> {
                    val zoneId = ZoneId.of(BotConfig.TimeZone[0])
                    val now = ZonedDateTime.now(zoneId)
                    val formatted = now.format(DateTimeFormatter.ofPattern("HH:mm:ss   Z"))
                    sendQuoteReply(sender, originalMessage, "铁蛋现在的时间为：\n$formatted\n（${BotConfig.TimeZone[1]}时间）")
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「${commandPrefix}t help」来查看指令帮助")
                }
            }
        } catch (ex: Exception) {
            logger.warning {"error: ${ex.message}"}
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${commandPrefix}t help」来查看指令帮助")
        }
    }

}