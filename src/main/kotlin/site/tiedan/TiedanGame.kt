package site.tiedan

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.DEFAULT_FORMAT_NAME
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import site.tiedan.command.*
import site.tiedan.config.BotConfig
import site.tiedan.config.FunctionConfig
import site.tiedan.config.MailConfig
import site.tiedan.config.PlatformConfig
import site.tiedan.data.*
import site.tiedan.module.Events
import site.tiedan.module.MessageRecorder
import site.tiedan.timer.AutoUpdateDailyData
import site.tiedan.timer.DateTime
import site.tiedan.timer.calculateNextSignDelay
import site.tiedan.timer.executeDailySign
import java.io.File
import java.util.*


object TiedanGame : KotlinPlugin(
    JvmPluginDescription(
        id = "site.tiedan.TiedanGame",
        name = "TiedanGame",
        version = "1.3.0-beta",
    ) {
        author("tiedan")
        info("""TiedanGame Plugin""")
    }
) {
    @OptIn(ConsoleExperimentalApi::class)
    val baseDataFolder = "./data/$dataHolderName"

    data class Command(val usage: String, val usageCN: String, val desc: String, val type: Int)

    override fun onEnable() {
        rdConfig()
        rdData()
        regEvent()
        regCommand()
        startTimer()
        BotInfoData.startTime = System.currentTimeMillis()

        if (FunctionConfig.apply_group == 0L) {
            logger.warning { "未配置申请群聊，私信白名单功能未启用" }
        } else {
            logger.info { "私信白名单功能已启用，申请群聊：${FunctionConfig.apply_group}" }
        }
        if (!FunctionConfig.timer_enable) logger.warning { "计时器指令未启用" }
        if (!FunctionConfig.kanxi_enable) logger.warning { "看戏指令未启用" }

        logger.info { "TiedanGame Plugin loaded!" }
    }

    override fun onDisable() {
        CommandAdmin.unregister()
        CommandBot.unregister()
        CommandTime.unregister()
        CommandRecall.unregister()
        CommandApply.unregister()
        CommandPoint.unregister()
        CommandRank.unregister()
        Commandkx.unregister()
        CommandScreenshot.unregister()
    }

    fun rdConfig() {
        BotConfig.reload()
        FunctionConfig.reload()
        MailConfig.reload()
        PlatformConfig.reload()
    }

    fun rdData() {
        AdminListData.reload()
        WhiteListData.reload()
        DomainWhiteListData.reload()
        BotInfoData.reload()
        ApplyData.reload()
        PointData.reload()
        BlackListData.reload()
        RankData.reload()

        if (WhiteListData.WhiteList.contains(BotConfig.BotId).not()) {
            WhiteListData.WhiteList.put(BotConfig.BotId, mutableMapOf())
        }
        if (WhiteListData.PrivateWhiteList.contains(BotConfig.BotId).not()) {
            WhiteListData.PrivateWhiteList.put(BotConfig.BotId, mutableSetOf())
        }
    }

    private fun regEvent() {
        GlobalEventChannel.registerListenerHost(Events)
        GlobalEventChannel.registerListenerHost(MessageRecorder)
    }

    private fun regCommand() {
        CommandAdmin.register()
        CommandBot.register()
        CommandTime.register()
        CommandRecall.register()
        CommandApply.register()
        CommandPoint.register()
        CommandRank.register()
        Commandkx.register()
        CommandScreenshot.register()
    }

    private fun startTimer() {
        val periodDay: Long = 24 * 60 * 60 * 1000
        val dailyUpdate = AutoUpdateDailyData()
        val updateDate = Date(DateTime.getCal(23, 59, 59,500).timeInMillis)
        Timer().schedule(dailyUpdate, updateDate, periodDay)
        logger.info { "已启用定时任务，每日0点自动更新统计数据" }
        // bot自动签到
        if (FunctionConfig.auto_sign) {
            launch {
                var success = true
                while (true) {
                    val delayTime = calculateNextSignDelay(success)
                    logger.info { "已重新加载协程，下次签到剩余时间 ${delayTime / 1000} 秒" }
                    delay(delayTime)
                    success = executeDailySign()
                }
            }
        } else {
            logger.info { "未启用自动签到功能" }
        }
    }

    /**
     * 发送引用消息
     */
    suspend fun CommandSender.sendQuoteReply(msgToSend: String) {
        sendQuoteReplyInternal(PlainText(msgToSend))
    }
    suspend fun CommandSender.sendQuoteReply(msgToSend: Message) {
        sendQuoteReplyInternal(msgToSend)
    }
    private suspend fun CommandSender.sendQuoteReplyInternal(message: Message) {
        if (this is CommandSenderOnMessage<*> && BotConfig.quote_enable) {
            sendMessage(buildMessageChain {
                +QuoteReply(fromEvent.message)
                +message
            })
        } else {
            sendMessage(message)
        }
    }

    /**
     * 上传文件至在线图片
     */
    suspend fun Contact.uploadFileToImage(file: File): Image? {
        return file.toExternalResource().use { resource ->     // 返回结果图片
            if (resource.formatName == DEFAULT_FORMAT_NAME) {
                return null
            }
            this.uploadImage(resource)
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

    private val platformNameRegex = Regex("[a-z0-9-]+")
    private val platformPrefixedIdRegex = Regex("^[a-z0-9-]+_.+")

    private fun normalizedPlatform(platform: String?): String? {
        val normalized = platform?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return normalized.takeIf { platformNameRegex.matches(it) }
    }

    private fun platformPrefix(botId: Long?): String? {
        if (botId == null || botId == BotConfig.BotId) return null
        return normalizedPlatform(botId.let { PlatformConfig.platforms[it]?.get("platform") })
    }

    private fun knownPlatformPrefixes(): Set<String> {
        return PlatformConfig.platforms.values.mapNotNull { normalizedPlatform(it["platform"]) }.toSet()
    }

    private fun hasKnownPlatformPrefix(id: String): Boolean {
        return knownPlatformPrefixes().any { id.startsWith("${it}_") }
    }

    fun permissionId(botId: Long?, userId: Long?): String? {
        val id = userId?.toString() ?: return null
        val prefix = platformPrefix(botId) ?: return id
        return if (id.startsWith("${prefix}_")) id else "${prefix}_$id"
    }

    fun adminConfigId(sender: CommandSender, id: String): String {
        val trimmed = id.trim().removePrefix("@")
        require(trimmed.isNotEmpty()) { "ID不能为空" }
        if (trimmed == "0") return trimmed

        val currentPrefix = platformPrefix(sender.bot?.id)
        val knownPrefixes = knownPlatformPrefixes()
        val inputPrefix = knownPrefixes.firstOrNull { trimmed.startsWith("${it}_") }

        if (currentPrefix == null) {
            require(inputPrefix == null && !platformPrefixedIdRegex.matches(trimmed)) {
                "当前账号未配置平台来源，请使用不带平台前缀的ID"
            }
            return trimmed
        }

        if (inputPrefix != null) {
            require(inputPrefix == currentPrefix) {
                "平台前缀不匹配，当前账号应使用 ${currentPrefix}_ 前缀"
            }
            require(trimmed.removePrefix("${inputPrefix}_").isNotEmpty()) { "平台前缀后的ID不能为空" }
            return trimmed
        }

        require(!platformPrefixedIdRegex.matches(trimmed)) {
            "未知平台前缀，请检查PlatformConfig或使用当前平台的 ${currentPrefix}_ 前缀"
        }
        return "${currentPrefix}_$trimmed"
    }

    fun isMaster(botId: Long?, userId: Long?): Boolean {
        val id = permissionId(botId, userId) ?: return false
        return id in BotConfig.master
    }

    fun isMaster(sender: CommandSender): Boolean = sender.isConsole() || isMaster(sender.bot?.id, sender.user?.id)

    fun isAdmin(botId: Long?, userId: Long?): Boolean {
        val id = permissionId(botId, userId) ?: return false
        return "0" in AdminListData.AdminList || id in AdminListData.AdminList || id in BotConfig.master
    }

    fun isAdmin(sender: CommandSender): Boolean = sender.isConsole() || isAdmin(sender.bot?.id, sender.user?.id)

    fun firstMasterQQ(): Long? = BotConfig.master.firstNotNullOfOrNull {
        it.takeUnless(::hasKnownPlatformPrefix)?.toLongOrNull()
    }

    fun masterQQs(): List<Long> = BotConfig.master.mapNotNull {
        it.takeUnless(::hasKnownPlatformPrefix)?.toLongOrNull()
    }

    fun masterOnly(sender: CommandSender) {
        if (!isMaster(sender)) {
            throw PermissionDeniedException("Master Only")
        }
    }

    fun adminOnly(sender: CommandSender) {
        if (!isAdmin(sender)) {
            throw PermissionDeniedException("未持有管理员权限")
        }
    }
}
