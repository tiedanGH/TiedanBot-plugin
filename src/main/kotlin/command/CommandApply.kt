package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.COMMAND_PREFIX
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.config.BotConfig
import com.tiedan.plugindata.ApplyData
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.warning

object CommandApply : RawCommand(
    owner = TiedanGame,
    primaryName = "apply",
    secondaryNames = arrayOf("申请"),
    description = "申请操作指令"
){
    private const val MAX_LENGTH = 80

    override suspend fun CommandContext.onCommand(args: MessageChain) {

        val qq = try {
            sender.user!!.id
        } catch (ex: NullPointerException) {
            logger.warning { "ConsoleCommandSender-> set to -1" }
            -1
        }
        val name = sender.name

        try {
            when (args[0].content) {

                "help"-> {   // 查看apply帮助（help）
                    var reply = "·apply指令帮助：\n" +
                                "-> 申请群聊白名单\n" +
                                "${COMMAND_PREFIX}apply white <group> <reason>\n" +
                                "-> 申请admin权限\n" +
                                "${COMMAND_PREFIX}apply admin <reason>\n" +
                                "-> 取消个人申请\n" +
                                "${COMMAND_PREFIX}apply cancel\n"
                    if (BotConfig.AdminList.contains(sender.user?.id) || sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n" +
                                "·admin管理指令：\n" +
                                "-> 查看申请列表\n" +
                                "${COMMAND_PREFIX}apply list [type]\n" +
                                "-> 处理申请\n" +
                                "${COMMAND_PREFIX}apply handle <qq> <同意/拒绝> [备注]\n"
                    }
                    reply  += "\n<>为必填项，group-群号，reason-申请原因"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助"-> {   // 查看apply帮助（帮助）
                    var reply = "·apply指令帮助：\n" +
                            "-> 申请群聊白名单\n" +
                            "${COMMAND_PREFIX}申请 白名单 <群号> <原因>\n" +
                            "-> 申请管理员权限\n" +
                            "${COMMAND_PREFIX}申请 管理员 <原因>\n" +
                            "-> 取消个人申请\n" +
                            "${COMMAND_PREFIX}申请 取消"
                    if (BotConfig.AdminList.contains(sender.user?.id) || sender.user?.id == BotConfig.master || sender.isConsole()) {
                        reply += "\n\n" +
                                "·admin管理指令：\n" +
                                "-> 查看申请列表\n" +
                                "${COMMAND_PREFIX}申请 列表 [申请种类]\n" +
                                "-> 处理申请\n" +
                                "${COMMAND_PREFIX}申请 处理 <申请人> <同意/拒绝> [备注]"
                    }
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "white", "白名单"-> {   // 申请群聊白名单
                    if (applyLock(sender, originalMessage)) { return }
                    val group = try {
                        args[1].content.toLong()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    }
                    val reason = try {
                        if (args[2].content.length > MAX_LENGTH) {
                            sendQuoteReply(sender, originalMessage, "申请理由过长，上限为80个字符")
                            return
                        }
                        args[2].content
                    } catch (ex: Exception) {
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

                    val notice = "[新申请通知]\n" +
                            "申请内容：white\n" +
                            "申请人：$name($qq)\n" +
                            "白名单：$group"
                    try {
                        sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送至bot所有者
                    } catch (ex: Exception) {
                        logger.warning(ex)
                    }
                }

                "admin", "管理员"-> {   // 申请admin权限
                    if (applyLock(sender, originalMessage)) { return }
                    val reason = try {
                        val content = args[1].content
                        if (content.length > MAX_LENGTH) {
                            sendQuoteReply(sender, originalMessage, "申请理由过长，上限为80个字符")
                            return
                        }
                        "申请人：$name\n原因：${content}"
                    } catch (ex: Exception) {
                        sendQuoteReply(sender, originalMessage, "reason为必填项")
                        return
                    }
                    ApplyData.AdminApplication[qq] = reason
                    ApplyData.ApplyLock.add(qq)
                    ApplyData.save()
                    sendQuoteReply(sender, originalMessage, "申请成功，等待管理员审核\n$reason")

                    val notice = "[新申请通知]\n" +
                            "申请内容：white\n" +
                            "申请人：$name($qq)"
                    try {
                        sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送至bot所有者
                    } catch (ex: Exception) {
                        logger.warning(ex)
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
                    val type = try {
                        args[1].content
                    } catch (ex: Exception) {
                        "all"
                    }
                    var reply = ""
                    if (type == "white" || type == "all"){   // 查看白名单申请列表
                        reply += "-> 白名单申请列表：\n"
                        ApplyData.WhiteListApplication.keys.forEachIndexed { index, key ->
                            reply += "·No.${index + 1} QQ号：$key\n" +
                                    "申请人：${ApplyData.WhiteListApplication[key]?.get("name")}\n" +
                                    "申请群号：${ApplyData.WhiteListApplication[key]?.get("group")}\n" +
                                    "原因：${ApplyData.WhiteListApplication[key]?.get("reason")}\n"
                        }
                    }
                    if (type == "admin" || type == "all"){   // 查看admin申请列表
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
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    }
                    var option = args[2].content
                    val remark = try {
                        args[3].content
                    } catch (ex: Exception) {
                        "申请处理"
                    }
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
                                ApplyData.WhiteListApplication[handleQQ]?.get("group")?.let { BotConfig.WhiteList.put(it.toLong(), remark) }
                                BotConfig.WhiteList = BotConfig.WhiteList.toSortedMap()
                            }
                            "admin"-> {
                                BotConfig.AdminList.add(handleQQ)
                            }
                        }
                    } else if (arrayListOf("refuse","拒绝").contains(option)) {
                        option = "拒绝"
                    } else {
                        sendQuoteReply(sender, originalMessage, "[操作无效] 指令参数错误")
                        return
                    }
                    var reply = "请求处理成功！\n处理人：$name($qq)\n操作：$option"
                    try {
                        var noticeApply = "[申请处理通知]\n" +
                                    "申请人：${handleQQ}\n" +
                                    "申请内容：${type}\n"
                        if (type == "white") {
                            noticeApply += "白名单：${ApplyData.WhiteListApplication[handleQQ]?.get("group")}\n"
                        }
                        noticeApply += "结果：$option"
                        sender.bot?.getFriendOrFail(handleQQ)?.sendMessage(noticeApply)   // 抄送结果至申请人

                        if (qq != BotConfig.master && sender.isNotConsole()) {
                            reply += "\n\n结果已抄送至：${BotConfig.master}"
                            var notice = "[申请处理结果]\n" +
                                        "处理人：$name($qq)\n" +
                                        "申请内容：${type}\n" +
                                        "申请人：${handleQQ}\n"
                            if (type == "white") {
                                notice += "白名单：${ApplyData.WhiteListApplication[handleQQ]?.get("group")}\n"
                            }
                            notice += "操作：$option"
                            sender.bot?.getFriendOrFail(BotConfig.master)?.sendMessage(notice)   // 抄送结果至bot所有者
                        }
                    } catch (ex: Exception) {
                        logger.warning {"error: ${ex.message}"}
                    }
                    ApplyData.WhiteListApplication.remove(handleQQ)
                    ApplyData.AdminApplication.remove(handleQQ)
                    ApplyData.ApplyLock.remove(handleQQ)
                    BotConfig.save()
                    ApplyData.save()
                    sendQuoteReply(sender, originalMessage, reply)   // 回复指令发出者
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「${COMMAND_PREFIX}apply help」来查看指令帮助")
                }
            }
        } catch (ex: PermissionDeniedException) {
            sendQuoteReply(sender, originalMessage, "[操作无效] ${ex.message}")
        } catch (ex: Exception) {
            logger.warning {"error: ${ex.message}"}
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${COMMAND_PREFIX}apply help」来查看指令帮助")
        }
    }

    private fun adminOnly(sender: CommandSender) {
        if (!BotConfig.AdminList.contains(sender.user?.id) && sender.user?.id != BotConfig.master && sender.isNotConsole()) {
            throw PermissionDeniedException("\nPermission Denied")
        }
    }

    private suspend fun applyLock(sender: CommandSender, originalMessage: MessageChain): Boolean {
        val qq = try {
            sender.user!!.id
        } catch (ex: NullPointerException) {
            0
        }
        val name = sender.name
        if (ApplyData.ApplyLock.contains(qq)) {
            sendQuoteReply(sender, originalMessage,
                    "$name($qq)已经提交过申请，请等待审核完成后在执行新的操作，或使用指令「${COMMAND_PREFIX}apply cancel」来取消个人申请")
            return true
        }
        return false
    }
}