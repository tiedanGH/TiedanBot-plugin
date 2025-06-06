package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.Command
import com.tiedan.TiedanGame.adminOnly
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.masterOnly
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.config.BotConfig
import com.tiedan.plugindata.AdminListData
import com.tiedan.plugindata.ApplyData
import com.tiedan.plugindata.WhiteListData
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content

object CommandApply : RawCommand(
    owner = TiedanGame,
    primaryName = "apply",
    secondaryNames = arrayOf("申请"),
    description = "申请操作指令",
    usage = "${commandPrefix}apply help"
){
    private val commandList = listOf(
        Command("apply white <group> <reason>", "申请 白名单 <群号> <原因>", "📌 申请群聊白名单", 1),
        Command("apply admin <reason>", "申请 管理员 <原因>", "🔑 申请管理员权限", 1),
        Command("apply cancel", "申请 取消", "❎ 取消个人申请", 1),

        Command("apply list [type]", "申请 列表 [申请种类]", "📋 查看申请列表", 2),
        Command("apply handle <qq> <同意/拒绝> [备注]", "申请 处理 <申请人> <同意/拒绝> [备注]", "📝 处理申请", 2),

        Command("apply handleAll <type> <同意/拒绝/忽略>", "申请 批量处理 <申请种类> <同意/拒绝/忽略>", "📦 批量处理申请", 3)
    )

    private const val MAX_LENGTH = 150

    override suspend fun CommandContext.onCommand(args: MessageChain) {

        val qq = sender.user?.id ?: 10000
        val name = sender.name
        val isAdmin = AdminListData.AdminList.contains(sender.user?.id) || sender.user?.id == BotConfig.master || sender.isConsole()
        val isMaster = sender.user?.id == BotConfig.master || sender.isConsole()

        try {
            when (args[0].content) {

                "help"-> {   // 查看apply帮助（help）
                    val reply = buildString {
                        append(" ·📮 apply指令帮助：\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        if (isAdmin) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        }
                        if (isMaster) {
                            append(" ·👑 master管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        }
                        append("\n🔸<group>-群号\n🔸<reason>-申请原因")
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助"-> {   // 查看apply帮助（帮助）
                    val reply = buildString {
                        append(" ·📮 apply指令帮助：\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        if (isAdmin) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        }
                        if (isMaster) {
                            append(" ·👑 master管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        }
                        append("\n🔸<原因>-申请原因")
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "white", "白名单"-> {   // 申请群聊白名单
                    if (applyLock(sender, originalMessage)) return
                    val group = try {
                        args[1].content.toLong()
                    } catch (e: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    }
                    val reason = try {
                        if (args[2].content.length > MAX_LENGTH) {
                            sendQuoteReply(sender, originalMessage, "申请理由过长，上限为${MAX_LENGTH}个字符")
                            return
                        }
                        args[2].content
                    } catch (e: Exception) {
                        sendQuoteReply(sender, originalMessage, "reason为必填项")
                        return
                    }
                    ApplyData.WhiteListApplication[qq] = mutableMapOf()
                    ApplyData.WhiteListApplication[qq]?.set("name", name)
                    ApplyData.WhiteListApplication[qq]?.set("group", group.toString())
                    ApplyData.WhiteListApplication[qq]?.set("reason", reason)
                    ApplyData.ApplyLock.add(qq)
                    ApplyData.save()
                    sendQuoteReply(sender, originalMessage, "申请成功，等待管理员审核\n" +
                            "申请人：$name($qq)\n" +
                            "申请群号：$group\n" +
                            "原因：$reason")

                    val notice = "【新申请通知】\n" +
                            "申请内容：white\n" +
                            "申请人：$name($qq)\n" +
                            "白名单：$group\n" +
                            "原因：$reason"
                    try {
                        sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送至bot所有者
                    } catch (e: Exception) {
                        logger.warning(e)
                    }
                }

                "admin", "管理员"-> {   // 申请admin权限
                    if (applyLock(sender, originalMessage)) return
                    val reason = try {
                        val content = args[1].content
                        if (content.length > MAX_LENGTH) {
                            sendQuoteReply(sender, originalMessage, "申请理由过长，上限为${MAX_LENGTH}个字符")
                            return
                        }
                        "申请人：$name\n原因：${content}"
                    } catch (e: Exception) {
                        sendQuoteReply(sender, originalMessage, "reason为必填项")
                        return
                    }
                    ApplyData.AdminApplication[qq] = reason
                    ApplyData.ApplyLock.add(qq)
                    ApplyData.save()
                    sendQuoteReply(sender, originalMessage, "申请成功，等待管理员审核\n$reason")

                    val notice = "【新申请通知】\n" +
                            "申请内容：admin\n" +
                            "申请人QQ：$qq\n" +
                            reason
                    try {
                        sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送至bot所有者
                    } catch (e: Exception) {
                        logger.warning(e)
                    }
                }

                "cancel", "取消"-> {   // 取消个人申请
                    ApplyData.WhiteListApplication.remove(qq)
                    ApplyData.AdminApplication.remove(qq)
                    val result = ApplyData.ApplyLock.remove(qq)
                    ApplyData.save()
                    if (result) {
                        sendQuoteReply(sender, originalMessage, "取消申请成功")
                    } else {
                        sendQuoteReply(sender, originalMessage, "您还没有进行任何申请")
                    }
                }

                // admin操作
                "list", "列表"-> {   // 查看申请列表
                    adminOnly(sender)
                    val type = args.getOrElse(1) { "all" }.toString()
                    var reply = ""
                    if (type == "white" || type == "all") {   // 查看白名单申请列表
                        reply += "-> 白名单申请列表：\n"
                        ApplyData.WhiteListApplication.keys.forEachIndexed { index, key ->
                            reply += "·No.${index + 1} QQ号：$key\n" +
                                     "申请人：${ApplyData.WhiteListApplication[key]?.get("name")}\n" +
                                     "申请群号：${ApplyData.WhiteListApplication[key]?.get("group")}\n" +
                                     "原因：${ApplyData.WhiteListApplication[key]?.get("reason")}\n"
                        }
                        reply += "\n"
                    }
                    if (type == "admin" || type == "all") {   // 查看admin申请列表
                        reply += "-> admin申请列表：\n"
                        ApplyData.AdminApplication.keys.forEachIndexed { index, key ->
                            reply += "·No.${index + 1} QQ号：$key\n${ApplyData.AdminApplication[key]}\n"
                        }
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "handle", "处理"-> {
                    adminOnly(sender)
                    val handleQQ = try {
                        args[1].content.toLong()
                    } catch (e: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    }
                    var option = args[2].content
                    val remark = args.getOrElse(3) { "申请处理(h*)" }.toString()
                    val type: String = if (ApplyData.WhiteListApplication.containsKey(handleQQ)) {
                        "white"
                    } else if (ApplyData.AdminApplication.containsKey(handleQQ)) {
                        "admin"
                    } else {
                        sendQuoteReply(sender, originalMessage, "操作失败，未找到此账号的申请记录")
                        return
                    }
                    if (arrayListOf("accept","同意").contains(option)) {
                        option = "同意"
                        when(type) {
                            "white"-> {
                                ApplyData.WhiteListApplication[handleQQ]?.get("group")?.let { WhiteListData.WhiteList.put(it.toLong(), remark) }
                                WhiteListData.WhiteList = WhiteListData.WhiteList.toSortedMap()
                            }
                            "admin"-> {
                                AdminListData.AdminList.add(handleQQ)
                            }
                        }
                    } else if (arrayListOf("refuse","拒绝").contains(option)) {
                        option = "拒绝"
                    } else {
                        sendQuoteReply(sender, originalMessage, "[操作无效] 指令参数错误")
                        return
                    }
                    var reply = "申请处理成功！\n处理人：$name($qq)\n操作：$option\n备注：$remark"
                    try {
                        var noticeApply = "【申请处理通知】\n" +
                                    "申请内容：${type}\n" +
                                    "处理人：$name($qq)\n"
                        if (type == "white") {
                            noticeApply += "白名单：${ApplyData.WhiteListApplication[handleQQ]?.get("group")}\n"
                        }
                        noticeApply += "结果：$option\n" +
                                       "备注：$remark"
                        sender.bot?.getFriendOrFail(handleQQ)?.sendMessage(noticeApply)   // 抄送结果至申请人

                        if (qq != BotConfig.master && sender.isNotConsole()) {
                            reply += "\n\n处理结果已抄送至：${BotConfig.master}"
                            var notice = "【其他申请处理结果】\n" +
                                        "处理人：$name($qq)\n" +
                                        "申请内容：${type}\n" +
                                        "申请人：${handleQQ}\n"
                            if (type == "white") {
                                notice += "白名单：${ApplyData.WhiteListApplication[handleQQ]?.get("group")}\n"
                            }
                            notice += "操作：$option\n" +
                                      "备注：$remark"
                            sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送结果至bot所有者
                        }
                    } catch (e: Exception) {
                        logger.warning(e)
                        sender.sendMessage("出现错误：${e.message}")
                    }
                    ApplyData.WhiteListApplication.remove(handleQQ)
                    ApplyData.AdminApplication.remove(handleQQ)
                    ApplyData.ApplyLock.remove(handleQQ)
                    BotConfig.save()
                    ApplyData.save()
                    sendQuoteReply(sender, originalMessage, reply)   // 回复指令发出者
                }

                // master操作
                "handleAll", "批量处理"-> {
                    masterOnly(sender)
                    val type = args[1].content
                    if (type != "white" && type != "admin" && type != "all") {
                        sendQuoteReply(sender, originalMessage, "无效的类型，仅支持 white、admin，或使用 all 处理全部申请")
                        return
                    }
                    val option = if (arrayListOf("accept","同意").contains(args[2].content)) {
                        "同意"
                    } else if (arrayListOf("refuse","拒绝").contains(args[2].content)) {
                        "拒绝"
                    } else if (arrayListOf("ignore","忽略").contains(args[2].content)) {
                        "忽略"
                    } else {
                        sendQuoteReply(sender, originalMessage, "无效的操作，仅支持 同意、拒绝、忽略")
                        return
                    }
                    var handleCount = 0
                    try {
                        if (type == "white" || type == "all") {
                            ApplyData.WhiteListApplication.keys.forEachIndexed { _, key ->
                                if (option == "同意") {
                                    ApplyData.WhiteListApplication[key]?.get("group")?.let { WhiteListData.WhiteList.put(it.toLong(), "批量处理(h*)") }
                                    WhiteListData.WhiteList = WhiteListData.WhiteList.toSortedMap()
                                }
                                if (option != "忽略") {
                                    val noticeApply = "【申请处理通知】\n" +
                                                      "申请内容：white\n" +
                                                      "处理人：$name($qq)\n" +
                                                      "白名单：${ApplyData.WhiteListApplication[key]?.get("group")}\n" +
                                                      "结果：$option\n" +
                                                      "备注：批量处理(h*)"
                                    sender.bot?.getFriendOrFail(key)?.sendMessage(noticeApply)   // 抄送结果至申请人
                                }
                                ApplyData.ApplyLock.remove(key)
                            }
                            handleCount += ApplyData.WhiteListApplication.size
                            ApplyData.WhiteListApplication.clear()
                        }
                        if (type == "admin" || type == "all") {
                            ApplyData.AdminApplication.keys.forEachIndexed { _, key ->
                                if (option == "同意") {
                                    AdminListData.AdminList.add(key)
                                }
                                if (option != "忽略") {
                                    val noticeApply = "【申请处理通知】\n" +
                                                      "申请内容：admin\n" +
                                                      "处理人：$name($qq)\n" +
                                                      "结果：$option\n" +
                                                      "备注：批量处理"
                                    sender.bot?.getFriendOrFail(key)?.sendMessage(noticeApply)   // 抄送结果至申请人
                                }
                                ApplyData.ApplyLock.remove(key)
                            }
                            handleCount += ApplyData.AdminApplication.size
                            ApplyData.AdminApplication.clear()
                        }
                    } catch (e: Exception) {
                        logger.warning(e)
                        sender.sendMessage("出现错误：${e.message}")
                    }
                    BotConfig.save()
                    ApplyData.save()
                    val reply = "批量处理申请成功！\n处理人：$name($qq)\n处理类别：$type\n操作：$option\n总处理数量：$handleCount"
                    sendQuoteReply(sender, originalMessage, reply)   // 回复指令发出者
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「${commandPrefix}apply help」来查看指令帮助")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply(sender, originalMessage, "[操作无效] ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${commandPrefix}apply help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply(sender, originalMessage, "[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    private suspend fun applyLock(sender: CommandSender, originalMessage: MessageChain): Boolean {
        val qq = sender.user?.id ?: 10000
        val name = sender.name
        if (ApplyData.ApplyLock.contains(qq)) {
            sendQuoteReply(sender, originalMessage,
                    "$name($qq)已经提交过申请，请等待审核完成后在执行新的操作，或使用指令「${commandPrefix}apply cancel」来取消个人申请")
            return true
        }
        return false
    }
}