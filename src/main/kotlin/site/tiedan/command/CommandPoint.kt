package site.tiedan.command

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.command.isConsole
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.getNickname
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.masterOnly
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.BotConfig
import site.tiedan.plugindata.PointData

object CommandPoint : RawCommand(
    owner = TiedanGame,
    primaryName = "point",
    secondaryNames = arrayOf("pt", "积分"),
    description = "铁蛋积分指令",
    usage = "${commandPrefix}pt help"
) {
    private val commandList = listOf(
        Command("pt balance [QQ号]", "积分 余额 [QQ号]", "查询游戏积分", 1),
        Command("pt exchange <数额>", "积分 提款 <数额>", "提款至虞姬积分", 1),
        Command("pt rank [页码]", "积分 排行 [页码]", "查看排行和数据", 1),
        Command("pt transfer <QQ号/@目标> <数额>", "积分 转账 <QQ号/@目标> <数额>", "向指定目标转账", 1),

        Command("pt ExchangeFunction <on/off>", "积分 提款功能 <开启/关闭>", "配置提款功能状态", 2),
        Command("pt TransferFunction <on/off>", "积分 转账功能 <开启/关闭>", "配置转账功能状态", 2),
        Command("pt add <QQ> <数额>", "积分 添加 <QQ> <数额>", "为指定账户增加积分", 2),
    )

    override suspend fun CommandSender.onCommand(args: MessageChain) {

        val qq = user?.id ?: 10000
        if (PointData.PointData.contains(qq).not()) {
            PointData.PointData[qq] = 0
            PointData.PointData = PointData.PointData.toSortedMap()
            PointData.save()
        }

        try {
            when (args[0].content) {

                "help" -> {   // 查看point可用帮助（help）
                    var reply = " ·游戏积分指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${commandPrefix}${it.usage}　${it.desc}\n" } +
                            "\n" +
                            "【请注意】在提款前请务必确保虞姬在线！实际到账积分会受到虞姬月卡等级和每日积分获取限制等影响，单日大量提款会亏损积分，建议提款额为3000以内\n" +
                            "【获取来源】大海战BOSS战、开放蜂巢、漫漫长夜、面包危机、爆金币\n"
                    if (args.getOrNull(1)?.content == "all" && (user?.id == BotConfig.master || isConsole())) {
                        reply += "\n ·master管理指令：\n" +
                                commandList.filter { it.type == 2 }.joinToString("") { "${commandPrefix}${it.usage}　${it.desc}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "帮助" -> {   // 查看point可用帮助（帮助）
                    var reply = " ·游戏积分指令帮助：\n" +
                            commandList.filter { it.type == 1 }.joinToString("") { "${commandPrefix}${it.usageCN}　${it.desc}\n" } +
                            "\n" +
                            "【请注意】在提款前请务必确保虞姬在线！实际到账积分会受到虞姬月卡等级和每日积分获取限制等影响，单日大量提款会亏损积分，建议提款额为3000以内\n" +
                            "【获取来源】大海战BOSS战、开放蜂巢、漫漫长夜、面包危机、爆金币\n"
                    if (args.getOrNull(1)?.content == "all" && (user?.id == BotConfig.master || isConsole())) {
                        reply += "\n ·master管理指令：\n" +
                                commandList.filter { it.type == 2 }.joinToString("") { "${commandPrefix}${it.usageCN}　${it.desc}\n" }
                    }
                    sendQuoteReply(reply)
                }

                "balance", "余额"-> {   // 查询游戏积分
                    val id = args.getOrNull(1)?.content?.replace("@", "")?.toLong()
                    if (id == null) {
                        if (PointData.PointData[qq] == 0L) {
                            sendQuoteReply("您没有待提取的游戏积分")
                        } else {
                            sendQuoteReply("您的游戏积分余额为：${PointData.PointData[qq]}")
                        }
                    } else {
                        val name = getNickname(this, id)
                        if (PointData.PointData.contains(id).not()) {
                            sendQuoteReply("${name}没有游戏积分账户")
                        } else {
                            sendQuoteReply("${name}的游戏积分余额为：${PointData.PointData[id]}")
                        }
                    }
                }

                "exchange", "提款"-> {   // 提款至虞姬积分
                    if (PointData.ExchangeFunction.not()) {
                        sendQuoteReply("提款功能被临时关闭")
                        return
                    }
                    if (subject !is Group) {
                        sendQuoteReply("此指令只能在群聊中执行，在提款前请务必确保虞姬在线！")
                        return
                    }
                    val point = args[1].content.toLong()
                    val balance = PointData.PointData[qq]!!
                    if (balance == 0L) {
                        sendQuoteReply("您没有待提取的游戏积分")
                        return
                    }
                    if (point < 0L) {
                        sendQuoteReply("抢劫是犯法的，罚你云巢癞子后完美块、每回合打top、大海战被BOSS锁头")
                        return
                    }
                    if (point == 0L) {
                        sendQuoteReply("成功将空气从保险柜取出")
                        return
                    }
                    if (point > balance) {
                        sendQuoteReply("[提款失败] 游戏积分不足，您的余额为：${balance}")
                        return
                    }
                    if (point > 10000L) {
                        sendQuoteReply("[提款失败] 单次提款不能超过10000积分\n【注意】提款积分计算在虞姬每日积分总收益中，单日建议提款额为3000以内")
                        return
                    }
                    sendMessage("/pt tiedan $qq $point")
                    savePointChange(qq, -point)
                    sendQuoteReply("游戏积分扣除：$point\n【注意】实际到账积分会受到虞姬月卡等级和每日积分获取限制等影响，单日建议提款额为3000以内")
                }

                "transfer", "转账"-> {   // 向指定目标转账
                    if (PointData.TransferFunction.not()) {
                        sendQuoteReply("转账功能未启用")
                        return
                    }
                    val id = args[1].content.replace("@", "").toLong()
                    val point = args[2].content.toLong()
                    val balance = PointData.PointData[qq]!!
                    val name = getNickname(this, id)
                    if (balance == 0L) {
                        sendQuoteReply("您没有待提取的游戏积分")
                        return
                    }
                    if (PointData.PointData.contains(id).not()) {
                        sendQuoteReply("目标账户不存在，建议提款后使用虞姬操作转账")
                        return
                    }
                    if (qq == id) {
                        sendQuoteReply("不能给自己转账")
                        return
                    }
                    if (point < 0L) {
                        sendQuoteReply("抢劫是犯法的，罚你云巢死后完美块、选秀末位312、大海战BOSS2核弹开局爆炸")
                        return
                    }
                    if (point == 0L) {
                        sendQuoteReply("空气运输中...运输失败")
                        return
                    }
                    if (point > balance) {
                        sendQuoteReply("游戏积分不足，您的余额为：${balance}")
                        return
                    }
                    savePointChange(qq, -point)
                    savePointChange(id, point)
                    sendQuoteReply("成功向 $name 转账 $point 积分")
                }

                "rank", "排行"-> {   // 查看积分排行榜
                    val page = try {
                        args.getOrElse(1) { 1 }.toString().toInt()
                    } catch (e: NumberFormatException) { 1 }
                    val sortedPoint = PointData.PointData .filter { it.value > 0 }.toList().sortedByDescending { it.second }
                    val totalPoints = sortedPoint.sumOf { it.second }
                    val totalUsers = sortedPoint.size
                    val totalPages = (totalUsers + 9) / 10
                    val startIndex = (page - 1) * 10
                    val pageData = sortedPoint.drop(startIndex).take(10)

                    val output = StringBuilder()
                    output.append("·持有用户数：$totalUsers\n")
                    output.append("·待提取总积分：$totalPoints\n")
                    if (pageData.isNotEmpty()) {
                        pageData.forEachIndexed { index, (key, value) ->
                            output.append("${startIndex + index + 1}.${getNickname(this, key)}($key) - $value\n")
                        }
                        if (startIndex + 9 < totalUsers) {
                            output.append("...\n")
                        }
                    } else {
                        output.append("[该页没有数据] 有效页码为 1-$totalPages\n")
                    }
                    val userPoint = sortedPoint.find { it.first == qq }?.second
                    if (userPoint != null) {
                        output.append("你的游戏积分：$userPoint")
                    }
                    sendQuoteReply(output.toString())
                }

                // master操作
                "ExchangeFunction", "提款功能"-> {   // 配置提款功能状态
                    masterOnly(this)
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    when {
                        enable.contains(args[1].content) -> {
                            PointData.ExchangeFunction = true
                            sendQuoteReply("已启用提款功能")
                        }
                        disable.contains(args[1].content) -> {
                            PointData.ExchangeFunction = false
                            sendQuoteReply("已关闭提款功能")
                        }
                    }
                    PointData.save()
                }

                "TransferFunction", "转账功能"-> {   // 配置转账功能状态
                    masterOnly(this)
                    val enable: List<String> = arrayListOf("enable","on","true","开启")
                    val disable: List<String> = arrayListOf("disable","off","false","关闭")
                    when {
                        enable.contains(args[1].content) -> {
                            PointData.TransferFunction = true
                            sendQuoteReply("已启用转账功能")
                        }
                        disable.contains(args[1].content) -> {
                            PointData.TransferFunction = false
                            sendQuoteReply("已关闭转账功能")
                        }
                    }
                    PointData.save()
                }

                "add", "添加"-> {   // 为指定账户增加积分
                    masterOnly(this)
                    val id = args[1].content.replace("@", "").toLong()
                    val point = args[2].content.toLong()
                    val force = args.getOrNull(3)?.content?.let { it == "force" || it == "f" } ?: false
                    val name = getNickname(this, id)
                    if (PointData.PointData.contains(id).not() && name == "[获取昵称失败]" && !force) {
                        sendQuoteReply("[警告] 无法获取目标用户信息，请再次确认是否输入正确，或添加参数强制添加")
                        return
                    }
                    savePointChange(id, point)
                    if (point >= 0) {
                        sendQuoteReply("为${name}添加 $point 游戏积分")
                    } else {
                        sendQuoteReply("为${name}扣除 ${-point} 游戏积分")
                    }
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${commandPrefix}pt help」来查看指令帮助")
                }
            }
        } catch (e: NumberFormatException) {
            sendQuoteReply("[操作失败] 数值超出或无法转换为Long类型")
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}pt help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    fun savePointChange(qq: Long, point: Long) {
        if (PointData.PointData.contains(qq).not()) {
            PointData.PointData[qq] = 0L
        }
        PointData.PointData[qq] = PointData.PointData[qq]!! + point
        PointData.save()
    }
}
