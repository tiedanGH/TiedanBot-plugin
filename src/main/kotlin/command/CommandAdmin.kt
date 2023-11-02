package com.tiedan.command

import com.tiedan.Config
import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.warning
import java.util.concurrent.TimeUnit

object CommandAdmin : RawCommand(
    owner = TiedanGame,
    primaryName = "admin",
    description = "管理员相关指令"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        if (!Config.AdminList.contains(sender.user?.id) && sender.user?.id != Config.master && sender.isNotConsole()) {
            sendQuoteReply(sender, originalMessage, "未持有管理员权限")
            return
        }

        val commands : MutableList<SingleMessage> = mutableListOf()
        for (element in args) {
            commands.add(element)
        }

        try {
            when (commands[0].content) {

                "help"-> {   // 查看admin可用帮助
                    val reply = "·admin可用帮助：\n" +
                                "->查看管理员列表\n" +
                                "#admin list\n" +
                                "->查看白名单列表\n" +
                                "#admin WhiteList [info]\n" +
                                "->设置白名单开关状态\n" +
                                "#admin setWhiteList <enable/disable>\n" +
                                "->添加白名单\n" +
                                "#admin addWhiteList <group> [desc]\n" +
                                "->移除白名单\n" +
                                "#admin delWhiteList [group]\n" +
                                "->重载配置及数据\n" +
                                "#admin reload\n"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "list"-> {   // 查看管理员列表
                    var adminListInfo = "·管理员列表：\n"
                    for (admin in Config.AdminList) {
                        adminListInfo = adminListInfo + admin + "\n"
                    }
                    sendQuoteReply(sender, originalMessage, adminListInfo)
                }

                "op"-> {   // 添加管理员
                    masterOnly(sender)
                    try {
                        val qq = commands[1].content.toLong()
                        val result = Config.AdminList.add(qq)
                        if (result) {
                            Config.AdminList.sort()
                            Config.AdminList = Config.AdminList.distinct().toMutableList()
                            Config.save()
                            sendQuoteReply(sender, originalMessage, "已将 $qq 设为管理员")
                        } else {
                            sendQuoteReply(sender, originalMessage, "[未知原因] 添加失败")
                        }
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "deop"-> {   // 移除管理员
                    masterOnly(sender)
                    try {
                        val qq = commands[1].content.toLong()
                        val result = Config.AdminList.remove(qq)
                        if (result) {
                            Config.save()
                            sendQuoteReply(sender, originalMessage, "已将 $qq 移除管理员")
                        } else {
                            sendQuoteReply(sender, originalMessage, "不存在管理员 $qq")
                        }
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "shutdown"-> {   // 关机指令
                    masterOnly(sender)
                    sendQuoteReply(sender, originalMessage, "机器人正在关机······")
                    withContext(Dispatchers.IO) {
                        TimeUnit.SECONDS.sleep(1)
                    }
                    BuiltInCommands.StopCommand.run {
                        sender.handle()
                    }
                }

                "transfer"-> {   // bot积分转账
                    masterOnly(sender)
                    val qq = commands[1].content
                    val point = commands[2].content
                    sender.sendMessage("/pt transfer $qq $point")
                }

                "send"-> {   // bot消息发送
                    masterOnly(sender)
                    var messages: MessageChain = messageChainOf()
                    for (element in args) {
                        if (element.content != "send") {
                            if (messages.isNotEmpty()) {
                                messages = messageChainOf(messages, PlainText(" "))
                            }
                            messages = messageChainOf(messages, element)
                        }
                    }
                    if (messages.isNotEmpty()) {
                        sender.sendMessage(messages)
                    } else {
                        sender.sendMessage(" ")
                    }
                }

                "WhiteList"-> {   // 查看白名单列表
                    val showDesc = try {
                        commands[1].content == "info"
                    } catch (ex: Exception) {
                        false
                    }
                    val whiteEnable: String = if (Config.WhiteList_enable) { "已启用" } else { "未启用" }
                    var whiteListInfo = "白名单功能：$whiteEnable\n·白名单列表：\n"
                    for (key in Config.WhiteList.keys) {
                        whiteListInfo += key
                        if (showDesc) {
                            whiteListInfo = whiteListInfo + " " + Config.WhiteList[key]
                        }
                        whiteListInfo += "\n"
                    }
                    sendQuoteReply(sender, originalMessage, whiteListInfo)
                }

                "setWhiteList"-> {   // 设置白名单功能状态
                    val option = commands[1].content
                    if (option == "enable" && !Config.WhiteList_enable) {
                        Config.WhiteList_enable = true
                        Config.save()
                        sendQuoteReply(sender, originalMessage, "已启用bot白名单功能")
                    } else if (option == "disable" && Config.WhiteList_enable) {
                        Config.WhiteList_enable = false
                        Config.save()
                        sendQuoteReply(sender, originalMessage, "已关闭bot白名单功能")
                    } else {
                        sendQuoteReply(sender, originalMessage, "指令或状态错误！\n当前白名单状态：${Config.WhiteList_enable}")
                    }
                }

                "addWhiteList"-> {   // 添加白名单
                    try {
                        val group = commands[1].content.toLong()
                        val desc = try {
                            commands[2].content
                        } catch (ex: Exception) {
                            logger.warning {"error: ${ex.message}"}
                            "no_desc"
                        }
                        val result = Config.WhiteList.put(group, desc)
                        if (result == null) {
                            Config.WhiteList = Config.WhiteList.toSortedMap()
                            sendQuoteReply(sender, originalMessage, "已将 $group 添加进白名单列表")
                        } else {
                            sendQuoteReply(sender, originalMessage, "$group 已存在，更新描述成功：$desc")
                        }
                        Config.save()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                    }
                }

                "delWhiteList"-> {   // 移除白名单
                    val group: Long? = try {
                        commands[1].content.toLong()
                    } catch (ex: NumberFormatException) {
                        sendQuoteReply(sender, originalMessage, "数字转换错误，请检查指令")
                        return
                    } catch (ex: Exception) {
                        sender.subject?.id
                    }
                    val result = Config.WhiteList.remove(group)
                    if (result != null) {
                        Config.save()
                        sendQuoteReply(sender, originalMessage, "已将 $group 移除白名单列表")
                    } else {
                        sendQuoteReply(sender, originalMessage, "白名单列表不存在群聊 $group")
                    }
                }

                "focus"-> {   // 专注模式
                    val option = commands[1].content
                    if (option == "disable") {
                        Config.focus_enable = false
                        Config.focus_to = 0
                        Config.save()
                        sendQuoteReply(sender, originalMessage,
                            "***专注模式 [已关闭]***\n已清除专注模式配置")
                    } else {
                        try {
                            Config.focus_to = option.toLong()
                            Config.save()
                            sendQuoteReply(sender, originalMessage,
                                "***专注模式 [已启用]***\nbot将专注于群聊 ${Config.focus_to} 进行服务")
                        } catch (ex: NumberFormatException) {
                            sendQuoteReply(sender, originalMessage, "参数转换错误，请检查指令")
                        }
                    }
                }

                "reload"-> {   // 重载配置及数据
                    try {
                        TiedanGame.rdConfig()
                        TiedanGame.rdData()
                        sendQuoteReply(sender, originalMessage, "配置及数据已重载")
                    } catch (ex: Exception) {
                        logger.warning(ex)
                        sendQuoteReply(sender, originalMessage, "出现错误：${ex.message}")
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
        if (sender.user?.id != Config.master && sender.isNotConsole()) {
            throw PermissionDeniedException("Master Only")
        }
    }
}