package com.tiedan.command

import com.tiedan.TiedanGame
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content

object Commandkanxi : RawCommand(
    owner = TiedanGame,
    primaryName = "看戏",
    description = "看戏",
    prefixOptional = true){

    override suspend fun CommandSender.onCommand(args: MessageChain){
        sendMessage("看什么戏，还不快in！")
    }

}

object Commandgkx : RawCommand(
    owner = TiedanGame,
    primaryName = "g",
    description = "看戏",
    prefixOptional = true){

    override suspend fun CommandSender.onCommand(args: MessageChain){
        if (args.content == "看戏" || args.content == "kanxi") {
            sendMessage("g 看什么戏！\n" +
                    "还不快点 /g join")
        }
    }

}