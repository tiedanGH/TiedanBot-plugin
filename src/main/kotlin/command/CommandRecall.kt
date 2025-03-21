package com.tiedan.command

import com.tiedan.MessageRecorder.from
import com.tiedan.MessageRecorder.quote
import com.tiedan.MessageRecorder.target
import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.config.BotConfig
import com.tiedan.plugindata.AdminListData
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.IllegalCommandArgumentException
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.render
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageSource.Key.recall

object CommandRecall : SimpleCommand(
    owner = TiedanGame,
    primaryName = "recall",
    secondaryNames = arrayOf("撤回"),
    description = "撤回消息"
) {
    @Handler @ConsoleExperimentalApi
    suspend fun CommandSender.handle(contact: Contact? = null) {
        if (AdminListData.AdminList.contains(user?.id).not() && AdminListData.AdminList.contains(0).not() && user?.id != BotConfig.master && user != null) {
            sendMessage("未持有管理员权限")
            return
        }
        val message = try {
            val source = when {
                contact is Member -> from(member = contact)
                contact != null -> target(contact = contact)
                this is CommandSenderOnMessage<*> -> quote(event = fromEvent)
                else -> throw IllegalCommandArgumentException("参数不足以定位消息")
            }
            if (source != null) {
                source.recall()
                "${contact?.render() ?: source.fromId}的消息撤回成功"
            } else {
                "${contact?.render().orEmpty()}未找到消息"
            }
        } catch (e: Exception) {
            logger.warning(e)
            "出现错误：${e.message}"
        }
        sendMessage(message)
    }
}