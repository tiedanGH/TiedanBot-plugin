package site.tiedan.plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object AdminListData : AutoSavePluginData("AdminListData") {

    @ValueDescription("管理员列表")
    var AdminList: MutableSet<Long> by value(mutableSetOf(114514))

}