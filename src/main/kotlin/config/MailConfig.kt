package com.tiedan.config

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.info
import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.io.path.writeText

@PublishedApi
internal object MailConfig : ReadOnlyPluginConfig("MailConfig") {

    @ValueName("offline_notify")
    @ValueDescription("机器人下线时，发送邮件")
    val offline: Boolean by value(true)

    @ValueName("close_notify")
    @ValueDescription("机器人正常关闭时，也发送邮件")
    val close: Boolean by value(false)

    @ValueName("bot_offline")
    @ValueDescription("机器人下线时，默认接收邮件的地址")
    val offline_mail: String by value("")

    @ValueName("log_backup")
    @ValueDescription("备份日志时，默认接收邮件的地址")
    val log_mail: String by value("")

    var properties = Path("mail.properties")
        private set

    @OptIn(ConsoleExperimentalApi::class)
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        if (owner is JvmPlugin) {
            properties = owner.resolveConfigPath("mail.properties")
            if (properties.notExists()) {
                properties.writeText(
                    """
                    mail.host=smtp.example.com
                    mail.auth=true
                    mail.user=xxx
                    mail.password=****
                    mail.from=xxx@example.com
                    mail.store.protocol=smtp
                    mail.transport.protocol=smtp
                    # smtp
                    mail.smtp.starttls.enable=true
                    mail.smtp.auth=true
                    mail.smtp.timeout=15000
                """.trimIndent()
                )
                owner.logger.info { "邮件配置文件已生成，请修改内容以生效 ${properties.toUri()}" }
            }
        }
    }
}