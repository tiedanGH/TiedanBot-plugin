package site.tiedan.command

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isNotConsole
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.adminOnly
import site.tiedan.TiedanGame.firstMasterQQ
import site.tiedan.TiedanGame.isAdmin
import site.tiedan.TiedanGame.isMaster
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.masterOnly
import site.tiedan.TiedanGame.permissionId
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.BotConfig
import site.tiedan.data.AdminListData
import site.tiedan.data.ApplyData
import site.tiedan.data.WhiteListData

object CommandApply : RawCommand(
    owner = TiedanGame,
    primaryName = "apply",
    secondaryNames = arrayOf("申请"),
    description = "申请操作指令",
    usage = "${commandPrefix}apply help"
){
    private val commandList = listOf(
        Command("apply private", "申请 私信", "✉️ 申请私信白名单", 1),
        Command("apply white <group> <reason>", "申请 白名单 <群号> <原因>", "📌 申请群聊白名单", 1),
        Command("apply admin <reason>", "申请 管理员 <原因>", "🔑 申请管理员权限", 1),
        Command("apply cancel", "申请 取消", "❎ 取消个人申请", 1),

        Command("apply list [type]", "申请 列表 [申请种类]", "📋 查看申请列表", 2),
        Command("apply handle <qq> <同意/拒绝> [备注]", "申请 处理 <申请人> <同意/拒绝> [备注]", "📝 处理申请", 2),

        Command("apply handleAll <type> <同意/拒绝/忽略>", "申请 批量处理 <申请种类> <同意/拒绝/忽略>", "📦 批量处理申请", 3)
    )

    private const val MAX_LENGTH = 150

    override suspend fun CommandSender.onCommand(args: MessageChain) {

        val qq = user?.id ?: 10000
        val hasAdminPermission = isAdmin(this)
        val hasMasterPermission = isMaster(this)

        try {
            when (args[0].content) {

                "help"-> {   // 查看apply帮助（help）
                    val reply = buildString {
                        append(" ·📮 apply指令帮助：\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        if (hasAdminPermission) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        }
                        if (hasMasterPermission) {
                            append(" ·👑 master管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        }
                        append("\n🔸<group>-群号\n🔸<reason>-申请原因")
                    }
                    sendQuoteReply(reply)
                }

                "帮助"-> {   // 查看apply帮助（帮助）
                    val reply = buildString {
                        append(" ·📮 apply指令帮助：\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        if (hasAdminPermission) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        }
                        if (hasMasterPermission) {
                            append(" ·👑 master管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        }
                        append("\n🔸<原因>-申请原因")
                    }
                    sendQuoteReply(reply)
                }

                "private", "私信"-> {   // 申请私信白名单（仅可在指定群聊内申请，自动通过无需审核）
                    if (BotConfig.apply_group == 0L) {
                        sendQuoteReply("私信白名单未启用，无需申请")
                        return
                    }
                    if (bot?.id != BotConfig.BotId) {
                        sendQuoteReply("当前bot无需申请私信白名单")
                        return
                    }
                    val subj = subject
                    if (subj !is Group || subj.id != BotConfig.apply_group) {
                        sendQuoteReply("私信白名单目前使用严格的名单管理，仅可在指定群聊内申请，请先加入群聊「${BotConfig.apply_group}」并在群内发送本指令")
                        return
                    }
                    val privateWhiteList = WhiteListData.PrivateWhiteList.getOrPut(bot!!.id) { mutableSetOf() }
                    if (privateWhiteList.add(qq)) {
                        WhiteListData.save()
                        sendQuoteReply("申请成功！已将您（$qq）加入私信白名单")
                    } else {
                        sendQuoteReply("您已在私信白名单中，无需重复申请")
                    }
                }

                "white", "白名单"-> {   // 申请群聊白名单
                    if (applyLock()) return
                    val group = try {
                        args[1].content.toLong()
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                        return
                    }
                    val reason = try {
                        if (args[2].content.length > MAX_LENGTH) {
                            sendQuoteReply("申请理由过长，上限为${MAX_LENGTH}个字符")
                            return
                        }
                        args[2].content
                    } catch (_: Exception) {
                        sendQuoteReply("reason为必填项")
                        return
                    }
                    ApplyData.WhiteListApplication[qq] = mutableMapOf()
                    ApplyData.WhiteListApplication[qq]?.set("name", name)
                    ApplyData.WhiteListApplication[qq]?.set("group", group.toString())
                    ApplyData.WhiteListApplication[qq]?.set("reason", reason)
                    ApplyData.ApplyLock.add(qq)
                    ApplyData.save()
                    sendQuoteReply("申请成功，等待管理员审核\n" +
                            "申请人：$name($qq)\n" +
                            "申请群号：$group\n" +
                            "原因：$reason")

                    val notice = "【新申请通知】\n" +
                            "申请内容：white\n" +
                            "申请人：$name($qq)\n" +
                            "白名单：$group\n" +
                            "原因：$reason"
                    try {
                        firstMasterQQ()?.let { bot?.getFriendOrFail(it)?.sendMessage(notice) }   // 抄送至bot所有者
                    } catch (e: Exception) {
                        logger.warning(e)
                    }
                }

                "admin", "管理员"-> {   // 申请admin权限
                    if (applyLock()) return
                    val reason = try {
                        val content = args[1].content
                        if (content.length > MAX_LENGTH) {
                            sendQuoteReply("申请理由过长，上限为${MAX_LENGTH}个字符")
                            return
                        }
                        "申请人：$name\n原因：${content}"
                    } catch (_: Exception) {
                        sendQuoteReply("reason为必填项")
                        return
                    }
                    ApplyData.AdminApplication[qq] = reason
                    ApplyData.ApplyLock.add(qq)
                    ApplyData.save()
                    sendQuoteReply("申请成功，等待管理员审核\n$reason")

                    val notice = "【新申请通知】\n" +
                            "申请内容：admin\n" +
                            "申请人QQ：$qq\n" +
                            reason
                    try {
                        firstMasterQQ()?.let { bot?.getFriendOrFail(it)?.sendMessage(notice) }   // 抄送至bot所有者
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
                        sendQuoteReply("取消申请成功")
                    } else {
                        sendQuoteReply("您还没有进行任何申请")
                    }
                }

                // admin操作
                "list", "列表"-> {   // 查看申请列表
                    adminOnly(this)
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
                    sendQuoteReply(reply)
                }

                "handle", "处理"-> {
                    adminOnly(this)
                    val handleQQ = try {
                        args[1].content.toLong()
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                        return
                    }
                    var option = args[2].content
                    val remark = args.getOrElse(3) { "申请处理(h*)" }.toString()
                    val type: String = if (ApplyData.WhiteListApplication.containsKey(handleQQ)) {
                        "white"
                    } else if (ApplyData.AdminApplication.containsKey(handleQQ)) {
                        "admin"
                    } else {
                        sendQuoteReply("操作失败，未找到此账号的申请记录")
                        return
                    }
                    if (arrayListOf("accept","同意").contains(option)) {
                        option = "同意"
                        when(type) {
                            "white"-> {
                                ApplyData.WhiteListApplication[handleQQ]?.get("group")?.let { WhiteListData.WhiteList[BotConfig.BotId]!!.put(it.toLong(), remark) }
                                WhiteListData.WhiteList[BotConfig.BotId] = WhiteListData.WhiteList[BotConfig.BotId]!!.toSortedMap()
                            }
                            "admin"-> {
                                permissionId(bot?.id, handleQQ)?.let { AdminListData.AdminList.add(it) }
                            }
                        }
                    } else if (arrayListOf("refuse","拒绝").contains(option)) {
                        option = "拒绝"
                    } else {
                        sendQuoteReply("[操作无效] 指令参数错误")
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
                        bot?.getFriendOrFail(handleQQ)?.sendMessage(noticeApply)   // 抄送结果至申请人

                        if (!isMaster(this) && isNotConsole()) {
                            reply += "\n\n处理结果已抄送至：${firstMasterQQ() ?: "MASTER"}"
                            var notice = "【其他申请处理结果】\n" +
                                        "处理人：$name($qq)\n" +
                                        "申请内容：${type}\n" +
                                        "申请人：${handleQQ}\n"
                            if (type == "white") {
                                notice += "白名单：${ApplyData.WhiteListApplication[handleQQ]?.get("group")}\n"
                            }
                            notice += "操作：$option\n" +
                                      "备注：$remark"
                            firstMasterQQ()?.let { bot?.getFriendOrFail(it)?.sendMessage(notice) }   // 抄送结果至bot所有者
                        }
                    } catch (e: Exception) {
                        logger.warning(e)
                        sendMessage("出现错误：${e.message}")
                    }
                    ApplyData.WhiteListApplication.remove(handleQQ)
                    ApplyData.AdminApplication.remove(handleQQ)
                    ApplyData.ApplyLock.remove(handleQQ)
                    BotConfig.save()
                    ApplyData.save()
                    sendQuoteReply(reply)   // 回复指令发出者
                }

                // master操作
                "handleAll", "批量处理"-> {
                    masterOnly(this)
                    val type = args[1].content
                    if (type != "white" && type != "admin" && type != "all") {
                        sendQuoteReply("无效的类型，仅支持 white、admin，或使用 all 处理全部申请")
                        return
                    }
                    val option = if (arrayListOf("accept","同意").contains(args[2].content)) {
                        "同意"
                    } else if (arrayListOf("refuse","拒绝").contains(args[2].content)) {
                        "拒绝"
                    } else if (arrayListOf("ignore","忽略").contains(args[2].content)) {
                        "忽略"
                    } else {
                        sendQuoteReply("无效的操作，仅支持 同意、拒绝、忽略")
                        return
                    }
                    var handleCount = 0
                    try {
                        if (type == "white" || type == "all") {
                            ApplyData.WhiteListApplication.keys.forEachIndexed { _, key ->
                                if (option == "同意") {
                                    ApplyData.WhiteListApplication[key]?.get("group")?.let { WhiteListData.WhiteList[BotConfig.BotId]!!.put(it.toLong(), "批量处理(h*)") }
                                    WhiteListData.WhiteList[BotConfig.BotId] = WhiteListData.WhiteList[BotConfig.BotId]!!.toSortedMap()
                                }
                                if (option != "忽略") {
                                    val noticeApply = "【申请处理通知】\n" +
                                                      "申请内容：white\n" +
                                                      "处理人：$name($qq)\n" +
                                                      "白名单：${ApplyData.WhiteListApplication[key]?.get("group")}\n" +
                                                      "结果：$option\n" +
                                                      "备注：批量处理(h*)"
                                    bot?.getFriendOrFail(key)?.sendMessage(noticeApply)   // 抄送结果至申请人
                                }
                                ApplyData.ApplyLock.remove(key)
                            }
                            handleCount += ApplyData.WhiteListApplication.size
                            ApplyData.WhiteListApplication.clear()
                        }
                        if (type == "admin" || type == "all") {
                            ApplyData.AdminApplication.keys.forEachIndexed { _, key ->
                                if (option == "同意") {
                                    permissionId(bot?.id, key)?.let { AdminListData.AdminList.add(it) }
                                }
                                if (option != "忽略") {
                                    val noticeApply = "【申请处理通知】\n" +
                                                      "申请内容：admin\n" +
                                                      "处理人：$name($qq)\n" +
                                                      "结果：$option\n" +
                                                      "备注：批量处理"
                                    bot?.getFriendOrFail(key)?.sendMessage(noticeApply)   // 抄送结果至申请人
                                }
                                ApplyData.ApplyLock.remove(key)
                            }
                            handleCount += ApplyData.AdminApplication.size
                            ApplyData.AdminApplication.clear()
                        }
                    } catch (e: Exception) {
                        logger.warning(e)
                        sendMessage("出现错误：${e.message}")
                    }
                    BotConfig.save()
                    ApplyData.save()
                    val reply = "批量处理申请成功！\n处理人：$name($qq)\n处理类别：$type\n操作：$option\n总处理数量：$handleCount"
                    sendQuoteReply(reply)   // 回复指令发出者
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${commandPrefix}apply help」来查看指令帮助")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}apply help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    private suspend fun CommandSender.applyLock(): Boolean {
        val qq = user?.id ?: 10000
        if (ApplyData.ApplyLock.contains(qq)) {
            sendQuoteReply("$name($qq)已经提交过申请，请等待审核完成后在执行新的操作，或使用指令「${commandPrefix}apply cancel」来取消个人申请")
            return true
        }
        return false
    }
}
