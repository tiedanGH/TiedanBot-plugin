package site.tiedan.command

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.commandPrefix
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.message.data.*
import site.tiedan.TiedanGame
import site.tiedan.TiedanGame.Command
import site.tiedan.TiedanGame.adminOnly
import site.tiedan.TiedanGame.getNickname
import site.tiedan.TiedanGame.isAdmin
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.save
import site.tiedan.TiedanGame.sendQuoteReply
import site.tiedan.config.FunctionConfig
import site.tiedan.data.NotifyData
import site.tiedan.data.NotifyPlayer
import java.time.LocalDate
import java.time.LocalTime

object CommandNotify : RawCommand(
    owner = TiedanGame,
    primaryName = "notify",
    secondaryNames = arrayOf("n", "通知"),
    description = "开游戏通知指令",
    usage = "${commandPrefix}n help"
){
    private val commandList = listOf(
        Command("n call <标签>", "通知 召集 <标签>", "📢 召集标签下的玩家（每日${MAX_DAILY_CALL}次）", 2),

        Command("n list [关键字]", "通知 标签 [关键字]", "🏷️ 查看全可用标签", 1),
        Command("n mine", "通知 我的", "👤 查看自己的登记信息", 1),
        Command("n add/del <标签...>", "通知 添加/删除 <标签...>", "✏ 登记/删除标签（可批量）", 1),
        Command("n on/off", "通知 开启/关闭", "🔔 个人通知开关", 1),
        Command("n time <时间段>", "通知 时间 <时间段>", "⏰ 设置接收通知时间段\n（如8-16、16-2）", 1),
        Command("n tag <标签>", "通知 查看 <标签>", "🔍 查看标签信息", 1),
        Command("n stats", "通知 统计", "📊 查看功能统计数据", 1),

        Command("n addtag <标签...>", "通知 添加标签 <标签...>", "🏷️ 添加标签至标签库", 3),
        Command("n deltag <标签...>", "通知 删除标签 <标签...>", "🗑️ 从标签库删除标签", 3),
        Command("n user <QQ>", "通知 查询 <QQ>", "🔍 查询指定用户信息", 3),
    )

    private const val MAX_DAILY_CALL = 10
    private const val MAX_AT_COUNT = 20
    private val timeRangeRegex = Regex("^(\\d{1,2})-(\\d{1,2})$")

    override suspend fun CommandSender.onCommand(args: MessageChain) {

        if (!FunctionConfig.notify_enable) {
            sendQuoteReply("游戏通知功能未启用")
            return
        }

        val qq = user?.id ?: 10000
        val hasAdminPermission = isAdmin(this)

        try {
            when (args[0].content) {

                "help"-> {   // 查看notify可用帮助（help）
                    val reply = buildString {
                        append(" ·🎲 开游戏通知指令帮助：\n")
                        commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        append("\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        if (hasAdminPermission) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usage}\n") }
                        }
                    }
                    sendQuoteReply(reply)
                }

                "帮助"-> {   // 查看notify可用帮助（帮助）
                    val reply = buildString {
                        append(" ·🎲 开游戏通知指令帮助：\n")
                        commandList.filter { it.type == 2 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        append("\n")
                        commandList.filter { it.type == 1 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        if (hasAdminPermission) {
                            append("\n ·🛠️ admin管理指令：\n")
                            commandList.filter { it.type == 3 }.forEach { append("${it.desc}\n${commandPrefix}${it.usageCN}\n") }
                        }
                    }
                    sendQuoteReply(reply)
                }

                "list", "标签"-> {   // 查看全部可用标签（按登记玩家数量排序，支持关键字搜索）
                    if (NotifyData.tags.isEmpty()) {
                        sendQuoteReply("标签库中暂无标签")
                        return
                    }
                    val sortedTags = NotifyData.tags
                        .map { tag -> tag to NotifyData.players.values.count { it.tags.contains(tag) } }
                        .sortedByDescending { it.second }
                    val keyword = args.getOrNull(1)?.content?.trim()
                    if (keyword.isNullOrEmpty()) {
                        sendQuoteReply("·可用标签（${sortedTags.size}个）：\n${formatTagLines(sortedTags)}")
                    } else {
                        val matched = sortedTags.filter { it.first.contains(keyword, ignoreCase = true) }
                        if (matched.isEmpty()) {
                            sendQuoteReply("未找到包含「$keyword」的标签")
                        } else {
                            sendQuoteReply("·搜索标签结果（${matched.size}个）：\n${formatTagLines(matched)}")
                        }
                    }
                }

                "mine", "我的"-> {   // 查看自己的登记信息
                    val player = NotifyData.players[qq]
                    if (player == null) {
                        sendQuoteReply("您还没有任何登记信息，请使用「${commandPrefix}n add <标签...>」登记标签")
                        return
                    }
                    resetDailyCount()
                    sendQuoteReply(buildPlayerInfo("您", player))
                }

                "add", "添加"-> {   // 登记标签（可批量）
                    val inputTags = parseTags(args)
                    if (inputTags.isEmpty()) {
                        sendQuoteReply("[参数不足] 请提供至少一个标签，多个标签用空格分隔")
                        return
                    }
                    val player = NotifyData.players.getOrPut(qq) { NotifyPlayer() }
                    val added = mutableListOf<String>()
                    val exist = mutableListOf<String>()
                    val invalid = mutableListOf<String>()
                    for (tag in inputTags) {
                        when {
                            tag !in NotifyData.tags -> invalid.add(tag)
                            !player.tags.add(tag) -> exist.add(tag)
                            else -> added.add(tag)
                        }
                    }
                    NotifyData.save()
                    val reply = buildString {
                        append("·标签登记结果：")
                        if (added.isNotEmpty()) append("\n✅ 登记成功：${added.joinToString(" ")}")
                        if (exist.isNotEmpty()) append("\n⚠️ 已登记过：${exist.joinToString(" ")}")
                        if (invalid.isNotEmpty()) {
                            append("\n❌ 无效标签：${invalid.joinToString(" ")}")
                            append(fuzzyMatchLine(invalid))
                            append("\n请使用「${commandPrefix}n list」查看可用标签")
                        }
                    }
                    sendQuoteReply(reply)
                }

                "del", "删除"-> {   // 删除登记标签（可批量）
                    val inputTags = parseTags(args)
                    if (inputTags.isEmpty()) {
                        sendQuoteReply("[参数不足] 请提供至少一个标签，多个标签用空格分隔")
                        return
                    }
                    val player = NotifyData.players[qq]
                    val removed = mutableListOf<String>()
                    val notFound = mutableListOf<String>()
                    for (tag in inputTags) {
                        if (player?.tags?.remove(tag) == true) removed.add(tag) else notFound.add(tag)
                    }
                    NotifyData.save()
                    val reply = buildString {
                        append("·标签删除结果：")
                        if (removed.isNotEmpty()) append("\n✅ 删除成功：${removed.joinToString(" ")}")
                        if (notFound.isNotEmpty()) append("\n⚠️ 未登记过：${notFound.joinToString(" ")}")
                    }
                    sendQuoteReply(reply)
                }

                "on", "开启"-> {   // 开启个人通知
                    NotifyData.players.getOrPut(qq) { NotifyPlayer() }.enable = true
                    NotifyData.save()
                    sendQuoteReply("已开启个人游戏通知")
                }

                "off", "关闭"-> {   // 关闭个人通知
                    NotifyData.players.getOrPut(qq) { NotifyPlayer() }.enable = false
                    NotifyData.save()
                    sendQuoteReply("已关闭个人游戏通知，您将不会被检索和@")
                }

                "time", "时间"-> {   // 设置接收通知时间段
                    val range = args[1].content
                    val match = timeRangeRegex.find(range)
                    if (match == null) {
                        sendQuoteReply("[格式错误] 时间段格式为 <开始小时>-<结束小时>（0-23的整数），例如 8-16 或跨天的 16-2")
                        return
                    }
                    val start = match.groupValues[1].toInt()
                    val end = match.groupValues[2].toInt()
                    if (start > 23 || end > 23) {
                        sendQuoteReply("[范围错误] 小时数仅支持 0-23")
                        return
                    }
                    NotifyData.players.getOrPut(qq) { NotifyPlayer() }.time = "$start-$end"
                    NotifyData.save()
                    sendQuoteReply("已设置接收通知时间段：$start-$end" + if (start > end) "（跨天）" else "")
                }

                "tag", "查看"-> {   // 查看标签信息与登记玩家
                    val tag = args[1].content
                    if (tag !in NotifyData.tags) {
                        sendQuoteReply("标签「$tag」不存在${fuzzyMatchLine(listOf(tag))}\n请使用「${commandPrefix}n list」查看可用标签")
                        return
                    }
                    val registered = NotifyData.players.filter { (_, player) -> player.tags.contains(tag) }.keys
                    val group = subject as? Group
                    val reply = buildString {
                        append("·标签「$tag」信息：\n")
                        if (group != null) {
                            val members = registered.filter { group.getMember(it) != null }
                            append("登记人数：${registered.size}（本群${members.size}人）\n")
                            append("累计召集次数：${NotifyData.tagCallCount[tag] ?: 0}\n")
                            append("本群登记玩家：\n")
                            append(members.joinToString("、") { getNickname(this@onCommand, it) }.ifEmpty { "无" })
                        } else {
                            append("登记人数：${registered.size}\n")
                            append("累计召集次数：${NotifyData.tagCallCount[tag] ?: 0}\n")
                            append("（玩家名单预览仅群聊中可见）")
                        }
                    }
                    sendQuoteReply(reply)
                }

                "stats", "统计"-> {   // 查看功能统计数据
                    val players = NotifyData.players.values
                    val totalTagRecords = players.sumOf { it.tags.size }
                    val registeredTagCount = NotifyData.tags.count { tag -> players.any { it.tags.contains(tag) } }
                    val avg = if (players.isEmpty()) 0.0 else totalTagRecords.toDouble() / players.size
                    val ranking = NotifyData.tagCallCount.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(10)
                    val reply = buildString {
                        append("·📊 游戏召集统计数据：\n")
                        append("总召集次数：${NotifyData.totalCallCount}\n")
                        append("玩家数量：${players.size}\n")
                        append("启用人数：${players.count { it.enable }}\n")
                        append("总标签数：${NotifyData.tags.size}\n")
                        append("被登记标签数：$registeredTagCount\n")
                        append("登记总人次：$totalTagRecords\n")
                        append("人均登记标签：${"%.1f".format(avg)}\n")
                        append("·🔝 标签召集次数排行：")
                        if (ranking.isEmpty()) {
                            append("\n暂无召集记录")
                        } else {
                            ranking.forEachIndexed { index, (tag, count) ->
                                append("\n${index + 1}. $tag　${count}次")
                            }
                        }
                    }
                    sendQuoteReply(reply)
                }

                "call", "召集"-> {   // 召集标签下的玩家
                    val group = subject as? Group
                    if (group == null) {
                        sendQuoteReply("此指令只能在群聊中执行")
                        return
                    }
                    val tag = args[1].content
                    if (tag !in NotifyData.tags) {
                        sendQuoteReply("[召集失败] 标签「$tag」不存在${fuzzyMatchLine(listOf(tag))}\n请使用「${commandPrefix}n list」查看可用标签")
                        return
                    }
                    resetDailyCount()
                    val caller = NotifyData.players.getOrPut(qq) { NotifyPlayer() }
                    if (caller.callCount >= MAX_DAILY_CALL) {
                        sendQuoteReply("[召集失败] 您今日的召集次数已达上限（${MAX_DAILY_CALL}次），请明日再试")
                        return
                    }
                    val hour = LocalTime.now().hour
                    val candidates = NotifyData.players.filter { (id, player) ->
                        id != qq &&
                        player.enable &&
                        player.tags.contains(tag) &&
                        inTimeRange(player.time, hour) &&
                        group.getMember(id) != null
                    }.keys.toList()
                    if (candidates.isEmpty()) {
                        sendQuoteReply("[召集失败] 「$tag」当前没有可通知的玩家，可能不在群内/未开启通知/不在接收时间段内")
                        return
                    }
                    caller.callCount++   // 召集成功才计入次数
                    caller.totalCall++
                    NotifyData.totalCallCount++
                    NotifyData.tagCallCount[tag] = (NotifyData.tagCallCount[tag] ?: 0) + 1
                    val totalRegistered = NotifyData.players.values.count { it.tags.contains(tag) }   // 本标签登记的玩家总数
                    val targets = if (candidates.size > MAX_AT_COUNT) candidates.shuffled().take(MAX_AT_COUNT) else candidates
                    targets.forEach { id -> NotifyData.players[id]?.let { it.notified++ } }   // 累计被通知次数
                    NotifyData.save()
                    val message = buildMessageChain {
                        +PlainText("【开游戏通知】\n$name 召集「$tag」玩家：\n\n")
                        targets.forEach {
                            +At(it)
                            +PlainText(" ")
                        }
                        +PlainText("\n\n总计 $totalRegistered 人，成功通知 ${targets.size} 人")
                        if (candidates.size > MAX_AT_COUNT) +PlainText("（随机抽取）")
                        +PlainText("\n今日剩余召集次数：${MAX_DAILY_CALL - caller.callCount}")
                    }
                    sendMessage(message)
                }

                // admin指令
                "addtag", "添加标签"-> {   // 添加标签至标签库
                    adminOnly(this)
                    val inputTags = parseTags(args)
                    if (inputTags.isEmpty()) {
                        sendQuoteReply("[参数不足] 请提供至少一个标签，多个标签用空格分隔")
                        return
                    }
                    val added = mutableListOf<String>()
                    val exist = mutableListOf<String>()
                    for (tag in inputTags) {
                        if (NotifyData.tags.add(tag)) added.add(tag) else exist.add(tag)
                    }
                    NotifyData.save()
                    val reply = buildString {
                        append("·标签库添加结果：")
                        if (added.isNotEmpty()) append("\n✅ 添加成功：${added.joinToString(" ")}")
                        if (exist.isNotEmpty()) append("\n⚠️ 已存在：${exist.joinToString(" ")}")
                    }
                    sendQuoteReply(reply)
                }

                "deltag", "删除标签"-> {   // 从标签库删除标签（级联移除玩家登记）
                    adminOnly(this)
                    val inputTags = parseTags(args)
                    if (inputTags.isEmpty()) {
                        sendQuoteReply("[参数不足] 请提供至少一个标签，多个标签用空格分隔")
                        return
                    }
                    val removed = mutableListOf<String>()
                    val notFound = mutableListOf<String>()
                    for (tag in inputTags) {
                        if (NotifyData.tags.remove(tag)) {
                            NotifyData.players.values.forEach { it.tags.remove(tag) }   // 级联移除玩家已登记的该标签
                            NotifyData.tagCallCount.remove(tag)
                            removed.add(tag)
                        } else {
                            notFound.add(tag)
                        }
                    }
                    NotifyData.save()
                    val reply = buildString {
                        append("·标签库删除结果：")
                        if (removed.isNotEmpty()) append("\n✅ 删除成功（已同步移除玩家登记）：${removed.joinToString(" ")}")
                        if (notFound.isNotEmpty()) append("\n⚠️ 不存在：${notFound.joinToString(" ")}")
                    }
                    sendQuoteReply(reply)
                }

                "user", "查询"-> {   // 查询指定用户信息
                    adminOnly(this)
                    val id = args[1].content.replace("@", "").toLong()
                    val player = NotifyData.players[id]
                    if (player == null) {
                        sendQuoteReply("用户 $id 没有任何登记信息")
                        return
                    }
                    resetDailyCount()
                    sendQuoteReply(buildPlayerInfo("${getNickname(this, id)}($id)", player))
                }

                else-> {
                    sendQuoteReply("[参数不匹配]\n请使用「${commandPrefix}n help」来查看指令帮助")
                }
            }
        } catch (_: NumberFormatException) {
            sendQuoteReply("数字转换错误，请检查指令")
        } catch (e: PermissionDeniedException) {
            sendQuoteReply("[操作无效] ${e.message}")
        } catch (_: IndexOutOfBoundsException) {
            sendQuoteReply("[参数不足]\n请使用「${commandPrefix}n help」来查看指令帮助")
        } catch (e: Exception) {
            logger.warning(e)
            sendQuoteReply("[指令执行未知错误]\n可能由于bot发消息出错，请联系铁蛋查看后台：${e::class.simpleName}(${e.message})")
        }
    }

    /**
     * 解析批量标签参数
     */
    private fun parseTags(args: MessageChain): List<String> {
        return args.drop(1).map { it.content.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    /**
     * 在标签库中模糊搜索关键词（结果汇总去重），生成🔍模糊匹配提示行；无匹配时返回空字符串
     */
    private fun fuzzyMatchLine(keywords: List<String>): String {
        val matched = keywords.flatMap { keyword ->
            NotifyData.tags.filter { it.contains(keyword, ignoreCase = true) }
        }.distinct()
        return if (matched.isEmpty()) "" else "\n🔍 模糊匹配结果：${matched.joinToString(" ")}"
    }

    /**
     * 标签列表输出：有登记的标签一行，0登记的标签另起一行
     */
    private fun formatTagLines(tags: List<Pair<String, Int>>): String {
        val registered = tags.filter { it.second > 0 }.joinToString(" ") { it.first }
        val zero = tags.filter { it.second == 0 }.joinToString(" ") { it.first }
        return listOf(registered, zero).filter { it.isNotEmpty() }.joinToString("\n")
    }

    /**
     * 构建玩家登记信息文本
     */
    private fun buildPlayerInfo(who: String, player: NotifyPlayer): String {
        return "·${who}的登记信息：\n" +
                "通知开关：${if (player.enable) "🟢开启" else "🔴关闭"}\n" +
                "接收时间段：${player.time}\n" +
                "今日已用召集：${player.callCount} / $MAX_DAILY_CALL\n" +
                "累计发起召集：${player.totalCall}\n" +
                "累计被通知：${player.notified}\n" +
                "🏷️ 已登记标签（${player.tags.size}个）：\n" +
                player.tags.joinToString(" ").ifEmpty { "无" }
    }

    /**
     * 每日召集次数重置
     */
    private fun resetDailyCount() {
        val today = LocalDate.now().toString()
        if (NotifyData.callDate != today) {
            NotifyData.players.values.forEach { it.callCount = 0 }
            NotifyData.callDate = today
            NotifyData.save()
        }
    }

    /**
     * 判断当前小时是否在玩家设置的时间段内（支持跨天）
     */
    private fun inTimeRange(range: String, hour: Int): Boolean {
        val match = timeRangeRegex.find(range) ?: return true   // 格式异常时视为全天
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        return if (start <= end) hour in start..end else hour >= start || hour <= end
    }
}
