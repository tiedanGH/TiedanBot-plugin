package com.tiedan

import com.tiedan.command.*
import com.tiedan.plugindata.BotInfoData
import com.tiedan.timer.AutoUpdateDailyData
import com.tiedan.timer.DateTime
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.info
import java.util.*


object TiedanGame : KotlinPlugin(
    JvmPluginDescription(
        id = "com.tiedan.TiedanGame",
        name = "TiedanGame",
        version = "1.1.0",
    ) {
        author("tiedan")
        info("""TiedanGame Plugin""")
    }
) {
    override fun onEnable() {
        rdConfig()
        rdData()
        regEvent()
        regCommand()
        startTimer()
        logger.info { "TiedanGame Plugin loaded!" }
    }

    override fun onDisable() {
        super.onDisable()
    }

    fun rdConfig() {
        Config.reload()
    }

    fun rdData() {
        BotInfoData.reload()
    }

    private fun regEvent() {
        GlobalEventChannel.registerListenerHost(Events)
    }

    private fun regCommand() {
        CommandAdmin.register()
        CommandBotHelp.register()
//        Commandkanxi.register()
//        Commandgkx.register()
//        CommandNewGame.register()
//        CommandPoint.register()
    }

    private fun startTimer() {
        val periodDay: Long = 24 * 60 * 60 * 1000
        val dailyUpdate = AutoUpdateDailyData(logger)
        val updateDate = DateTime.getCal(23, 59, 59,500)
        Timer().schedule(dailyUpdate, updateDate, periodDay)
        TiedanGame.logger.info { "已启用定时任务，每天0点自动更新统计数据" }
    }

    suspend fun sendQuoteReply(sender: CommandSender, originalMessage: MessageChain, msgToSend: String) {
        if (sender.isConsole() || !Config.quote_enable) {
            sender.sendMessage(msgToSend)
        } else {
            sender.sendMessage(buildMessageChain {
                +QuoteReply(originalMessage)
                +PlainText(msgToSend)
            })
        }
    }
}