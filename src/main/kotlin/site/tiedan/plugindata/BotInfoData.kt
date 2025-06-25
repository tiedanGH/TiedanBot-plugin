package site.tiedan.plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object BotInfoData : AutoSavePluginData("BotInfoData") {

    @ValueName("总计发送消息")
    var totalMsgNum: Int by value(0)

    @ValueName("总计发送图片")
    var totalImageNum: Int by value(0)

    @ValueName("昨日发送消息")
    var yesterdayMsgNum: Int by value(0)

    @ValueName("昨日发送图片")
    var yesterdayImageNum: Int by value(0)

    @ValueName("今日发送消息")
    var todayMsgNum: Int by value(0)

    @ValueName("今日发送图片")
    var todayImageNum: Int by value(0)

    @ValueName("今日私信图片")
    var todayPrivateImageNum: Int by value(0)

    @ValueName("启动时间")
    var startTime: Long by value(0L)

}
