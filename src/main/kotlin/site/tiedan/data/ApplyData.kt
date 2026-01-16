package site.tiedan.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object ApplyData : AutoSavePluginData("ApplyData") {

    @ValueName("已进行申请的用户")
    var ApplyLock: MutableList<Long> by value()

    @ValueName("白名单申请列表")
    var WhiteListApplication: MutableMap<Long, MutableMap<String, String>> by value()

    @ValueName("admin申请列表")
    var AdminApplication: MutableMap<Long, String> by value()

}