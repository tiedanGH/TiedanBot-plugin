package site.tiedan.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.ExceptionInEventHandlerException
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.warning
import site.tiedan.TiedanGame.logger
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
     * 群消息去重：同一条消息仅首个bot继续处理，其余bot的事件被拦截
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun MessageEvent.multiBotCheck() {
        val group = subject as? Group ?: return                                 // 仅群聊存在多bot冲突
        if (Bot.instances.count { it.getGroup(group.id) != null } <= 1) return   // 本群仅一个bot

        val botId = bot.id
        // 以「群+发送者+消息时间+内容」作为跨bot的消息标识
        val key = "${group.id}:${sender.id}:${time}:${message.content}"
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
