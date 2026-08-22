package site.tiedan.module

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.debug
import site.tiedan.TiedanGame.logger
import java.util.concurrent.ConcurrentHashMap

/**
 * ### 多bot指令冲突处理
 * 多个bot同处一个群聊时，同一条指令消息会被每个bot分别接收并执行，导致重复回复。
 * 此处按「首个bot认领」的方式自动去重：同一条群消息仅首个执行的bot生效，其余bot静默跳过。
 */
object MultiBotGuard {

    /** 同一条消息的认领有效期（毫秒） */
    private const val CLAIM_TTL = 2_000L

    /** 触发过期记录清理的缓存条数阈值 */
    private const val CLEAN_THRESHOLD = 100

    /** 指令认领记录：消息标识 -> (认领的botId, 认领时间) */
    private val claims = ConcurrentHashMap<String, Pair<Long, Long>>()

    /**
     * 认领当前指令
     * @return true表示可继续执行；false表示已由其他bot处理，应直接返回
     */
    fun CommandSender.claimCommand(): Boolean {
        val botId = bot?.id ?: return true                        // 控制台等无bot场景不处理
        val group = subject as? Group ?: return true              // 仅群聊存在多bot冲突
        if (this !is CommandSenderOnMessage<*>) return true        // 非消息触发的指令不处理
        if (Bot.instances.count { it.getGroup(group.id) != null } <= 1) return true   // 本群仅一个bot

        val key = "${group.id}:${user?.id}:${fromEvent.message.content}"
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
            logger.debug { "[多bot冲突] 指令已由 ${claims[key]?.first} 处理，跳过 $botId 的重复执行：$key" }
        }
        return allowed
    }
}
