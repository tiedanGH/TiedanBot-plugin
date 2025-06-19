import command.*
import config.BotConfig
import config.MailConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.isNotConsole
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.info
import plugindata.*
import timer.AutoUpdateDailyData
import timer.DateTime
import timer.calculateNextSignDelay
import timer.executeDailySign
import java.util.*


object TiedanGame : KotlinPlugin(
    JvmPluginDescription(
        id = "com.tiedan.TiedanGame",
        name = "TiedanGame",
        version = "1.3.0-beta",
    ) {
        author("tiedan")
        info("""TiedanGame Plugin""")
    }
) {
    data class Command(val usage: String, val usageCN: String, val desc: String, val type: Int)

    override fun onEnable() {
        rdConfig()
        rdData()
        regEvent()
        regCommand()
        startTimer()
        BotInfoData.startTime = System.currentTimeMillis()
        logger.info { "TiedanGame Plugin loaded!" }
    }

    override fun onDisable() {
        CommandAdmin.unregister()
        CommandBotHelp.unregister()
        CommandTime.unregister()
        CommandRecall.unregister()
        CommandApply.unregister()
        CommandPoint.unregister()
        CommandRank.unregister()
        Commandkx.unregister()
    }

    fun rdConfig() {
        BotConfig.reload()
        MailConfig.reload()
    }

    fun rdData() {
        AdminListData.reload()
        WhiteListData.reload()
        BotInfoData.reload()
        ApplyData.reload()
        PointData.reload()
        BlackListData.reload()
        RankData.reload()
    }

    private fun regEvent() {
        GlobalEventChannel.registerListenerHost(Events)
        GlobalEventChannel.registerListenerHost(MessageRecorder)
    }

    private fun regCommand() {
        CommandAdmin.register()
        CommandBotHelp.register()
        CommandTime.register()
        CommandRecall.register()
        CommandApply.register()
        CommandPoint.register()
        CommandRank.register()
        Commandkx.register()
    }

    private fun startTimer() {
        val periodDay: Long = 24 * 60 * 60 * 1000
        val dailyUpdate = AutoUpdateDailyData()
        val updateDate = Date(DateTime.getCal(23, 59, 59,500).timeInMillis)
        Timer().schedule(dailyUpdate, updateDate, periodDay)
        logger.info { "已启用定时任务，每日0点自动更新统计数据" }
        // bot自动签到
        launch {
            var success = true
            while (true) {
                val delayTime = calculateNextSignDelay(success)
                logger.info { "已重新加载协程，下次签到剩余时间 ${delayTime / 1000} 秒" }
                delay(delayTime)
                success = executeDailySign()
            }
        }
    }

    suspend fun CommandSender.sendQuoteReply(msgToSend: String) {
        if (this is CommandSenderOnMessage<*> && BotConfig.quote_enable) {
            sendMessage(buildMessageChain {
                +QuoteReply(fromEvent.message)
                +PlainText(msgToSend)
            })
        } else {
            sendMessage(msgToSend)
        }
    }

    fun getNickname(sender: CommandSender, qq: Long): String {
        val subject = sender.subject
        var nickname: String? = null
        if (subject is Group) {
            nickname = subject.getMember(qq)?.nameCardOrNick
        }
        if (nickname == null) {
            nickname = sender.bot?.getFriend(qq)?.nameCardOrNick
        }
        return nickname ?: "[获取昵称失败]"
    }

    fun masterOnly(sender: CommandSender) {
        if (sender.user?.id != BotConfig.master && sender.isNotConsole()) {
            throw PermissionDeniedException("Master Only")
        }
    }

    fun adminOnly(sender: CommandSender) {
        if (AdminListData.AdminList.contains(sender.user?.id).not() && AdminListData.AdminList.contains(0).not() &&
            sender.user?.id != BotConfig.master && sender.isNotConsole()) {
            throw PermissionDeniedException("未持有管理员权限")
        }
    }
}