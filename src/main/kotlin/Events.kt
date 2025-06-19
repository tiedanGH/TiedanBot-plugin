import TiedanGame.logger
import TiedanGame.save
import command.CommandPoint.savePointChange
import config.BotConfig
import config.MailConfig
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.containsFriend
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.warning
import plugindata.*
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream

object Events : SimpleListenerHost() {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is ExceptionInEventHandlerException -> {
                logger.warning({ "TiedanGame with ${exception.event}" }, exception.cause)
            }
            else -> {
                logger.warning({ "TiedanGame" }, exception)
            }
        }
    }

    /**
     * 拦截机器人发送临时会话和陌生人消息（增强安全模式）
     */
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupTempMessagePreSendEvent.check() {
        if (BotConfig.SecureMode > 0) cancel()
    }
    @EventHandler(priority = EventPriority.HIGH)
    internal fun StrangerMessagePreSendEvent.check() {
        if (BotConfig.SecureMode > 0) cancel()
    }
    @EventHandler(priority = EventPriority.HIGH)
    internal fun UserMessagePreSendEvent.check() {
        if (BotConfig.SecureMode > 0 && bot.containsFriend(target.id).not()) cancel()
    }

    @EventHandler(priority = EventPriority.HIGH)
    internal fun MessageEvent.check() {
        // 黑名单检测
        if (BlackListData.BlackList.contains(sender.id)) {
            intercept()
        }
        // 专注模式
        if (BotConfig.focus_enable && ((subject is Group && BotConfig.focus_to != subject.id) ||
            (subject is Friend && bot.getGroup(BotConfig.focus_to)?.getMember(sender.id) == null)) &&
            AdminListData.AdminList.contains(sender.id).not() && sender.id != BotConfig.master)
        {
            intercept()
        }
    }

    /**
     * 白名单检测
     */
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupMessageEvent.check() {
        if (BotConfig.WhiteList_enable &&
            WhiteListData.WhiteList.containsKey(group.id).not() &&
            AdminListData.AdminList.contains(sender.id).not() &&
            sender.id != BotConfig.master
            ) {
            intercept()
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    internal fun NudgeEvent.check() {
        if (BotConfig.WhiteList_enable &&
            WhiteListData.WhiteList.containsKey(target.id).not() &&
            subject is Group
            ) {
            intercept()
        }
    }

    /**
     * 监测bot新好友
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun NewFriendRequestEvent.newFriendRequest() {
        val notice: String =
                "【机器人收到新好友请求】\n" +
                "eventId：$eventId\n" +
                "好友：$fromNick\n" +
                "QQ号：$fromId\n" +
                "来自群：$fromGroup\n" +
                "申请消息：$message"
        try {
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (e: Exception) {
            logger.warning(e)
        }
    }

    /**
     * 监测bot被邀请加群
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun BotInvitedJoinGroupRequestEvent.newGroup() {
        var notice: String =
                "【机器人被邀请加群】\n" +
                "eventId：$eventId\n" +
                "邀请进群：$groupName\n" +
                "群号：$groupId\n" +
                "邀请人：$invitorNick\n" +
                "QQ号：$invitorId"
        try {
            if (BotConfig.WhiteList_enable) {
                notice += if (WhiteListData.WhiteList.containsKey(groupId)) {
                    if (BotConfig.SecureMode == 2) {
                        invitor?.sendMessage("【提醒】目标群在白名单中，但因机器人处于增强安全模式，仍需手动同意申请，请您联系账号所有者")
                        "\n目标群在白名单中，但仍需手动同意邀请，已发送私信提醒邀请人"
                    } else {
                        this.accept()
                        "\n目标群在白名单中，已自动同意邀请"
                    }
                } else {
                    invitor?.sendMessage("【重要提醒】机器人在被拉群后需要白名单才能正常使用，请先联系机器人管理员申请白名单，或尝试使用「${CommandManager.commandPrefix}apply white <群号> <原因>」指令发送白名单申请")
                    "\n目标群并不在白名单中，已发送私信提醒邀请人"
                }
            }
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (e: Exception) {
            logger.warning(e)
        }
    }

    /**
     * 监测bot进群
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun BotJoinGroupEvent.joinGroup() {
        var notice: String =
                    "【机器人加入新群聊】\n" +
                    "群名称：${group.name}\n" +
                    "群号：${groupId}"
        try {
            if (BotConfig.WhiteList_enable && group.getMember(BotConfig.master) == null) {
                notice += if (WhiteListData.WhiteList.containsKey(groupId)) {
                    "\n目标群在白名单中"
                } else {
                    group.sendMessage("【重要提醒】本群 $groupId 并不在机器人的白名单中，需要白名单才能正常使用，请先联系机器人管理员申请白名单，或尝试使用「${CommandManager.commandPrefix}apply white <群号> <原因>」指令发送白名单申请。" +
                            "\n目前机器人正在使用非开源的私密签名服务，可能会导致本群*聊天记录的泄露*，请勿在此群中发送任何包含个人隐私或其他重要内容的信息，如果担心类似的安全问题，请将此机器人移出群聊")
                    "\n目标群并不在白名单中，已发送群消息提醒目标群聊"
                }
            }
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (e: Exception) {
            logger.warning(e)
        }
    }

    /**
     * 监测bot退出白名单群聊
     */
    @OptIn(MiraiExperimentalApi::class)
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun BotLeaveEvent.leaveGroup() {
        if (WhiteListData.WhiteList.containsKey(groupId)) {
            val type = when (this) {
                is BotLeaveEvent.Active-> "主动退出"
                is BotLeaveEvent.Disband -> "群聊解散"
                is BotLeaveEvent.Kick -> "被踢出群聊"
            }
            val notice: String =
                        "【机器人退出白名单群聊】\n" +
                        "群名称：${group.name}\n" +
                        "群号：${groupId}\n" +
                        "原因：$type\n" +
                        "白名单注释：${WhiteListData.WhiteList[groupId]}\n" +
                        "（白名单已被自动移除）"
            WhiteListData.WhiteList.remove(groupId)
            BotConfig.save()
            try {
                bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
            } catch (e: Exception) {
                logger.warning(e)
            }
        }
    }

    /**
     * bot数据统计
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun MessagePostSendEvent<*>.count() {
        if (message.contains(Image)) {
            if (target !is Group) {
                BotInfoData.todayPrivateImageNum++
            }
            BotInfoData.totalImageNum++
            BotInfoData.todayImageNum++
        }
        if (message.isNotEmpty()) {
            BotInfoData.totalMsgNum++
            BotInfoData.todayMsgNum++
        }
        BotInfoData.save()
        if (message.content.startsWith("新游戏积分已记录")) {
            try {
                val lines = message.content.lines()
                for (line in lines.drop(1)) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("「")) continue
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val qq = parts[0].toLong()
                        val point = parts[1].toLong()
                        savePointChange(qq, point)
                    }
                }
                PointData.save()
            } catch (e: Exception) {
                logger.warning(e)
                target.sendMessage("[错误] 积分自动记录出现错误，请联系铁蛋查看后台")
            }
        }

    }

    /**
     * ### bot掉线时发送邮件
     * [mirai-administrator](https://github.com/cssxsh/mirai-administrator/blob/main/src/main/kotlin/xyz/cssxsh/mirai/admin/MiraiAdministrator.kt#L484)
     * @author cssxsh
     */
    @EventHandler
    internal fun BotOfflineEvent.handle() {
        if (MailConfig.offline.not()) return
        if (MailConfig.close.not() && this is BotOfflineEvent.Active) return
        val session = buildMailSession {
            MailConfig.properties.inputStream().use {
                load(it)
            }
        }
        val offline = this

        launch {
            val mail = buildMailContent(session) {
                to = MailConfig.offline_mail.ifEmpty { "${BotConfig.master}@qq.com" }
                title = if (reconnect) "机器人掉线通知" else "机器人下线通知"
                text {
                    append(bot)
                    @OptIn(MiraiInternalApi::class)
                    when (offline) {
                        is BotOfflineEvent.Active -> {
                            append("主动离线")
                        }
                        is BotOfflineEvent.Dropped -> {
                            append("因网络问题而掉线")
                            if (offline.cause != null) {
                                append('\n')
                                append("cause:\n")
                                append(offline.cause!!.stackTraceToString())
                            }
                        }
                        is BotOfflineEvent.Force -> {
                            append("被挤下线.")
                        }
                        is BotOfflineEvent.MsfOffline -> {
                            append("被服务器断开.")
                            if (offline.cause != null) {
                                append('\n')
                                append("cause:\n")
                                append(offline.cause!!.stackTraceToString())
                            }
                        }
                        is BotOfflineEvent.RequireReconnect -> {
                            append("服务器主动要求更换另一个服务器.")
                            if (offline.cause != null) {
                                append('\n')
                                append("cause:\n")
                                append(offline.cause!!.stackTraceToString())
                            }
                        }
                    }

                    var start = 0
                    while (isActive) {
                        val index = indexOf("\t", start)
                        if (index == -1) break
                        replace(index, index + 1, "    ")
                        start = index + 4
                    }
                }
                file("console.log") {
                    val logs = java.io.File("logs")
                    logs.listFiles()?.maxByOrNull { it.lastModified() }

                }
                file("network.log") {
                    val logs = java.io.File("bots/${bot.id}/logs")
                    logs.listFiles()?.maxByOrNull { it.lastModified() }
                }
            }

            val current = Thread.currentThread()
            val oc = current.contextClassLoader
            try {
                current.contextClassLoader = MailConfig::class.java.classLoader
                jakarta.mail.Transport.send(mail)
            } catch (cause: jakarta.mail.MessagingException) {
                logger.error({ "邮件发送失败" }, cause)
            } finally {
                current.contextClassLoader = oc
            }
        }
    }
}