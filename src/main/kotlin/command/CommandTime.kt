package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.Command
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.sendQuoteReply
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
    private val timeCommandList = listOf(
        Command("t count <秒> [名称]", "时间 倒计时 <秒> [名称]", "▶️ 启动一个计时器", 1),
        Command("t tiedan", "时间 铁蛋", "🥚 查看铁蛋的时间", 1),
        Command("t star", "时间 星星", "🌟 查看星星的时间", 1)
    )

    private var THREAD : Int = 0

    override suspend fun CommandContext.onCommand(args: MessageChain) {

        try {
            when (args[0].content) {

                "help" -> {  // 英文帮助
                    val reply = buildString {
                        append(" ·⏱️ 计时器指令帮助\n")
                        timeCommandList.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助" -> {  // 中文帮助
                    val reply = buildString {
                        append(" ·⏱️ 计时器指令帮助\n")
                        timeCommandList.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "count", "倒计时"-> {
                    val second = try {
                        args[1].content.toInt()
                    } catch (e: Exception) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，时间必须为Int型整数")
                        return
                    }
                    var name = args.getOrElse(2) { "" }.toString()
                    if (name.isNotEmpty()) name = " $name "
                    if (second < 1 || second > 3600) {
                        sendQuoteReply(sender, originalMessage, "倒计时仅支持1 ~ 3600")
                        return
                    }
                    if (THREAD >= 5) {
                        sendQuoteReply(sender, originalMessage, "计时器无法启动，因为已经有 $THREAD 个进程正在运行")
                        return
                    }
                    sendQuoteReply(sender, originalMessage, "倒计时开始")
                    var remainingTime = second
                    THREAD++
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
                                    THREAD--
                                    sendQuoteReply(sender, originalMessage, "${name}时间到！")
                                }
                            }
                            remainingTime--
                            delay(1000)
                        }
                    }
                }

                "star", "星星"-> {
//                    val zoneId = ZoneId.of("America/Los_Angeles")
                    val zoneId = ZoneId.of("Asia/Shanghai")
                    val now = ZonedDateTime.now(zoneId)
                    val formatted = now.format(DateTimeFormatter.ofPattern("HH:mm:ss   Z"))
//                    sendQuoteReply(sender, originalMessage, "星星现在的时间为：\n$formatted\n（太平洋标准时间）")
                    sendQuoteReply(sender, originalMessage, "星星现在的时间为：\n$formatted\n（北京时间）")
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
        } catch (e: IndexOutOfBoundsException) {
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${commandPrefix}t help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply(sender, originalMessage, "[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

}