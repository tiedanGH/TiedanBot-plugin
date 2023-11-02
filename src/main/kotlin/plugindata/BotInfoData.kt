package com.tiedan.plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object BotInfoData : AutoSavePluginData("BotInfoData") {

    @ValueName("总计发送消息")
    @ValueDescription("所有消息总数")
    var totalMsgNum: Int by value(0)

    @ValueName("总计发送图片")
    @ValueDescription("图片消息总数")
    var totalImageNum: Int by value(0)

    @ValueName("昨日发送消息")
    @ValueDescription("昨日发送消息数")
    var yesterdayMsgNum: Int by value(0)

    @ValueName("昨日发送图片")
    @ValueDescription("昨日发送图片数")
    var yesterdayImageNum: Int by value(0)

    @ValueName("今日发送消息")
    @ValueDescription("今日发送消息数")
    var todayMsgNum: Int by value(0)

    @ValueName("今日发送图片")
    @ValueDescription("今日发送图片数")
    var todayImageNum: Int by value(0)

    @ValueName("今日私信图片")
    @ValueDescription("今日私信图片数")
    var todayFriendImageNum: Int by value(0)

}
