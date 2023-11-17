package com.tiedan.plugindata

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

/**
 * ### 此部分不再使用，已迁移至 [mirai-console-jcc-plugin](https://github.com/tiedanGH/mirai-console-jcc-plugin/blob/pastebin/src/main/kotlin/PastebinData.kt)
 */
@PublishedApi
internal object PastebinData : AutoSavePluginData("PastebinData") {

    @ValueName("pastebin代码数据")
    var pastebin: MutableMap<String, MutableMap<String, String>> by value(mutableMapOf("example" to mutableMapOf("language" to "python", "pastebinUrl" to "https://glot.io", "stdin" to "1")))

}