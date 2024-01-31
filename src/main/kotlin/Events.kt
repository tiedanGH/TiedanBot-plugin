package com.tiedan

import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.config.BotConfig
import com.tiedan.config.MailConfig
import com.tiedan.plugindata.BotInfoData
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.warning
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
     * 白名单检测
     */
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupMessageEvent.check() {
        if (BotConfig.WhiteList_enable &&
            BotConfig.WhiteList.containsKey(group.id).not() &&
            BotConfig.AdminList.contains(sender.id).not() &&
            sender.id != BotConfig.master
            ) {
            intercept()
        }
    }

    /**
     * 监测bot新好友
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun NewFriendRequestEvent.newFriend() {
        val notice: String =
                "【机器人添加新好友】\n" +
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
        val notice: String =
                "【机器人被邀请加群】\n" +
                "eventId：$eventId\n" +
                "邀请进群：$groupName\n" +
                "群号：$groupId\n" +
                "邀请人：$invitorNick\n" +
                "QQ号：$invitorId"
        try {
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
            // invitor?.sendMessage("")
        } catch (ex: Exception) {
            logger.warning(ex)
        }
    }

    /**
     * 监测bot进群
     */
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun BotJoinGroupEvent.joinGroup() {
        val notice: String =
                    "【机器人加入新群聊】\n" +
                    "群名称：${group.name}\n" +
                    "群号：${group.id}"
        try {
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (ex: Exception) {
            logger.warning(ex)
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
     * [mirai-administrator](https://github.com/cssxsh/mirai-administrator/blob/main/src/main/kotlin/xyz/cssxsh/mirai/admin/MiraiAdministrator.kt)
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