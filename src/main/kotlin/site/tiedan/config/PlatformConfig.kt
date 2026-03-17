package site.tiedan.config

import net.mamoe.mirai.console.data.*

@PublishedApi
internal object PlatformConfig : AutoSavePluginConfig("PlatformConfig") {

    @ValueDescription("其他平台多账号配置（不在列表内默认为使用主账号）")
    val platforms: MutableMap<Long, MutableMap<String, String>> by value(
        mutableMapOf(
            114514L to mutableMapOf(
                "platform" to "kook",
                "whitelist" to "false",
            )
        )
    )
}