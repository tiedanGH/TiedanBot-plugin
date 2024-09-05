package com.tiedan.command

import com.tiedan.TiedanGame
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content

@Deprecated("已废弃")
object Commandkx : RawCommand(
    owner = TiedanGame,
    primaryName = "看戏",
    secondaryNames = arrayOf("g"),
    description = "看戏",
    prefixOptional = true){

    override suspend fun CommandSender.onCommand(args: MessageChain){
        if (args.content == "看戏" || args.content == "kanxi") {
            sendMessage("g 看什么戏！\n" +
                    "还不快点 /g join")
        } else {
            sendMessage("看什么戏，还不快in！")
        }
    }

}
