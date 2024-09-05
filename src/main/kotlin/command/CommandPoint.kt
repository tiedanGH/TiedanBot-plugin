package com.tiedan.command

import com.tiedan.TiedanGame
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@Deprecated("已废弃")
@OptIn(ConsoleExperimentalApi::class)
object CommandPoint : CompositeCommand(
    owner = TiedanGame,
    primaryName = "point",
    secondaryNames = arrayOf("pt"),
    description = "积分相关指令"
){
    @Description("查看帮助")
    @SubCommand("help","帮助")
    suspend fun CommandSender.ptHelp() {
        sendMessage("<pt help message>")
    }

    @Description("查看个人积分余额")
    @SubCommand("info","balance")
    suspend fun CommandSender.ptInfo() {
        sendMessage("<pt info>")
    }

    @Description("向bot存入积分")
    @SubCommand("in")
    suspend fun CommandSender.ptIn(@Name("积分") num: Int) {
        sendMessage("<pt in>")
    }

    @Description("从bot转出积分")
    @SubCommand("out")
    suspend fun CommandSender.ptOut(@Name("积分") num: Int) {
        sendMessage("<pt out>")
    }

}