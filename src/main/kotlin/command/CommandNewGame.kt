package command

import TiedanGame
import TiedanGame.sendQuoteReply
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@Deprecated("已废弃")
@OptIn(ConsoleExperimentalApi::class)
object CommandNewGame : CompositeCommand(
    owner = TiedanGame,
    primaryName = "newgame",
    secondaryNames = arrayOf("ng"),
    description = "游戏相关指令"
){

    @Description("查看帮助")
    @SubCommand("help","帮助")
    suspend fun ngHelp(sender: CommandSender) {
        sender.sendQuoteReply("<ng help message>")
    }

    @Description("新建游戏房间")
    @SubCommand("create","创建")
    suspend fun ngCreate(sender: CommandSender, @Name("游戏名称") name: String, @Name("模式") mode: String) {
        sender.sendQuoteReply("<ng create> $name $mode")
    }

    @Description("设置当前游戏模式")
    @SubCommand("set","设置")
    suspend fun ngSet(sender: CommandSender) {
        sender.sendQuoteReply("<ng set>")
    }

    @Description("多人游戏开始")
    @SubCommand("start","开始")
    suspend fun ngStart(sender: CommandSender) {
        sender.sendQuoteReply("<ng start game>")
    }

    @Description("加入多人游戏房间")
    @SubCommand("join","加入")
    suspend fun ngJoin(sender: CommandSender) {
        sender.sendQuoteReply("<ng join>")
    }

    @Description("退出多人游戏房间")
    @SubCommand("quit","退出")
    suspend fun ngQuit(sender: CommandSender) {
        sender.sendQuoteReply("<ng quit>")
    }

}