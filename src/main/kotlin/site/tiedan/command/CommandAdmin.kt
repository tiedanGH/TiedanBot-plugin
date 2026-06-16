package site.tiedan.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.*
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.adminConfigId
import site.tiedan.TiedanGame.adminOnly
import site.tiedan.TiedanGame.firstMasterQQ
import site.tiedan.TiedanGame.isMaster
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.masterOnly
import site.tiedan.TiedanGame.permissionId
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.BotConfig
import site.tiedan.config.FunctionConfig
import site.tiedan.config.MailConfig
import site.tiedan.data.AdminListData
import site.tiedan.data.BlackListData
import site.tiedan.data.WhiteListData
import site.tiedan.module.buildMailContent
import site.tiedan.module.buildMailSession
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
    private val commandList = listOf(
        Command("admin list", "管理 列表", "查看管理员列表", 1),
        Command("admin BlackList", "管理 黑名单", "查看黑名单列表", 1),
        Command("admin black <qq>", "管理 黑名单 <QQ号>", "添加/移除黑名单", 1),
        Command("admin WhiteList [info]", "管理 白名单 [信息]", "查看白名单列表", 1),
        Command("admin setWhiteList <开启/关闭>", "管理 设置白名单 <开启/关闭>", "设置白名单开关状态", 1),
        Command("admin addWhiteList [group] [desc]", "管理 添加白名单 [群号] [描述]", "添加白名单", 1),
        Command("admin delWhiteList [group]", "管理 移除白名单 [群号]", "移除白名单", 1),
        Command("admin group <操作>", "管理 群聊 <操作>", "群聊相关操作", 1),
        Command("admin send <qq> [message]", "管理 发送 <QQ号> [消息]", "消息发送", 1),

        Command("admin op/deop <qq>", "管理 添加/移除管理员 <QQ号>", "添加/移除管理员", 2),
        Command("admin shutdown", "管理 关机", "机器人关机", 2),
        Command("admin transfer <qq> <point>", "管理 转账 <QQ号> <积分>", "积分转账", 2),
        Command("admin reload", "管理 重载", "配置及数据重载", 2),
        Command("admin sendmail [address]", "管理 发送邮件 [邮件地址]", "发送邮件备份日志", 2),
    )


    override suspend fun CommandSender.onCommand(args: MessageChain) {

        try {
            adminOnly(this)
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("${e.message}")
            return
        }

        val whiteEnable: String = if (FunctionConfig.WhiteList_enable) { "已启用" } else { "未启用" }

        try {
            when (args[0].content) {

                "help"-> {   // 查看admin可用帮助（help）
                    var reply = " ·🔧 admin可用帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "-> ${it.desc}\n${commandPrefix}${it.usage}\n" }
                    if (isMaster(this)) {
                        reply += " ·👑 master管理指令：\n" +
                            commandList.filter { it.type == 2 }.joinToString("") { "-> ${it.desc}\n${commandPrefix}${it.usage}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "帮助"-> {   // 查看admin可用帮助（帮助）
                    var reply = " ·🔧 admin可用帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "-> ${commandPrefix}${it.usageCN}　${it.desc}\n" }
                    if (isMaster(this)) {
                        reply += " ·👑 master管理指令：\n" +
                            commandList.filter { it.type == 2 }.joinToString("") { "-> ${commandPrefix}${it.usageCN}　${it.desc}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "list", "列表"-> {   // 查看管理员列表
                    var adminListInfo = "·管理员列表："
                    for (admin in AdminListData.AdminList) {
                        adminListInfo += "\n$admin"
                    }
                    sendQuoteReply(adminListInfo)
                }

                "op", "添加管理员"-> {   // 添加管理员
                    masterOnly(this)
                    try {
                        val qq = adminConfigId(this, args[1].content)
                        val result = AdminListData.AdminList.add(qq)
                        if (result) {
                            AdminListData.AdminList = AdminListData.AdminList.toSortedSet()
                            AdminListData.save()
                            if (qq == "0") {   // 0视为all
                                sendQuoteReply("已解除管理员权限限制")
                            } else {
                                sendQuoteReply("已将 $qq 设为管理员")
                            }
                        } else {
                            sendQuoteReply("管理员已存在 $qq")
                        }
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                    } catch (e: IllegalArgumentException) {
                        sendQuoteReply("[ID格式错误] ${e.message}")
                    }
                }

                "deop", "移除管理员"-> {   // 移除管理员
                    masterOnly(this)
                    try {
                        val qq = adminConfigId(this, args[1].content)
                        val result = AdminListData.AdminList.remove(qq)
                        if (result) {
                            AdminListData.save()
                            if (qq == "0") {   // 0视为all
                                sendQuoteReply("已恢复管理员权限限制")
                            } else {
                                sendQuoteReply("已将 $qq 移除管理员")
                            }
                        } else {
                            sendQuoteReply("不存在管理员 $qq")
                        }
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                    } catch (e: IllegalArgumentException) {
                        sendQuoteReply("[ID格式错误] ${e.message}")
                    }
                }

                "BlackList", "黑名单"-> {   // 查看黑名单列表
                    var blackListInfo = "·黑名单列表："
                    for (black in BlackListData.BlackList) {
                        blackListInfo += "\n$black"
                    }
                    sendQuoteReply(blackListInfo)
                }

                "black", "添加黑名单"-> {   // 添加/移除黑名单
                    try {
                        val qq = args[1].content.replace("@", "").toLong()
                        if (isMaster(bot?.id, qq)) {
                            sendQuoteReply("操作保护：Master不能被移入黑名单")
                            return
                        }
                        if (qq == user?.id) {
                            sendQuoteReply("操作保护：不能把自己移入黑名单")
                            return
                        }
                        if (permissionId(bot?.id, qq)?.let { it in AdminListData.AdminList } == true) {
                            sendQuoteReply("操作保护：管理员不能被移入黑名单")
                            return
                        }
                        var reply = ""
                        var type: String
                        if (BlackListData.BlackList.contains(qq)) {
                            BlackListData.BlackList.remove(qq)
                            reply = "已将 $qq 移出黑名单"
                            type = "移出黑名单"
                        } else {
                            BlackListData.BlackList.add(qq)
                            reply = "已将 $qq 移入黑名单"
                            type = "移入黑名单"
                        }
                        try {
                            if (!isMaster(this) && isNotConsole()) {
                                reply += "\n\n操作结果已抄送至 MASTER"
                                var notice = "【黑名单指令操作】\n" +
                                        "处理人：$name(${user?.id})\n" +
                                        "操作：$type\n" +
                                        "目标ID：$qq"
                                firstMasterQQ()?.let { bot?.getFriendOrFail(it)?.sendMessage(notice) }   // 抄送结果至bot所有者
                            }
                        } catch (e: Exception) {
                            logger.warning(e)
                            sendMessage("出现错误：${e.message}")
                        }
                        sendQuoteReply(reply)
                        BlackListData.save()
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                    }
                }

                "shutdown", "关机"-> {   // 关机指令
                    masterOnly(this)
                    sendQuoteReply("机器人正在关机······")
                    withContext(Dispatchers.IO) {
                        TimeUnit.SECONDS.sleep(1)
                    }
                    BuiltInCommands.StopCommand.run {
                        ConsoleCommandSender.handle()
                    }
                }

                "transfer", "转账"-> {   // bot积分转账
                    masterOnly(this)
                    val qq = args[1].content.replace("@", "")
                    val point = args[2]
                    sendMessage("/pt transfer $qq $point")
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
                        masterOnly(this)
                        sendMessage(messages)
                    } else {
                        messages = messageChainOf(PlainText("${name}(${user?.id})给您发送了一条私信：\n") + messages)
                        try {
                            bot?.getFriendOrFail(qq)!!.sendMessage(messages)
                            sendMessage("发送私信成功")
                        } catch (e: Exception) {
                            logger.warning(e)
                            sendMessage("出现错误：${e}")
                        }
                    }
                }

                "WhiteList", "whitelist", "白名单"-> {   // 查看白名单列表
                    if (WhiteListData.WhiteList.contains(BotConfig.BotId).not()) {
                        sendQuoteReply("当前账号下无任何白名单记录")
                        return
                    }
                    val showDesc = args.getOrNull(1)?.content?.let { it == "info" || it == "信息" } == true
                    var whiteListInfo = "白名单功能：$whiteEnable\n白名单总数：${WhiteListData.WhiteList[BotConfig.BotId]!!.size}\n·白名单列表："
                    for (key in WhiteListData.WhiteList[BotConfig.BotId]!!.keys) {
                        whiteListInfo += "\n$key"
                        if (showDesc) {
                            whiteListInfo += " ${WhiteListData.WhiteList[BotConfig.BotId]!![key]}"
                        }
                    }
                    sendQuoteReply(whiteListInfo)
                }

                "setWhiteList", "setwhitelist", "设置白名单"-> {   // 设置白名单功能状态
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    val option = args[1].content
                    when (option) {
                        in enable -> {
                            FunctionConfig.WhiteList_enable = true
                            sendQuoteReply("已启用bot白名单功能")
                        }
                        in disable -> {
                            FunctionConfig.WhiteList_enable = false
                            sendQuoteReply("已关闭bot白名单功能")
                        }
                        else-> {
                            sendQuoteReply("无效配置：请设置 开启/关闭 白名单功能")
                        }
                    }
                    FunctionConfig.save()
                }

                "addWhiteList", "addwhitelist", "添加白名单"-> {   // 添加白名单
                    val group: Long = try {
                        args[1].content.toLong()
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                        return
                    } catch (_: Exception) {
                        if (subject is Friend || isConsole()) {
                            throw PermissionDeniedException("Group only")
                        }
                        subject!!.id
                    }
                    val desc = args.getOrElse(2) { "no_desc" }.toString()
                    val result = WhiteListData.WhiteList[BotConfig.BotId]!!.put(group, desc)
                    if (result == null) {
                        WhiteListData.WhiteList[BotConfig.BotId] = WhiteListData.WhiteList[BotConfig.BotId]!!.toSortedMap()
                        sendQuoteReply("已将 $group 添加进白名单列表")
                    } else {
                        sendQuoteReply("$group 已存在，更新描述成功：$desc")
                    }
                    BotConfig.save()
                }

                "delWhiteList", "delwhitelist", "移除白名单"-> {   // 移除白名单
                    val group: Long = try {
                        args[1].content.toLong()
                    } catch (_: NumberFormatException) {
                        sendQuoteReply("数字转换错误，请检查指令")
                        return
                    } catch (_: Exception) {
                        if (subject is Friend || isConsole()) {
                            throw PermissionDeniedException("Group only")
                        }
                        subject!!.id
                    }
                    val result = WhiteListData.WhiteList[BotConfig.BotId]!!.remove(group)
                    if (result != null) {
                        BotConfig.save()
                        sendQuoteReply("已将 $group 移除白名单列表")
                    } else {
                        sendQuoteReply("白名单列表不存在群聊 $group")
                    }
                }

                "timezone", "时区"-> {   // 修改时区显示
                    masterOnly(this)
                    val zone = args[1].content
                    val zoneName = args[2].content
                    BotConfig.TimeZone = mutableListOf(zone, zoneName)
                    BotConfig.save()
                    sendQuoteReply("时区显示已修改：${BotConfig.TimeZone[0]}（${BotConfig.TimeZone[1]}时间）")
                }

                "focus", "专注"-> {   // 专注模式
                    masterOnly(this)
                    val option = args[1].content
                    if (option == "disable") {
                        FunctionConfig.focus_enable = false
                        FunctionConfig.focus_to = 0
                        FunctionConfig.save()
                        sendQuoteReply("***专注模式 [已关闭]***\n已清除专注模式配置")
                    } else {
                        try {
                            FunctionConfig.focus_to = option.toLong()
                            FunctionConfig.save()
                            sendQuoteReply("***专注模式 [已启用]***\nbot将专注于群聊 ${FunctionConfig.focus_to} 进行服务")
                        } catch (_: NumberFormatException) {
                            sendQuoteReply("参数转换错误，请检查指令")
                        }
                    }
                }

                "group", "Group", "群聊"-> {
                    val groups = bot?.groups
                    if (groups == null) {
                        sendQuoteReply("错误：获取群列表失败或群列表为空")
                        return
                    }
                    when (args[1].content) {
                        "info", "信息"-> {
                            val type = args.getOrElse(2) { "inactive" }.toString()
                            var activeCount = 0
                            var activeInfo = "【激活群聊信息】"
                            var inactiveInfo = "【未激活群聊信息】"
                            for (group in groups) {
                                if (group.id in WhiteListData.WhiteList[BotConfig.BotId]!!) {
                                    activeCount++
                                    activeInfo += "\n${group.name}(${group.id}) [人数：${group.members.size}]"
                                } else {
                                    inactiveInfo += "\n${group.name}(${group.id}) [人数：${group.members.size}]"
                                }
                            }
                            val groupInfo = "白名单功能：$whiteEnable\n" +
                                            "群聊总数：${groups.size}\n" +
                                            "白名单总数：${WhiteListData.WhiteList[BotConfig.BotId]!!.size}\n" +
                                            "激活群聊数：$activeCount\n" +
                                            "未知群聊数：${groups.size - activeCount}"
                            val forward = buildForwardMessage(subject!!) {
                                displayStrategy = object : ForwardMessage.DisplayStrategy {
                                    override fun generateTitle(forward: RawForwardMessage): String = "群聊信息查询"
                                    override fun generateBrief(forward: RawForwardMessage): String = "[群聊信息]"
                                    override fun generatePreview(forward: RawForwardMessage): List<String> =
                                        listOf("白名单总数：${WhiteListData.WhiteList[BotConfig.BotId]!!.size}", "激活群聊数：$activeCount", "未知群聊数：${groups.size - activeCount}")
                                    override fun generateSummary(forward: RawForwardMessage): String = "白名单功能：$whiteEnable"
                                }
                                subject!!.bot says groupInfo
                                if (type == "active" || type == "all" || type == "激活" || type == "全部")
                                    subject!!.bot says activeInfo
                                if (type == "inactive" || type == "all" || type == "未知" || type == "全部")
                                    subject!!.bot says inactiveInfo
                            }
                            sendMessage(forward)
                        }
                        "quit", "退群"-> {
                            masterOnly(this)
                            val id = args[2].content.toLong()
                            if (id in groups) {
                                bot?.getGroup(id)?.quit()
                                sendQuoteReply("退出群 $id 成功")
                            } else {
                                sendQuoteReply("错误：此群号不在群列表中")
                            }
                        }
                        "autoQuit", "自动退群"-> {
                            masterOnly(this)
                            var count = 0
                            for (group in groups) {
                                if ((group.id in WhiteListData.WhiteList[BotConfig.BotId]!!).not()) {
                                    group.sendMessage("【管理员操作自动退群】本群不在机器人白名单中，请联系机器人管理员申请白名单，或使用「${commandPrefix}apply white <群号> <原因>」指令发送白名单申请")
                                    group.quit()
                                    count++
                                }
                            }
                            sendQuoteReply("自动退出未知群聊 $count 个")
                        }
                        else-> {
                            sendQuoteReply("Group指令：未知的操作")
                        }
                    }
                }

                "reload", "重载"-> {   // 重载配置及数据
                    masterOnly(this)
                    try {
                        TiedanGame.rdConfig()
                        TiedanGame.rdData()
                        sendQuoteReply("配置及数据已重载")
                    } catch (e: Exception) {
                        logger.warning(e)
                        sendQuoteReply("出现错误：${e.message}")
                    }
                }

                "sendmail", "发送邮件"-> {
                    masterOnly(this)
                    val address: String = try {
                        args[1].content
                    } catch (_: Exception) {
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
                            val logs = File("bots/${bot?.id}/logs")
                            logs.listFiles()?.maxByOrNull { it.lastModified() }
                        }
                    }

                    val current = Thread.currentThread()
                    val oc = current.contextClassLoader
                    try {
                        current.contextClassLoader = MailConfig::class.java.classLoader
                        jakarta.mail.Transport.send(mail)
                        sendQuoteReply("邮件发送成功")
                    } catch (cause: jakarta.mail.MessagingException) {
                        sendQuoteReply("邮件发送失败, cause: ${cause.message}")
                    } finally {
                        current.contextClassLoader = oc
                    }
                }

                else-> {
                    sendQuoteReply("[操作无效] 请检查指令")
                }
            }
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[操作无效] 未知的参数")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

}
