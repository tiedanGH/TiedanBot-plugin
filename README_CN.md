# TiedanBot plugin

👉[English](README.md)

> 基于 [Mirai Console](https://github.com/mamoe/mirai) 编写的简单机器人管理插件

目前实现的功能有:

* 帮助相关：用户查看机器人可用帮助
* 数据统计：统计机器人消息数据
* 群聊相关：群聊白名单，可以对群聊进行备注
* 行为相关：bot添加新好友、被邀请进群时进行通知
* 申请相关：用户申请白名单/管理权限
* 邮件相关：备份日志、异常下线时发送邮件

## 指令

`<>`内为必填参数，`[]`为可选参数

### CommandBotHelp

- 用于查看bot相关帮助

| Command       | Description |
|:--------------|:------------|
| `#bot help`   | 查看bot相关帮助   |
| `#bot info`   | 查看bot数据信息   |
| `#bot status` | 查看mirai状态   |

- bot发言数据通过`MessagePostSendEvent`进行统计，在启动时注册 [定时任务](src/main/kotlin/timer/AutoUpdateDailyData.kt)，每日0点自动更新统计数据。数据保存在 [BotInfoData](src/main/kotlin/plugindata/BotInfoData.kt) 中。

### CommandAdmin

- 用于管理员对bot功能进行管理

| Command                               | Description |
|:--------------------------------------|:------------|
| `#admin help`                         | 查看admin可用帮助 |
| `#admin list`                         | 查看管理员列表     |
| `#admin op <QQ>`                      | 添加管理员`*`    |
| `#admin deop <QQ>`                    | 移除管理员`*`    |
| `#admin shutdown`                     | 关机指令`*`     |
| `#admin transfer <QQ> <point>`        | bot积分转账`*`  |
| `#admin send [message]`               | bot消息发送`*`  |
| `#admin WhiteList [info]`             | 查看白名单列表     |
| `#admin setWhiteList <enable/diable>` | 设置白名单开关状态   |
| `#admin addWhiteList [group] [desc]`  | 添加白名单       |
| `#admin delWhiteList [group]`         | 移除白名单       |
| `#admin focus <group/disable>`        | 专注模式（未实现）   |
| `#admin reload`                       | 重载配置及数据`*`  |

- 带`*`的指令仅拥有master权限才可执行，详见 [BotConfig](#机器人配置BotConfig)。

- 添加和移除白名单时，不填写`group`参数时仅可在群聊中执行，默认为执行指令的群聊。

### CommandApply

- 用于用户申请群聊白名单或admin权限，申请数据保存在 [ApplyData](src/main/kotlin/plugindata/ApplyData.kt) 中。

| Command                           | Description |
|:----------------------------------|:------------|
| `#apply help`                     | 查看apply帮助   |
| `#apply white <group> <reason>`   | 申请群聊白名单     |
| `#apply admin <reason>`           | 申请admin权限   |
| `#apply cancel`                   | 取消个人申请      |
| `#apply list [type]`              | 查看申请列表`*`   |
| `#apply handle <qq> <同意/拒绝> [备注]` | 处理申请`*`     |

- 带`*`的指令仅拥有admin权限才可执行，详见 [BotConfig](#机器人配置BotConfig)。

### CommandPastebin

- 用于查看和添加pastebin代码，并使用在线编译器运行，数据保存在 [PastebinData](src/main/kotlin/plugindata/PastebinData.kt) 中。

| Command                                                    | Description  |
|:-----------------------------------------------------------|:-------------|
| `#pastebin help`                                           | 查看pastebin帮助 |
| `#pastebin list`                                           | 查看完整列表       |
| `#pastebin info <名称>`                                      | 查看具体参数及运行示例  |
| `#run <名称> [stdin]`                                        | 运行pastebin代码 |
| `#pastebin add <名称> <作者> <语言> <pastebinUrl> [示例输入(stdin)]` | 添加pastebin数据 |
| `#pastebin set <名称> <参数名> <内容>`                            | 修改数据中某一项的参数  |

## 配置

### 机器人配置BotConfig

- 保存于文件`config/com.tiedan.TiedanGame/BotConfig.yml`

```text
# 填写bot所有者，拥有master权限
master: 1145141919810
# 启用引用回复
quote_enable: true
# 管理员列表（可通过指令进行添加、删除操作）
AdminList:
  - 114514
# 启用白名单功能（可通过指令修改状态）
WhiteList_enable: false
# 白名单列表（可通过指令进行添加、删除操作）
WhiteList:
  1919810: none
  114514: no_desc
# 启用专注功能（暂未实现）
focus_enable: false
# 专注功能服务对象（暂未实现）
focus_to: 0
# 设置每日图片上限检测标准
dailyLimit: 2000
```

### 邮件配置MailConfig

- 保存于文件`config/com.tiedan.TiedanGame/MailConfig.yml`

```text
# 机器人下线时，是否发送邮件
offline_notify: true
# 机器人正常关闭时，也发送邮件
close_notify: false
# 机器人下线时，默认接收邮件的地址
bot_offline: ''
# 备份日志时，默认接收邮件的地址
log_backup: ''
```

- 在bot首次启动时会生成邮件配置文件`mail.properties`，需要配置 `mail.host`, `mail.user`, `mail.password`, `mail.from`。

参考：[QQMail](https://service.mail.qq.com/detail/0/427) [Gmail](https://support.google.com/mail/answer/7126229)
