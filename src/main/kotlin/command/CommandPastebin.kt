package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.save
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.plugindata.PastebinData
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.warning

object CommandPastebin : RawCommand(
    owner = TiedanGame,
    primaryName = "pastebin",
    secondaryNames = arrayOf("pb", "代码"),
    description = "查看和添加pastebin代码"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        val commands : MutableList<SingleMessage> = mutableListOf()
        for (element in args) {
            commands.add(element)
        }

        try {
            when (commands[0].content) {

                "help", "帮助"-> {   // 查看pastebin帮助
                    val reply = "·pastebin查看相关帮助：\n" +
                                "-> 查看完整列表\n" +
                                "#pastebin list\n" +
                                "-> 查看代码运行示例\n" +
                                "#pastebin run <名称>\n" +
                                "-> 查看代码具体参数\n" +
                                "#pastebin info <名称>\n" +
                                "\n" +
                                "·pastebin添加修改帮助：\n" +
                                "-> 添加pastebin数据\n" +
                                "#pastebin add <名称> <语言> <pastebinUrl> <示例输入(stdin)>\n" +
                                "-> 修改数据中某一项的参数\n" +
                                "#pastebin set <名称> <参数名> <内容>\n" +
                                "（指令可以简写成「#pb」）"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "list", "列表"-> {   // 查看完整列表
                    var pastebinList = "pastebin列表：\n"
                    for (key in PastebinData.pastebin.keys) {
                        pastebinList += "$key ${PastebinData.pastebin[key]?.get("language")}\n"
                    }
                    sendQuoteReply(sender, originalMessage, pastebinList)
                }

                "run", "示例", "运行"-> {   // 查看代码运行示例
                    val name = commands[1].content
                    if (PastebinData.pastebin.containsKey(name).not()) {
                        sendQuoteReply(sender, originalMessage, "未知的名称：$name\n请使用「#pastebin list」来查看完整列表")
                        return
                    }
                    sender.sendMessage(
                        "run ${PastebinData.pastebin[name]?.get("language")} " +
                        "${PastebinData.pastebin[name]?.get("pastebinUrl")} " +
                        "${PastebinData.pastebin[name]?.get("stdin")}"
                    )
                }

                "info", "信息"-> {   // 查看数据具体参数
                    val name = commands[1].content
                    if (PastebinData.pastebin.containsKey(name).not()) {
                        sendQuoteReply(sender, originalMessage, "未知的名称：$name\n请使用「#pastebin list」来查看完整列表")
                        return
                    }
                    sendQuoteReply(sender, originalMessage,
                        "名称：$name\n" +
                                "语言：${PastebinData.pastebin[name]?.get("language")}\n" +
                                "pastebinUrl：\n" +
                                "${PastebinData.pastebin[name]?.get("pastebinUrl")}\n" +
                                "示例输入：${PastebinData.pastebin[name]?.get("stdin")}"
                    )
                }

                "add", "添加", "新增"-> {   // 添加pastebin数据
                    val name = commands[1].content
                    if (PastebinData.pastebin.containsKey(name)) {
                        sendQuoteReply(sender, originalMessage, "添加失败，名称 $name 已存在")
                        return
                    }
                    val language = commands[2].content
                    val pastebinUrl = commands[3].content
                    var stdin = ""
                    args.forEachIndexed { index, item ->
                        if (index == 4) { stdin += item.content }
                        if (index > 4) { stdin += " ${item.content}" }
                    }
                    PastebinData.pastebin[name] =
                        mutableMapOf("language" to language, "pastebinUrl" to pastebinUrl, "stdin" to stdin)
                    PastebinData.save()
                    sendQuoteReply(sender, originalMessage,
                        "添加pastebin成功！\n" +
                                "名称：$name\n" +
                                "语言：$language\n" +
                                "pastebinUrl：\n" +
                                "${pastebinUrl}\n" +
                                "示例输入：${stdin}"
                    )
                }

                "set", "修改", "设置"-> {   // 修改数据中某一项的参数
                    val name = commands[1].content
                    val option = commands[2].content
                    var content = ""
                    args.forEachIndexed { index, item ->
                        if (index == 3) { content += item.content }
                        if (index > 3) {
                            if (option == "name") {
                                sendQuoteReply(sender, originalMessage, "无法修改，因为名称不能包含空格！")
                                return
                            }
                            content += " ${item.content}"
                        }
                    }
                    if (content.isEmpty()) {
                        sendQuoteReply(sender, originalMessage, "无法修改，因为输入的值为空！")
                        return
                    }
                    if (PastebinData.pastebin.containsKey(name).not()) {
                        sendQuoteReply(sender, originalMessage, "未知的名称：$name\n请使用「#pastebin list」来查看完整列表")
                        return
                    }
                    if (listOf("name", "language", "pastebinUrl", "stdin").contains(option).not()) {
                        sendQuoteReply(sender, originalMessage,
                            "未知的配置项：$option\n仅支持配置\"name\",\"language\",\"pastebinUrl\",\"stdin\"")
                        return
                    }
                    if (option == "name") {
                        PastebinData.pastebin[content] = PastebinData.pastebin[name]!!
                        PastebinData.pastebin.remove(name)
                    } else {
                        PastebinData.pastebin[name]?.set(option, content)
                    }
                    sendQuoteReply(sender, originalMessage, "成功将 $name 的 $option 参数修改为 $content")
                    PastebinData.save()
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「#pastebin help」来查看指令帮助")
                }
            }
        } catch (ex: Exception) {
            logger.warning {"error: ${ex.message}"}
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「#pastebin help」来查看指令帮助")
        }
    }
}