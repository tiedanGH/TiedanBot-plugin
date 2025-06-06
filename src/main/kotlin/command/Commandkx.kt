package com.tiedan.command

import com.tiedan.TiedanGame
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.messageChainOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object Commandkx : RawCommand(
    owner = TiedanGame,
    primaryName = "看戏",
    secondaryNames = arrayOf("kanxi"),
    description = "看戏指令回复",
    usage = "kanxi",
    prefixOptional = true){

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        val formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val kxReply = mutableListOf(
            "还在看戏，还不赶紧加入！",
            "看什么戏，还不快in！",
            "in，为什么不in！",
            "都看了多久戏了，为什么还不in！",
            "看戏，看戏！为什么不加入！",
            "你看看这都 $formatted 了，还不打算加入！",
            "别让等待成为遗憾，加入，现在就开",
            "理论不如实践，看戏不如行动",
            "看戏虽好，但亲自上场才会更有乐趣",
            "机会稍纵即逝，现在加入，不要错过享受游戏的机会！",
        )
        if (isConsole() || subject is Friend) {
            sendMessage(kxReply[Random.nextInt(kxReply.size)])
        } else {
            sendMessage(
                messageChainOf(
                    At(user!!.id),
                    PlainText(" "),
                    PlainText(kxReply[Random.nextInt(kxReply.size)])
                )
            )
        }
    }

}
