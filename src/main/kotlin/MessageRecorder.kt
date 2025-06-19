import config.BotConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.event.events.source
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.findIsInstance

internal object MessageRecorder : SimpleListenerHost() {

    private val records: MutableMap<Long, MutableList<MessageSource>> = HashMap()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessageEvent.mark() {
        val record = records.getOrPut(subject.id, ::mutableListOf)
        if (record.size >= BotConfig.recordLimit) {
            record.removeFirst()
        }
        record.add(source)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessagePostSendEvent<*>.mark() {
        val record = records.getOrPut(target.id, ::mutableListOf)
        if (record.size >= BotConfig.recordLimit) {
            record.removeFirst()
        }
        record.add(source ?: return)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun MessageRecallEvent.mark() {
        when (this) {
            is MessageRecallEvent.FriendRecall -> records[author.id]?.removeIf {
                it.ids.contentEquals(messageIds) && it.internalIds.contentEquals(messageInternalIds)
            }
            is MessageRecallEvent.GroupRecall -> records[group.id]?.removeIf {
                it.ids.contentEquals(messageIds) && it.internalIds.contentEquals(messageInternalIds)
            }
        }
    }

    fun from(member: Member): MessageSource? {
        return records[member.group.id]?.findLast { it.fromId == member.id }
    }

    fun target(contact: Contact): MessageSource? {
        return records[contact.id]?.findLast { it.fromId == contact.bot.id }
    }

    fun quote(event: MessageEvent): MessageSource? {
        return event.message.findIsInstance<QuoteReply>()?.source
            ?: records[event.subject.id]?.findLast { it.fromId != event.source.fromId }
    }

    fun quoteMessage(event: MessageEvent): MessageChain? {
        val quote = event.message.findIsInstance<QuoteReply>() ?: return null
        val recordList = records[event.subject.id] ?: return null
        val sourceIds = quote.source.ids
        return recordList.asReversed().firstOrNull { rec -> rec.ids.any { it in sourceIds } }?.originalMessage
    }
}
