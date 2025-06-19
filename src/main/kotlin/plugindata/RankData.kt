package plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object RankData : AutoSavePluginData("RankData") {

    @ValueName("记录功能状态")
    var enable_record: Boolean by value(false)

    @ValueName("比赛链接URL")
    var URL: String by value("none")

    @ValueName("牧场比赛数据")
    var rankData: MutableMap<Long, MutableMap<String, MutableList<Int>>> by value(mutableMapOf(
        2295824927 to mutableMapOf(
            "count" to mutableListOf(20, 5),
            "points" to mutableListOf(2000, 100, 30, 20)),
        114514L to mutableMapOf(
            "count" to mutableListOf(3, 1),
            "points" to mutableListOf(5000, 1000, 300, 200))
    ))

//    @ValueName("大海战比赛数据")
//    var rankData: MutableMap<Long, MutableMap<String, MutableList<Int>>> by value(mutableMapOf(
//        2295824927 to mutableMapOf(
//            "count" to mutableListOf(20, 5),
//            "points" to mutableListOf(2000, 100, 30, 20)),
//        114514L to mutableMapOf(
//            "count" to mutableListOf(3, 1),
//            "points" to mutableListOf(5000, 1000, 300, 200))
//    ))

}