package com.tiedan

import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.config.BotConfig
import com.tiedan.config.MailConfig
import com.tiedan.plugindata.AdminListData
import com.tiedan.plugindata.BlackListData
import com.tiedan.plugindata.BotInfoData
import com.tiedan.plugindata.WhiteListData
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.warning
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream
import kotlin.random.Random.Default.nextInt

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

//    // TODO 专注模式事件
//    @EventHandler
//    internal fun MessagePreSendEvent.check() {
//        if (BotConfig.focus_enable && ((target is Group && BotConfig.focus_to != target.id) ||
//            (target is Friend && BotConfig.focus_to.(target.id))) &&
//            ) {
//            message = PlainText(
//                "***专注模式运行中***\n" +
//                        "bot正在专注于群聊 ${BotConfig.focus_to} 进行服务，可能正在进行比赛或其他重要事项，暂时不支持其他服务\n" +
//                        "如有疑问，请联系管理员")
//        }
//    }

    /**
     * 拦截临时会话消息
     */
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupTempMessagePreSendEvent.check() {
        intercept()
    }
    @EventHandler(priority = EventPriority.HIGH)
    internal fun StrangerMessagePreSendEvent.check() {
        intercept()
    }

    /**
     * 黑名单检测
     */
    @EventHandler(priority = EventPriority.HIGH)
    internal fun MessageEvent.check() {
        for (black in BlackListData.BlackList) {
            if (sender.id == black && sender.id != BotConfig.master) {
                intercept()
                break
            }
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
        } catch (ex: Exception) {
            logger.warning(ex)
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
                    this.accept()
                    "\n目标群在白名单中，已自动同意邀请"
                } else {
                    invitor?.sendMessage("【重要提醒】机器人在被拉群后需要白名单才能正常使用，请先联系机器人管理员申请白名单，或尝试使用「${CommandManager.commandPrefix}apply white <群号> <原因>」指令发送白名单申请")
                    "\n目标群并不在白名单中，已发送私信提醒邀请人"
                }
            }
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (ex: Exception) {
            logger.warning(ex)
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
        } catch (ex: Exception) {
            logger.warning(ex)
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
            } catch (ex: Exception) {
                logger.warning(ex)
            }
        }
    }

    /**
     * “看戏”关键词回复
     */
    @EventHandler(priority = EventPriority.NORMAL)
    internal suspend fun GroupMessageEvent.kx() {
        val kxReply = mutableListOf(
            "还在看戏，还不赶紧加入！",
            "看什么戏，还不快in！",
            "in，为什么不in！",
            "都看了多久戏了，为什么还不in！",
            "看戏，看戏！为什么不加入！",
            "你看看这都几点了，还不打算加入！",
            "别让等待成为遗憾，加入，现在就开",
            "理论不如实践，看戏不如行动",
        )
        if (message.content == "看戏" || message.content.endsWith("g 看戏")) {
            group.sendMessage(messageChainOf(At(sender.id),PlainText(" "), PlainText(kxReply[nextInt(kxReply.size)])))
        }
    }

    /**
     * bot数据统计
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal fun MessagePostSendEvent<*>.count() {
        if (message.contains(Image)) {
            if (target is Friend) {
                BotInfoData.todayFriendImageNum++
            }
            BotInfoData.totalImageNum++
            BotInfoData.todayImageNum++
        }
        if (message.isNotEmpty()) {
            BotInfoData.totalMsgNum++
            BotInfoData.todayMsgNum++
        }
        BotInfoData.save()
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