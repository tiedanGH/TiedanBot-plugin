# TiedanBot plugin

👉[简体中文](README_cn.md)

> A simple bot management plugin written based on [Mirai Console](https://github.com/mamoe/mirai)

The functions currently implemented include the following sections:

* Help-related: Users can view available help for the bot
* Data statistics: Statistics of bot message data
* Group WhiteList: Users can make notes on each group in the WhiteList
* Behavior-related: Notify the owner when bot adds new friends or is invited to join new group
* Application-related: Users can apply for WhiteList or Admin permissions
* Email-related: backup logs, sending emails when bot abnormal offline

## Commands

`<>` is a required parameter, `[]` is an optional parameter.

The command prefix will automatically change according to the `commandPrefix` parameter in `config\Console\Command.yml`.

### CommandBotHelp

- Used to view bot help

| Command       | Description               |
|:--------------|:--------------------------|
| `/bot help`   | view bot help             |
| `/bot info`   | view bot data information |
| `/bot status` | view mirai status         |

- The bot message data is collected through `MessagePostSendEvent`. [Scheduled task](src/main/kotlin/timer/AutoUpdateDailyData.kt) is registered at startup, and the statistical data is automatically updated at 0 o'clock every day. The data is saved in [BotInfoData](src/main/kotlin/plugindata/BotInfoData.kt).

### CommandAdmin

- Used by administrators to manage bot functions

| Command                               | Description                   |
|:--------------------------------------|:------------------------------|
| `/admin help`                         | view available help for admin |
| `/admin list`                         | view the AdminList            |
| `/admin BlackList`                    | view the BlackList            |
| `/admin op <QQ>`                      | add administrator`*`          |
| `/admin deop <QQ>`                    | remove administrator`*`       |
| `/admin black <qq>`                   | add/remove BlackList`*`       |
| `/admin shutdown`                     | shutdown command`*`           |
| `/admin transfer <QQ> <point>`        | point transfer`*`             |
| `/admin send <QQ> [message]`          | send message                  |
| `/admin WhiteList [info]`             | view the WhiteList            |
| `/admin setWhiteList <enable/diable>` | set the WhiteList status      |
| `/admin addWhiteList [group] [desc]`  | add group to WhiteList        |
| `/admin delWhiteList [group]`         | remove group from WhiteList   |
| `/admin focus <group/disable>`        | focus mode (not implemented)  |
| `/admin reload`                       | reload config and data`*`     |

- Commands with `*` can only be executed by user with **master** permission, see [BotConfig](#BotConfig) for details.

- When adding and removing WhiteList, if the `group` parameter is empty, it can only be executed in groups. The default value is the id of the group where the command is executed.

### CommandApply

- Used by users to apply for group WhiteList or admin permission. The application data is saved in [ApplyData](src/main/kotlin/plugindata/ApplyData.kt).

| Command                                       | Description                 |
|:----------------------------------------------|:----------------------------|
| `/apply help`                                 | view apply help             |
| `/apply white <group> <reason>`               | apply for group WhiteList   |
| `/apply admin <reason>`                       | apply for admin permission  |
| `/apply cancel`                               | cancel personal application |
| `/apply list [type]`                          | view application list`*`    |
| `/apply handle <qq> <Agree/Reject> [Remarks]` | handle application`*`       |

- Commands with `*` can only be executed by user with **admin** permission, see [BotConfig](#BotConfig) for details.

### CommandPastebin

- Used to view and add pastebin codes and run them by using the online compiler. The data is saved in [PastebinData](src/main/kotlin/plugindata/PastebinData.kt).

| Command                                                                        | Description                                   |
|:-------------------------------------------------------------------------------|:----------------------------------------------|
| `/pastebin help`                                                               | view pastebin help                            |
| `/pastebin list`                                                               | view the list                                 |
| `/pastebin info <name>`                                                        | view specific parameters and running examples |
| `/run <name> [stdin]`                                                          | run pastebin code                             |
| `/pastebin add <name> <author> <language> <pastebinUrl> [Sample input(stdin)]` | add new pastebin data                         |
| `/pastebin set <name> <parameter> <content>`                                   | modify one parameter of an item in the data   |

## Config

### BotConfig

- save in the file `config/com.tiedan.TiedanGame/BotConfig.yml`

```text
# Fill in the bot owner which have the master permission
master: 1145141919810
# Enable quote replies
quote_enable: true
# Administrator list (can be added and deleted through commands)
AdminList:
  - 114514
# Enable the WhiteList function (the status can be modified through commands)
WhiteList_enable: false
# The WhiteList (can be added and deleted through commands)
WhiteList:
  1919810: none
  114514: no_desc
# Enable focus function (not implemented)
focus_enable: false
# Focus on functional service objects (not implemented)
focus_to: 0
#Set daily image upper limit detection standard
dailyLimit: 2000
```

### MailConfig

- save in the file `config/com.tiedan.TiedanGame/MailConfig.yml`

```text
# Whether to send mails when the bot offline
offline_notify: true
# When the bot is stopped normally, the mail will also be sent.
close_notify: false
# When the bot offline, the default address for receiving mails
bot_offline: ''
# When backing up logs, the default address for receiving mails
log_backup: ''
```

When the bot is started for the first time, the file `mail.properties` will be generated, which needs to be configured `mail.host`, `mail.user`, `mail.password`, `mail.from`.

references：[QQMail](https://service.mail.qq.com/detail/0/427) [Gmail](https://support.google.com/mail/answer/7126229)
