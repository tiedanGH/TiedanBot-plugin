package com.tiedan.plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object PointData : AutoSavePluginData("PointData") {

    @ValueDescription("转账开关")
    var TransferFunction: Boolean by value(false)

    @ValueDescription("积分数据")
    var PointData: MutableMap<Long, Long> by value()

}