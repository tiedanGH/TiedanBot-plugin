package site.tiedan.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object FunctionConfig : AutoSavePluginConfig("FunctionConfig") {

    @ValueDescription("启用白名单功能")
    var WhiteList_enable: Boolean by value(false)

    @ValueDescription("私信白名单申请群聊（默认不启用私信白名单功能，多平台账号始终不启用）")
    var apply_group: Long by value(0L)

    @ValueDescription("启用专注功能")
    var focus_enable: Boolean by value(false)

    @ValueDescription("专注功能服务对象")
    var focus_to: Long by value()

    @ValueDescription("启用自动签到")
    var auto_sign: Boolean by value(false)

    @ValueDescription("启用计时器指令")
    var timer_enable: Boolean by value(true)

    @ValueDescription("启用看戏指令")
    var kanxi_enable: Boolean by value(true)

    @ValueDescription("启用开游戏通知功能")
    var notify_enable: Boolean by value(true)

}
