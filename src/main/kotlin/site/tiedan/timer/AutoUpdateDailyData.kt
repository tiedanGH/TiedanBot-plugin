package site.tiedan.timer

import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.info
import site.tiedan.TiedanGame.logger
import site.tiedan.TiedanGame.save
import site.tiedan.config.BotConfig
import site.tiedan.data.BotInfoData
import java.util.*

class AutoUpdateDailyData : TimerTask() {
    override fun run() {
        BotInfoData.yesterdayMsgNum = BotInfoData.todayMsgNum
        BotInfoData.yesterdayImageNum = BotInfoData.todayImageNum
        BotInfoData.todayMsgNum = 0
        BotInfoData.todayImageNum = 0
        BotInfoData.todayGroupMsgNum = 0
        BotInfoData.save()
        logger.info { "定时更新数据成功！" }
    }
}

class DateTime {
    companion object {
        fun getCal(hour: Int, minute: Int, second: Int, millisecond: Int): Calendar {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, second)
            cal.set(Calendar.MILLISECOND, millisecond)
            return cal
        }
    }
}

fun calculateNextSignDelay(success: Boolean): Long {
    if (success.not()) {
        return 1800000
    }
    val currentTime = Calendar.getInstance()
    val nextSignTime = DateTime.getCal(0, 0, 0, 0)
    if (currentTime.timeInMillis > nextSignTime.timeInMillis) {
        nextSignTime.add(Calendar.DAY_OF_YEAR, 1)
    }
    return nextSignTime.timeInMillis - currentTime.timeInMillis
}

suspend fun executeDailySign(): Boolean {
    try {
        Bot.getInstanceOrNull(BotConfig.BotId)!!.getGroup(541402580)!!.sendMessage("/g sign")
        return true
    } catch (e: NullPointerException) {
        logger.warning(e)
        logger.warning("签到消息发送失败。原因：$e")
        return true
    } catch (e: Exception) {
        logger.warning(e)
        logger.warning("签到消息发送失败，将在30分钟后重试。原因：$e")
        return false
    }
}