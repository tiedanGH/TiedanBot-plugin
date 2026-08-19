package site.tiedan.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object BlackListData : AutoSavePluginData("BlackListData") {

    @ValueName("黑名单用户")
    var BlackList: MutableList<Long> by value()

    @ValueName("黑名单群聊")
    var BlackListGroup: MutableMap<Long, MutableSet<Long>> by value()

}