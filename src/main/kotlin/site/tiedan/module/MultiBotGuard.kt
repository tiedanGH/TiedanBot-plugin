package site.tiedan.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.warning
import site.tiedan.TiedanGame.logger
import site.tiedan.config.FunctionConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * ### 多bot消息冲突处理
 * 多个bot同处一个群聊时，同一条群消息会被每个bot分别接收，导致指令重复执行、插件重复响应。
 * 此处按「首个bot认领」的方式自动去重：同一条群消息仅首个bot继续处理，其余bot的事件被直接拦截。
 *
 * - 以最高优先级拦截，被拦截的事件不再传递给本插件及其他插件的任何监听器
 * - 仅在群聊内、且该群确实存在多个bot时介入，单bot环境行为完全不变
 * - 记录仅保存在内存中，重启后失效
 */
object MultiBotGuard : SimpleListenerHost() {

    /** 同一条消息的认领有效期（毫秒） */
    private const val CLAIM_TTL = 2_000L

    /** 触发过期记录清理的缓存条数阈值 */
    private const val CLEAN_THRESHOLD = 100

    /** 消息认领记录：消息标识 -> (认领的botId, 认领时间) */
    private val claims = ConcurrentHashMap<String, Pair<Long, Long>>()

    /** 路由正则缓存，避免每条消息重复编译 */
    private val regexCache = ConcurrentHashMap<String, Regex>()

    /** 永不匹配的正则，用于占位无效的路由配置 */
    private val NEVER_MATCH = Regex("(?!)")

    /**
     * 按配置顺序匹配指令路由，返回绑定的botId；未命中时返回null
     * - 同时匹配纯文本内容与mirai码：用于 `[mirai:at:xxx]` 这类元素规则
     * - 绑定的bot不在该群时跳过该规则，交由后续规则或认领逻辑处理
     */
    private fun matchRoute(content: String, miraiCode: String, botsInGroup: List<Bot>): Long? {
        for ((pattern, boundBot) in FunctionConfig.multibot_routes) {
            if (botsInGroup.none { it.id == boundBot }) continue
            val regex = regexCache.getOrPut(pattern) {
                try {
                    Regex(pattern)
                } catch (e: Exception) {
                    logger.warning({ "[多bot路由] 无效的正则配置：$pattern" }, e)
                    NEVER_MATCH
                }
            }
            if (regex.containsMatchIn(content) || regex.containsMatchIn(miraiCode)) return boundBot
        }
        return null
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is ExceptionInEventHandlerException -> {
                logger.warning({ "MultiBotGuard with ${exception.event}" }, exception.cause)
            }
            else -> {
                logger.warning({ "MultiBotGuard" }, exception)
            }
        }
    }

    /**
     * 群消息去重：同一条消息仅一个bot继续处理，其余bot的事件被拦截
     * - 命中指令路由的消息，固定由绑定的bot处理，避免仅存在于该bot的功能被抢占
     * - 未命中路由的消息，按「首个认领」处理
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun MessageEvent.multiBotCheck() {
        val group = subject as? Group ?: return                     // 仅群聊存在多bot冲突
        val botId = bot.id
        // 黑名单群中的bot不参与处理
        if (Events.isBlackListGroup(botId, group.id)) return
        val botsInGroup = Bot.instances.filter {
            it.getGroup(group.id) != null && !Events.isBlackListGroup(it.id, group.id)
        }
        if (botsInGroup.size <= 1) return                            // 本群仅一个可用bot

        val content = message.content                                // 纯文本内容（At呈现为@123456）
        val miraiCode = message.filterNot { it is MessageSource }     // mirai码（At呈现为[mirai:at:123456]）
            .joinToString("") { it.toString() }
        // 指令路由：命中规则的消息固定由绑定的bot处理，不参与认领竞争
        val designated = matchRoute(content, miraiCode, botsInGroup)
        if (designated != null) {
            if (botId != designated) {
                logger.debug { "[多bot路由] 消息已绑定至bot $designated，拦截 $botId 的事件：$content" }
                intercept()
            }
            return
        }
        // 以「群+发送者+消息时间+内容」作为跨bot的消息标识
        val key = "${group.id}:${sender.id}:${time}:${content}"
        val now = System.currentTimeMillis()
        if (claims.size >= CLEAN_THRESHOLD) {
            claims.entries.removeIf { now - it.value.second > CLAIM_TTL }
        }
        // 原子认领：无记录或记录已过期时由当前bot认领
        var allowed = false
        claims.compute(key) { _, previous ->
            if (previous == null || now - previous.second > CLAIM_TTL) {
                allowed = true
                botId to now
            } else {
                previous
            }
        }
        if (!allowed) {
            logger.debug { "[多bot冲突] 消息已由 ${claims[key]?.first} 处理，拦截 $botId 的重复事件：$key" }
            intercept()
        }
    }
}
