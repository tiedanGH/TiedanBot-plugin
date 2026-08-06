package site.tiedan.data

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 开游戏通知的玩家数据
 */
@Serializable
data class NotifyPlayer(
    /** 是否开启通知（关闭后不被检索和@） */
    var enable: Boolean = true,
    /** 可用时间段（支持跨天如16-3；分天模式下为空格分隔的多组，与week分组一一对应） */
    var time: String = "9-22",
    /** 每周通知范围（空值代表整周使用time；分天模式下为空格分隔的多组星期，如"1-5 6-7"） */
    var week: String = "",
    /** 登记的标签 */
    val tags: MutableSet<String> = mutableSetOf(),
    /** 今日已用召集次数 */
    var callCount: Int = 0,
    /** 累计发起召集次数 */
    var totalCall: Int = 0,
    /** 累计被通知次数 */
    var notified: Int = 0,
)

@PublishedApi
internal object NotifyData : AutoSavePluginData("NotifyData") {

    @ValueDescription("标签库")
    var tags: MutableSet<String> by value()

    @ValueDescription("玩家数据")
    var players: MutableMap<Long, NotifyPlayer> by value()

    @ValueDescription("召集计数日期")
    var callDate: String by value("")

    @ValueDescription("总召集次数")
    var totalCallCount: Int by value(0)

    @ValueDescription("标签召集次数")
    var tagCallCount: MutableMap<String, Int> by value()

    @ValueDescription("标签别名（别名 → 真实标签名）")
    var tagAlias: MutableMap<String, String> by value()

}
