package com.tiedan.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object BotConfig : AutoSavePluginConfig("BotConfig") {

    @ValueDescription("MASTER")
    var master: Long by value(1145141919810)

    @ValueDescription("启用引用回复")
    var quote_enable: Boolean by value(true)

    @ValueDescription("管理员列表")
    var AdminList: MutableSet<Long> by value(mutableSetOf(114514))

    @ValueDescription("启用白名单功能")
    var WhiteList_enable: Boolean by value(false)

    @ValueDescription("白名单列表")
    var WhiteList: MutableMap<Long, String> by value(mutableMapOf(1919810.toLong() to "none"))

    @ValueDescription("时区")
    var TimeZone : MutableList<String> by value(mutableListOf("Asia/Shanghai", "北京"))

    @ValueDescription("启用专注功能")
    var focus_enable: Boolean by value(false)

    @ValueDescription("专注功能服务对象")
    var focus_to: Long by value()

    @ValueDescription("设置每日图片上限检测标准")
    var dailyLimit: Int by value(2000)

}