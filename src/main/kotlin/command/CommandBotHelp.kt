package com.tiedan.command

import com.tiedan.TiedanGame
import com.tiedan.TiedanGame.logger
import com.tiedan.TiedanGame.sendQuoteReply
import com.tiedan.plugindata.BotInfoData
import net.mamoe.mirai.console.command.BuiltInCommands
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content

object CommandBotHelp : RawCommand(
    owner = TiedanGame,
    primaryName = "bot",
    secondaryNames = arrayOf("b"),
    description = "查看bot相关帮助",
    usage = "${commandPrefix}bot help"
){
    override suspend fun CommandContext.onCommand(args: MessageChain) {

        try {
            when (args[0].content) {

                "help"-> {   // 查看bot帮助（help）
                    val reply = " ·bot插件及功能帮助：\n" +
                            "${commandPrefix}bot info    查看bot数据📊\n" +
                            "${commandPrefix}bot status   mirai状态📶\n" +
                            "${commandPrefix}bot lgt    LGT相关帮助🤖\n" +
                            "${commandPrefix}bot cloud    词云帮助☁️\n" +
                            "${commandPrefix}bot fly    飞行棋帮助✈️\n" +
                            "${commandPrefix}bot grass   草图相关帮助🌿\n" +
                            "${commandPrefix}bot pet    表情相关帮助🐸\n" +
                            "${commandPrefix}bot jcc    在线编译器帮助💻\n" +
//                                "${commandPrefix}bot mcmod    MC百科查询帮助\n" +
//                                "${commandPrefix}抽卡    原神抽卡插件菜单\n" +
                            "\n" +
                            "📋 查看和添加pastebin代码\n" +
                            "${commandPrefix}pb help\n" +
                            "🖼️ 上传图片至图床\n" +
                            "${commandPrefix}upload help\n" +
                            "⏱️ 计时器指令帮助\n" +
                            "${commandPrefix}time help\n" +
                            "🎮 游戏积分相关指令\n" +
                            "${commandPrefix}point help\n" +
                            "📮 提交权限申请相关指令\n" +
                            "${commandPrefix}apply help\n" +
                            "\n" +
                            "如bot使用出现任何问题可直接在群内联系铁蛋"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "帮助"-> {   // 查看bot帮助（帮助）
                    val reply = " ·bot插件及功能帮助：\n" +
                            "${commandPrefix}b 信息    查看bot数据📊\n" +
                            "${commandPrefix}b 状态   mirai状态📶\n" +
                            "${commandPrefix}b LGT    LGT相关帮助🤖\n" +
                            "${commandPrefix}b 词云    词云帮助☁️\n" +
                            "${commandPrefix}b 飞行棋    飞行棋帮助✈️\n" +
                            "${commandPrefix}b 生草   草图相关帮助🌿\n" +
                            "${commandPrefix}b 表情    表情相关帮助🐸\n" +
                            "${commandPrefix}b 编译器    在线编译器帮助💻\n" +
//                            "${commandPrefix}b MC    MC百科查询帮助\n" +
//                            "${commandPrefix}抽卡    原神抽卡插件菜单\n" +
                            "\n" +
                            "📋 查看和添加pastebin代码\n" +
                            "${commandPrefix}代码 帮助\n" +
                            "🖼️ 上传图片至图床\n" +
                            "${commandPrefix}上传 帮助\n" +
                            "⏱️ 计时器指令帮助\n" +
                            "${commandPrefix}时间 帮助\n" +
                            "🎮 游戏积分相关指令\n" +
                            "${commandPrefix}积分 帮助\n" +
                            "📮 提交权限申请相关指令\n" +
                            "${commandPrefix}申请 帮助\n" +
                            "\n" +
                            "如bot使用出现任何问题可直接在群内联系铁蛋"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "info", "信息"-> {   // 查看bot信息
//                    val whiteEnable: String = if (BotConfig.WhiteList_enable) {"已启用"} else {"未启用"}
//                    val limit: String =
//                        if (BotInfoData.todayFriendImageNum < BotConfig.dailyLimit * 0.85) {
//                            "未达"
//                        } else if (BotInfoData.todayFriendImageNum < BotConfig.dailyLimit) {
//                            "*即将*"
//                        } else {
//                            "*已达*"
//                        }
                    val seconds = (System.currentTimeMillis() - BotInfoData.startTime) / 1000
                    val days = seconds / (24 * 3600)
                    val hours = (seconds % (24 * 3600)) / 3600
                    val minutes = (seconds % 3600) / 60
                    val reply = "运行时间：${days}天${hours}小时${minutes}分钟\n" +
//                                "Plugin version：v${TiedanGame.version}\n" +
//                                "白名单功能：$whiteEnable\n" +
                                "群聊数量：${sender.bot?.groups?.size}\n" +
                                "好友数量：${sender.bot?.friends?.size}\n" +
                                "  ·消息数据统计\n" +
                                "总计发送消息：${BotInfoData.totalMsgNum}\n" +
                                "总计发送图片：${BotInfoData.totalImageNum}\n" +
                                "昨日发送消息：${BotInfoData.yesterdayMsgNum}\n" +
                                "昨日发送图片：${BotInfoData.yesterdayImageNum}\n" +
                                "  ·今日数据统计\n" +
                                "今日发送消息：${BotInfoData.todayMsgNum}\n" +
                                "今日发送图片：${BotInfoData.todayImageNum}\n" +
                                "今日私信图片：${BotInfoData.todayPrivateImageNum}"
//                                "今日私信图片${limit}上限：\n" +
//                                "       ${BotInfoData.todayFriendImageNum} / ${BotConfig.dailyLimit}"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "status", "状态"-> {   // 查看mirai状态
                    BuiltInCommands.StatusCommand.runCatching {
                        sender.handle()
                    }
                }

                "lgt", "LGT"-> {   // LGT相关帮助
                    val reply = "LGTBot\n" +
                                "\n" +
                                "作者：森高（QQ：654867229）\n" +
                                "GitHub：https://github.com/slontia/lgtbot\n" +
                                "\n" +
                                "若您使用中遇到任何 BUG 或其它问题，欢迎私信作者，或前往 GitHub 主页提 issue\n" +
                                "本项目仅供娱乐和技术交流，请勿用于商业用途，健康游戏，拒绝赌博\n" +
                                "\n" +
                                "·发送信息：\n" +
                                "<@此bot> #帮助\n" +
                                "·来查看LGTBot相关帮助"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "cloud", "词云"-> {   // 词云帮助
                    val reply = "☁\uFE0F 词云指令列表：\n" +
                                "·本日词云\n" +
                                "·昨日词云\n" +
                                "·本月词云\n" +
                                "·获取词云 <from> <to>\n" +
                                "请注意：词云功能比较消耗性能，请尽量在没有游戏房间运行时使用"
//                                "用户本日词云 + <用户名>\n" +
//                                "用户昨日词云 + <用户名>\n" +
//                                "用户本月词云 + <用户名>\n" +
//                                "获取用户词云 + <用户名> <from> <to>"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "fly", "飞行棋"-> {   // 飞行棋帮助
                    val reply = "✈\uFE0F飞行棋插件相关帮助：\n" +
                                "·创建游戏指令：\n" +
                                "    创建飞行棋\n" +
                                "·加入游戏指令：\n" +
                                "    加入飞行棋\n" +
                                "·开始游戏指令\n" +
                                "    开始飞行棋\n" +
                                "·移动棋子指令：\n" +
                                "    /1  /2  /3  /4\n" +
                                " （数字前加“/”）"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "grass", "生草", "草图"-> {   // 草图相关帮助
                    val reply = "\uD83C\uDF3F 草图插件相关帮助：\n" +
                                "·获取草图指令：\n" +
                                "    生草\n" +
                                "·查看插件数据：\n" +
                                "    草图信息\n" +
                                "·草图官方网站：\n" +
                                "https://grass.nlrdev.top"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "pet", "表情"-> {   // 表情包相关帮助
                    val reply = "\uD83D\uDC38 表情包生成帮助：\n" +
                                "·触发格式：\n" +
                                "#关键字 + @对象/QQ昵称/QQ号\n" +
                                "#关键字 + <回复消息/发送图片>\n" +
                                "\n" +
                                "·部分可用关键字：\n" +
                                "摸、贴、打、抱、锤/捶/敲、踩、踹/踢\n" +
                                "·以上关键字后加“爆”可加速\n" +
                                "\n" +
                                "·更多关键字请发送“pet”来获取完整列表\n" +
                                "注：pet列表内的关键字需要加空格才能使用（中英文皆可）\n" +
                                "戳一戳有10%概率生成随机表情\n" +
                                "\n" +
                                "·群主或管理员可以使用“pet on/off”开启和关闭此表情包功能（包括戳一戳随机表情）"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                "jcc", "编译器"-> {   // jcc在线编译器相关帮助
                   val reply = "·\uD83D\uDCBB 在线运行代码指令:\n" +
                                "run <language> <code>\n" +
                                "run <language> <pastebinUrl> [stdin]\n" +
                                "引用消息: run <language> [stdin]\n" +
                                "·仓库地址：https://github.com/tiedanGH/mirai-console-jcc-plugin\n" +
                                "·其它指令：\n" +
                                "${commandPrefix}jcc help    查看jcc帮助\n" +
                                "${commandPrefix}jcc list    列出所有支持的编程语言\n" +
                                "${commandPrefix}jcc template <language>    获取指定语言的模板"
                   sendQuoteReply(sender, originalMessage, reply)
                }

                "mcmod", "MC", "mc"-> {   // MC百科查询帮助
                    val reply = "·Minecraft百科查询插件使用说明:\n" +
                                "请直接输入 关键字 + 内容 进行查询\n" +
                                "回复序号查询详细内容（每页从0-10编号）\n" +
                                "查询物品:物品 <物品关键词>\n" +
                                "查询模组:模组 <模组关键词>\n" +
                                "查询教程:教程 <教程关键词>\n" +
                                "查询整合包:整合包 <整合包关键词>\n" +
                                "查询服务器:服务器 <服务器关键词>\n" +
                                "·资料均来自:mcmod.cn"
                    sendQuoteReply(sender, originalMessage, reply)
                }

                else-> {
                    sendQuoteReply(sender, originalMessage, "[参数不匹配]\n请使用「${commandPrefix}bot help」来查看指令帮助")
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            sendQuoteReply(sender, originalMessage, "[参数不足]\n请使用「${commandPrefix}bot help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply(sender, originalMessage, "[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

}