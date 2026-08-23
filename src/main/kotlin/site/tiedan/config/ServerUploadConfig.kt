package site.tiedan.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object ServerUploadConfig : AutoSavePluginConfig("ServerUploadConfig") {

    @ValueDescription("lgtbot路径")
    var lgtbot_path: String by value("")

    @ValueDescription("草图上传路径")
    var grass_path: String by value("")

}
