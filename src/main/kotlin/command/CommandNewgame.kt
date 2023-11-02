package com.tiedan.command

import com.tiedan.TiedanGame
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain

object CommandNewgame : CompositeCommand(
    owner = TiedanGame,
    primaryName = "newgame",
    secondaryNames = arrayOf("ng"),
    description = "游戏相关指令"
){

    @Description("查看帮助")
    @SubCommand("help","帮助")
    suspend fun ngHelp(context: CommandContext) {
        if (context.sender.isConsole()) {
            context.sender.sendMessage("<ng help message>")
        } else {
            context.sender.sendMessage(buildMessageChain {
                +QuoteReply(context.originalMessage)
                +PlainText("<ng help message>")
            })
        }
    }

    @Description("新建游戏房间")
    @SubCommand("create","创建")
    suspend fun CommandSender.ngCreate(@Name("游戏名称") name: String,@Name("模式") mode: String) {
        sendMessage("<ng create> $name $mode")
    }

    @Description("设置当前游戏模式")
    @SubCommand("set","设置")
    suspend fun CommandSender.ngSet(@Name("模式") mode: String) {
        sendMessage("<ng set>")
    }

    @Description("多人游戏开始")
    @SubCommand("start","开始")
    suspend fun CommandSender.ngStart() {
        sendMessage("<ng start game>")
    }

    @Description("加入多人游戏房间")
    @SubCommand("join","加入")
    suspend fun CommandSender.ngJoin() {
        sendMessage("<ng join>")
    }

    @Description("退出多人游戏房间")
    @SubCommand("quit","退出")
    suspend fun CommandSender.ngQuit() {
        sendMessage("<ng quit>")
    }

}