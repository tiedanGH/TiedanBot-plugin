package com.tiedan

import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.plugindata.BotInfoData
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.event.events.MessagePreSendEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
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
//        if (Config.focus_enable && ((target is Group && Config.focus_to != target.id) ||
//            (target is Friend && Config.focus_to.(target.id))) &&
//            ) {
//            message = PlainText(
//                "***专注模式运行中***\n" +
//                        "bot正在专注于群聊 ${Config.focus_to} 进行服务，可能正在进行比赛或其他重要事项，暂时不支持其他服务\n" +
//                        "如有疑问，请联系管理员")
//        }
//    }

    // 白名单检测
    @EventHandler(priority = EventPriority.HIGH)
    internal fun GroupMessageEvent.check() {
        if (Config.WhiteList_enable &&
            !Config.WhiteList.containsKey(group.id) &&
            !Config.AdminList.contains(sender.id) &&
            sender.id != Config.master
            ) {
            intercept()
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