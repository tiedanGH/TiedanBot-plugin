package site.tiedan.data

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

@PublishedApi
internal object DomainWhiteListData : AutoSavePluginData("DomainWhiteListData") {

    @ValueDescription("域名白名单")
    var WhiteList: MutableSet<String> by value(mutableSetOf())

}