package site.tiedan.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object WhiteListData : AutoSavePluginData("WhiteListData") {

    @ValueDescription("白名单列表")
    var WhiteList: MutableMap<Long, MutableMap<Long, String>> by value(mutableMapOf(10000L to mutableMapOf(1919810L to "no_desc")))

}