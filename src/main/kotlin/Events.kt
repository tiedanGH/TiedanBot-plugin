package com.tiedan

import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.config.BotConfig
import com.tiedan.plugindata.BotInfoData
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.warning
import kotlin.coroutines.CoroutineContext

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

    // 白名单检测
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupMessageEvent.check() {
        if (BotConfig.WhiteList_enable &&
            !BotConfig.WhiteList.containsKey(group.id) &&
            !BotConfig.AdminList.contains(sender.id) &&
            sender.id != BotConfig.master
            ) {
            intercept()
        }
    }

    // 监测bot新好友
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun NewFriendRequestEvent.monitor() {
        val notice: String =
                "[机器人添加新好友]\n" +
                "eventId：$eventId\n" +
                "好友：$fromNick\n" +
                "QQ号：$fromId\n" +
                "来自群：$fromGroup\n" +
                "申请消息：$message"
        try {
            bot.getFriendOrFail(BotConfig.master).sendMessage(notice)
        } catch (ex: NoSuchElementException) {
            logger.warning(ex)
        }
    }

    // 监测bot被邀请进群
    @EventHandler(priority = EventPriority.MONITOR)
    internal suspend fun BotInvitedJoinGroupRequestEvent.monitor() {
        val notice: String =
                "[机器人被邀请进群]\n" +
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

    // bot数据统计
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
}