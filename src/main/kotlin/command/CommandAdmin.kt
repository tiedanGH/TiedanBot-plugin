package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.buildMailContent
import com.tiedan.buildMailSession
import com.tiedan.config.BotConfig
import com.tiedan.config.MailConfig
import com.tiedan.plugindata.BlackListData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.messageChainOf
import net.mamoe.mirai.utils.warning
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.inputStream

object CommandAdmin : RawCommand(
    owner = TiedanGame,
    primaryName = "admin",
    secondaryNames = arrayOf("管理"),
    description = "管理员相关指令"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        val whiteEnable: String = if (BotConfig.WhiteList_enable) { "已启用" } else { "未启用" }

        if (BotConfig.AdminList.contains(sender.user?.id).not() && BotConfig.AdminList.contains(0).not() &&
            sender.user?.id != BotConfig.master && sender.isNotConsole()) {
            sendQuoteReply(sender, originalMessage, "未持有管理员权限")
            return
        }

        try {
            when (args[0].content) {

                "help"-> {   // 查看admin可用帮助（help）
                    var reply = " ·admin可用帮助：\n" +
                                "-> 查看管理员列表\n" +
                                "${commandPrefix}admin list\n" +
                                "-> 查看黑名单列表\n" +
                                "${commandPrefix}admin BlackList\n" +
                                "-> 查看白名单列表\n" +
                                "${commandPrefix}admin WhiteList [info]\n" +
                                "-> 设置白名单开关状态\n" +
                                "${commandPrefix}admin setWhiteList <开启/关闭>\n" +
                                "-> 添加白名单\n" +
                                "${commandPrefix}admin addWhiteList [group] [desc]\n" +
                                "-> 移除白名单\n" +
                                "${commandPrefix}admin delWhiteList [group]"
                    if (sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n" +
                                " ·master管理指令：\n" +
                                "-> 添加/移除管理员\n" +
                                "${commandPrefix}admin op/deop <qq>\n" +
                                "-> 添加/移除黑名单\n" +
                                "${commandPrefix}admin black <qq>\n" +
                                "-> 机器人关机\n" +
                                "${commandPrefix}admin shutdown\n" +
                                "-> 积分转账\n" +
                                "${commandPrefix}admin transfer <qq> <point>\n" +
                                "-> 消息发送\n" +
                                "${commandPrefix}admin send <qq> [message]\n" +
                                "-> 配置及数据重载\n" +
                                "${commandPrefix}admin reload\n" +
                                "-> 发送邮件备份日志\n" +
                                "${commandPrefix}admin sendmail [address]"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助"-> {   // 查看admin可用帮助（帮助）
                    var reply = " ·admin可用帮助：\n" +
                            "-> 查看管理员列表\n" +
                            "${commandPrefix}管理 列表\n" +
                            "-> 查看黑名单列表\n" +
                            "${commandPrefix}管理 黑名单\n" +
                            "-> 查看白名单列表\n" +
                            "${commandPrefix}管理 白名单 [信息]\n" +
                            "-> 设置白名单开关状态\n" +
                            "${commandPrefix}管理 设置白名单 <开启/关闭>\n" +
                            "-> 添加白名单\n" +
                            "${commandPrefix}管理 添加白名单 [群号] [描述]\n" +
                            "-> 移除白名单\n" +
                            "${commandPrefix}管理 移除白名单 [群号]"
                    if (sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n" +
                                " ·master管理指令：\n" +
                                "-> 添加/移除管理员\n" +
                                "${commandPrefix}管理 添加/移除管理员 <QQ号>\n" +
                                "-> 添加/移除黑名单\n" +
                                "${commandPrefix}管理 黑名单 <QQ号>\n" +
                                "-> 机器人关机\n" +
                                "${commandPrefix}管理 关机\n" +
                                "-> 积分转账\n" +
                                "${commandPrefix}管理 转账 <QQ号> <积分>\n" +
                                "-> 消息发送\n" +
                                "${commandPrefix}管理 发送 <QQ号> [消息]\n" +
                                "-> 配置及数据重载\n" +
                                "${commandPrefix}管理 重载\n" +
                                "-> 发送邮件备份日志\n" +
                                "${commandPrefix}管理 发送邮件 [邮件地址]"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "list", "列表"-> {   // 查看管理员列表
                    var adminListInfo = "·管理员列表：\n"
                    for (admin in BotConfig.AdminList) {
                        adminListInfo = adminListInfo + admin + "\n"
                    }
                    sendQuoteReply(sender, originalMessage, adminListInfo)
                }

                "op", "添加管理员"-> {   // 添加管理员
                    masterOnly(sender)
                    try {
                        val qq = args[1].content.toLong()
                        val result = BotConfig.AdminList.add(qq)
                        if (result) {
                            BotConfig.AdminList.sort()
                            BotConfig.AdminList = BotConfig.AdminList.distinct().toMutableList()
                            BotConfig.save()
                            if (qq == 0.toLong()) {   // 0视为all
                                sendQuoteReply(sender, originalMessage, "已解除管理员权限限制")
                            } else {
                                sendQuoteReply(sender, originalMessage, "已将 $qq 设为管理员")
                            }
                        } else {
                            sendQuoteReply(sender, originalMessage, "[未知原因] 添加失败")
                        }
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "deop", "移除管理员"-> {   // 移除管理员
                    masterOnly(sender)
                    try {
                        val qq = args[1].content.toLong()
                        val result = BotConfig.AdminList.remove(qq)
                        if (result) {
                            BotConfig.save()
                            if (qq == 0.toLong()) {   // 0视为all
                                sendQuoteReply(sender, originalMessage, "已恢复管理员权限限制")
                            } else {
                                sendQuoteReply(sender, originalMessage, "已将 $qq 移除管理员")
                            }
                        } else {
                            sendQuoteReply(sender, originalMessage, "不存在管理员 $qq")
                        }
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "BlackList", "黑名单"-> {   // 查看黑名单列表
                    var blackListInfo = "·黑名单列表：\n"
                    for (black in BlackListData.BlackList) {
                        blackListInfo = blackListInfo + black + "\n"
                    }
                    sendQuoteReply(sender, originalMessage, blackListInfo)
                }

                "black", "添加黑名单"-> {   // 添加/移除黑名单
                    masterOnly(sender)
                    try {
                        val qq = args[1].content.toLong()
                        if (BlackListData.BlackList.contains(qq)) {
                            BlackListData.BlackList.remove(qq)
                            sendQuoteReply(sender, originalMessage, "已将 $qq 移出黑名单")
                        } else {
                            BlackListData.BlackList.add(qq)
                            sendQuoteReply(sender, originalMessage, "已将 $qq 移入黑名单")
                        }
                        BlackListData.save()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "shutdown", "关机"-> {   // 关机指令
                    masterOnly(sender)
                    sendQuoteReply(sender, originalMessage, "机器人正在关机······")
                    withContext(Dispatchers.IO) {
                        TimeUnit.SECONDS.sleep(1)
                    }
                    BuiltInCommands.StopCommand.run {
                        ConsoleCommandSender.handle()
                    }
                }

                "transfer", "转账"-> {   // bot积分转账
                    masterOnly(sender)
                    val qq = args[1]
                    val point = args[2]
                    sender.sendMessage("/pt transfer $qq $point")
                }

                "send", "发送"-> {   // bot消息发送
                    val qq = args[1].content.toLong()
                    var messages: MessageChain = messageChainOf()
                    args.forEachIndexed { index: Int, element ->
                        if (index == 2) { messages += element }
                        if (index > 2) { messages = messages + PlainText(" ") + element }
                    }
                    if (messages.isEmpty()) {
                        messages = messageChainOf(PlainText(" "))
                    }
                    if (qq == 0.toLong()) {
                        masterOnly(sender)
                        sender.sendMessage(messages)
                    } else {
                        try {
                            sender.bot?.getFriendOrFail(qq)!!.sendMessage(messages)
                            sender.sendMessage("发送私信成功")
                        } catch (ex: Exception) {
                            logger.warning(ex)
                            sender.sendMessage("出现错误：${ex}")
                        }
                    }
                }

                "WhiteList", "白名单"-> {   // 查看白名单列表
                    val showDesc = try {
                        args[1].content == "info" || args[1].content == "信息"
                    } catch (ex: Exception) {
                        false
                    }
                    var whiteListInfo = "白名单功能：$whiteEnable\n·白名单列表：\n"
                    for (key in BotConfig.WhiteList.keys) {
                        whiteListInfo += key
                        if (showDesc) {
                            whiteListInfo += " ${BotConfig.WhiteList[key]}"
                        }
                        whiteListInfo += "\n"
                    }
                    sendQuoteReply(sender, originalMessage, whiteListInfo)
                }

                "setWhiteList", "设置白名单"-> {   // 设置白名单功能状态
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    val option = args[1].content
                    if (enable.contains(option) && !BotConfig.WhiteList_enable) {
                        BotConfig.WhiteList_enable = true
                        BotConfig.save()
                        sendQuoteReply(sender, originalMessage, "已启用bot白名单功能")
                    } else if (disable.contains(option) && BotConfig.WhiteList_enable) {
                        BotConfig.WhiteList_enable = false
                        BotConfig.save()
                        sendQuoteReply(sender, originalMessage, "已关闭bot白名单功能")
                    } else {
                        sendQuoteReply(sender, originalMessage, "指令或状态错误！\n当前白名单状态：$whiteEnable")
                    }
                }

                "addWhiteList", "添加白名单"-> {   // 添加白名单
                    val group: Long = try {
                        args[1].content.toLong()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    } catch (ex: Exception) {
                        if (sender.subject is Friend || sender.isConsole()) {
                            throw PermissionDeniedException("Group only")
                        }
                        sender.subject!!.id
                    }
                    val desc = try {
                        args[2].content
                    } catch (ex: Exception) {
                        logger.warning {"error: ${ex.message}"}
                        "no_desc"
                    }
                    val result = BotConfig.WhiteList.put(group, desc)
                    if (result == null) {
                        BotConfig.WhiteList = BotConfig.WhiteList.toSortedMap()
                        sendQuoteReply(sender, originalMessage, "已将 $group 添加进白名单列表")
                    } else {
                        sendQuoteReply(sender, originalMessage, "$group 已存在，更新描述成功：$desc")
                    }
                    BotConfig.save()
                }

                "delWhiteList", "移除白名单"-> {   // 移除白名单
                    val group: Long = try {
                        args[1].content.toLong()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    } catch (ex: Exception) {
                        if (sender.subject is Friend || sender.isConsole()) {
                            throw PermissionDeniedException("Group only")
                        }
                        sender.subject!!.id
                    }
                    val result = BotConfig.WhiteList.remove(group)
                    if (result != null) {
                        BotConfig.save()
                        sendQuoteReply(sender, originalMessage, "已将 $group 移除白名单列表")
                    } else {
                        sendQuoteReply(sender, originalMessage, "白名单列表不存在群聊 $group")
                    }
                }

                "focus", "专注"-> {   // 专注模式
                    val option = args[1].content
                    if (option == "disable") {
                        BotConfig.focus_enable = false
                        BotConfig.focus_to = 0
                        BotConfig.save()
                        sendQuoteReply(sender, originalMessage,
                            "***专注模式 [已关闭]***\n已清除专注模式配置")
                    } else {
                        try {
                            BotConfig.focus_to = option.toLong()
                            BotConfig.save()
                            sendQuoteReply(sender, originalMessage,
                                "***专注模式 [已启用]***\nbot将专注于群聊 ${BotConfig.focus_to} 进行服务")
                        } catch (ex: NumberFormatException) {
                            sendQuoteReply(sender, originalMessage, "参数转换错误，请检查指令")
                        }
                    }
                }

                "reload", "重载"-> {   // 重载配置及数据
                    masterOnly(sender)
                    try {
                        TiedanGame.rdConfig()
                        TiedanGame.rdData()
                        sendQuoteReply(sender, originalMessage, "配置及数据已重载")
                    } catch (ex: Exception) {
                        logger.warning(ex)
                        sendQuoteReply(sender, originalMessage, "出现错误：${ex.message}")
                    }
                }

                "sendmail", "发送邮件"-> {
                    masterOnly(sender)
                    val address: String = try {
                        args[1].content
                    } catch (ex: Exception) {
                        MailConfig.log_mail
                    }
                    val session = buildMailSession {
                        MailConfig.properties.inputStream().use {
                            load(it)
                        }
                    }

                    val mail = buildMailContent(session) {
                        to = address
                        title = "日志备份"
                        text {
                            val plugins = File("plugins")
                            append("plugins: \n")
                            for (file in plugins.listFiles().orEmpty()) {
                                append("    ").append(file.name)
                                    .append(" ").append(file.length().div(1024)).append("KB").append('\n')
                            }
                            val libs = File("libs")
                            append("libs: \n")
                            for (file in libs.listFiles().orEmpty()) {
                                append("    ").append(file.name)
                                    .append(" ").append(file.length().div(1024)).append("KB").append('\n')
                            }
                        }
                        file("console.log") {
                            val logs = File("logs")
                            logs.listFiles()?.maxByOrNull { it.lastModified() }
                        }
                        file("network.log") {
                            val logs = File("bots/${sender.bot?.id}/logs")
                            logs.listFiles()?.maxByOrNull { it.lastModified() }
                        }
                    }

                    val current = Thread.currentThread()
                    val oc = current.contextClassLoader
                    try {
                        current.contextClassLoader = MailConfig::class.java.classLoader
                        jakarta.mail.Transport.send(mail)
                        sendQuoteReply(sender, originalMessage, "邮件发送成功")
                    } catch (cause: jakarta.mail.MessagingException) {
                        sendQuoteReply(sender, originalMessage, "邮件发送失败, cause: ${cause.message}")
                    } finally {
                        current.contextClassLoader = oc
                    }
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[操作无效] 请检查指令")
                }
            }
        } catch (ex: PermissionDeniedException) {
            sendQuoteReply(sender, originalMessage, "[操作无效] ${ex.message}")
        } catch (ex: Exception) {
            logger.warning {"error: ${ex.message}"}
            sendQuoteReply(sender, originalMessage, "[操作无效] 未知的参数")
        }
    }

    private fun masterOnly(sender: CommandSender) {
        if (sender.user?.id != BotConfig.master && sender.isNotConsole()) {
            throw PermissionDeniedException("Master Only")
        }
    }
}