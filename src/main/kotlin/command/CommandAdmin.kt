package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.adminOnly
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.masterOnly
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.buildMailContent
import com.tiedan.buildMailSession
import com.tiedan.config.BotConfig
import com.tiedan.config.MailConfig
import com.tiedan.plugindata.AdminListData
import com.tiedan.plugindata.BlackListData
import com.tiedan.plugindata.WhiteListData
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
    description = "管理员相关指令",
    usage = "${commandPrefix}admin help"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        try {
            adminOnly(sender)
        } catch (ex: PermissionDeniedException) {
            sendQuoteReply(sender, originalMessage, "${ex.message}")
            return
        }

        val whiteEnable: String = if (BotConfig.WhiteList_enable) { "已启用" } else { "未启用" }

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
                                "${commandPrefix}admin delWhiteList [group]\n" +
                                "-> 群聊相关操作\n" +
                                "${commandPrefix}admin group <操作>\n" +
                                "-> 消息发送\n" +
                                "${commandPrefix}admin send <qq> [message]"
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
                                "${commandPrefix}管理 移除白名单 [群号]\n" +
                                "-> 群聊相关操作\n" +
                                "${commandPrefix}管理 群聊 <操作>\n" +
                                "-> 消息发送\n" +
                                "${commandPrefix}管理 发送 <QQ号> [消息]"
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
                                "-> 配置及数据重载\n" +
                                "${commandPrefix}管理 重载\n" +
                                "-> 发送邮件备份日志\n" +
                                "${commandPrefix}管理 发送邮件 [邮件地址]"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "list", "列表"-> {   // 查看管理员列表
                    var adminListInfo = "·管理员列表："
                    for (admin in AdminListData.AdminList) {
                        adminListInfo += "\n$admin"
                    }
                    sendQuoteReply(sender, originalMessage, adminListInfo)
                }

                "op", "添加管理员"-> {   // 添加管理员
                    masterOnly(sender)
                    try {
                        val qq = args[1].content.toLong()
                        val result = AdminListData.AdminList.add(qq)
                        if (result) {
                            AdminListData.AdminList = AdminListData.AdminList.toSortedSet()
                            BotConfig.save()
                            if (qq == 0.toLong()) {   // 0视为all
                                sendQuoteReply(sender, originalMessage, "已解除管理员权限限制")
                            } else {
                                sendQuoteReply(sender, originalMessage, "已将 $qq 设为管理员")
                            }
                        } else {
                            sendQuoteReply(sender, originalMessage, "管理员已存在 $qq")
                        }
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "deop", "移除管理员"-> {   // 移除管理员
                    masterOnly(sender)
                    try {
                        val qq = args[1].content.toLong()
                        val result = AdminListData.AdminList.remove(qq)
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
                    var blackListInfo = "·黑名单列表："
                    for (black in BlackListData.BlackList) {
                        blackListInfo += "\n$black"
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
                    val qq = args[1].content.replace("@", "")
                    val point = args[2]
                    sender.sendMessage("/pt transfer $qq $point")
                }

                "send", "发送"-> {   // bot消息发送
                    val qq = args[1].content.toLong()
                    var messages: MessageChain = messageChainOf()
                    args.forEachIndexed { index: Int, element ->
                        if (index == 2) messages += element
                        if (index > 2) messages = messages + PlainText(" ") + element
                    }
                    if (messages.isEmpty()) {
                        messages = messageChainOf(PlainText(" "))
                    }
                    if (qq == 0.toLong()) {
                        masterOnly(sender)
                        sender.sendMessage(messages)
                    } else {
                        messages = messageChainOf(PlainText("${sender.name}(${sender.user?.id})给您发送了一条私信：\n") + messages)
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
                    val showDesc = args.getOrNull(1)?.content?.let { it == "info" || it == "信息" } ?: false
                    var whiteListInfo = "白名单功能：$whiteEnable\n白名单总数：${WhiteListData.WhiteList.size}\n·白名单列表："
                    for (key in WhiteListData.WhiteList.keys) {
                        whiteListInfo += "\n$key"
                        if (showDesc) {
                            whiteListInfo += " ${WhiteListData.WhiteList[key]}"
                        }
                    }
                    sendQuoteReply(sender, originalMessage, whiteListInfo)
                }

                "setWhiteList", "设置白名单"-> {   // 设置白名单功能状态
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    val option = args[1].content
                    when {
                        enable.contains(option) -> {
                            BotConfig.WhiteList_enable = true
                            sendQuoteReply(sender, originalMessage, "已启用bot白名单功能")
                        }
                        disable.contains(option) -> {
                            BotConfig.WhiteList_enable = false
                            sendQuoteReply(sender, originalMessage, "已关闭bot白名单功能")
                        }
                    }
                    BotConfig.save()
                }

                "addWhiteList", "addwhitelist", "添加白名单"-> {   // 添加白名单
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
                    val desc = args.getOrElse(2) { "no_desc" }.toString()
                    val result = WhiteListData.WhiteList.put(group, desc)
                    if (result == null) {
                        WhiteListData.WhiteList = WhiteListData.WhiteList.toSortedMap()
                        sendQuoteReply(sender, originalMessage, "已将 $group 添加进白名单列表")
                    } else {
                        sendQuoteReply(sender, originalMessage, "$group 已存在，更新描述成功：$desc")
                    }
                    BotConfig.save()
                }

                "delWhiteList", "delwhitelist", "移除白名单"-> {   // 移除白名单
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
                    val result = WhiteListData.WhiteList.remove(group)
                    if (result != null) {
                        BotConfig.save()
                        sendQuoteReply(sender, originalMessage, "已将 $group 移除白名单列表")
                    } else {
                        sendQuoteReply(sender, originalMessage, "白名单列表不存在群聊 $group")
                    }
                }

                "timezone", "时区"-> {   // 修改时区显示
                    masterOnly(sender)
                    val zone = args[1].content
                    val zoneName = args[2].content
                    BotConfig.TimeZone = mutableListOf(zone, zoneName)
                    BotConfig.save()
                    sendQuoteReply(sender, originalMessage, "时区显示已修改：${BotConfig.TimeZone[0]}（${BotConfig.TimeZone[1]}时间）")
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

                "group", "Group", "群聊"-> {
                    val groups = sender.bot?.groups
                    if (groups == null) {
                        sendQuoteReply(sender, originalMessage, "错误：获取群列表失败或群列表为空")
                        return
                    }
                    when (args[1].content) {
                        "info", "信息"-> {
                            val type = args.getOrElse(2) { "inactive" }.toString()
                            var activeCount = 0
                            var activeInfo = "【激活群聊信息】"
                            var inactiveInfo = "【未激活群聊信息】"
                            for (group in groups) {
                                if (group.id in WhiteListData.WhiteList) {
                                    activeCount++
                                    activeInfo += "\n${group.name}(${group.id}) [人数：${group.members.size + 1}]"
                                } else {
                                    inactiveInfo += "\n${group.name}(${group.id}) [人数：${group.members.size + 1}]"
                                }
                            }
                            val groupInfo = "白名单功能：$whiteEnable\n" +
                                            "群聊总数：${groups.size}\n" +
                                            "白名单总数：${WhiteListData.WhiteList.size}\n" +
                                            "激活群聊数：$activeCount\n" +
                                            "未知群聊数：${groups.size - activeCount}"
                            val forward = buildForwardMessage(sender.subject!!) {
                                displayStrategy = object : ForwardMessage.DisplayStrategy {
                                    override fun generateTitle(forward: RawForwardMessage): String = "群聊信息查询"
                                    override fun generateBrief(forward: RawForwardMessage): String = "[群聊信息]"
                                    override fun generatePreview(forward: RawForwardMessage): List<String> =
                                        listOf("白名单总数：${WhiteListData.WhiteList.size}", "激活群聊数：$activeCount", "未知群聊数：${groups.size - activeCount}")
                                    override fun generateSummary(forward: RawForwardMessage): String = "白名单功能：$whiteEnable"
                                }
                                sender.subject!!.bot says groupInfo
                                if (type == "active" || type == "all" || type == "激活" || type == "全部")
                                    sender.subject!!.bot says activeInfo
                                if (type == "inactive" || type == "all" || type == "未知" || type == "全部")
                                    sender.subject!!.bot says inactiveInfo
                            }
                            sender.sendMessage(forward)
                        }
                        "quit", "退群"-> {
                            masterOnly(sender)
                            val id = args[2].content.toLong()
                            if (id in groups) {
                                sender.bot?.getGroup(id)?.quit()
                                sendQuoteReply(sender, originalMessage, "退出群 $id 成功")
                            } else {
                                sendQuoteReply(sender, originalMessage, "错误：此群号不在群列表中")
                            }
                        }
                        "autoQuit", "自动退群"-> {
                            masterOnly(sender)
                            var count = 0
                            for (group in groups) {
                                if ((group.id in WhiteListData.WhiteList).not()) {
                                    group.sendMessage("【管理员操作自动退群】本群不在机器人白名单中，请联系机器人管理员申请白名单，或使用「${CommandManager.commandPrefix}apply white <群号> <原因>」指令发送白名单申请")
                                    group.quit()
                                    count++
                                }
                            }
                            sendQuoteReply(sender, originalMessage, "自动退出未知群聊 $count 个")
                        }
                        else-> {
                            sendQuoteReply(sender, originalMessage, "Group指令：未知的操作")
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

}