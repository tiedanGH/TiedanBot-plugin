package com.tiedan.timer

import com.tiedan.TiedanGame.save
import com.tiedan.plugindata.BotInfoData
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import java.util.*

class AutoUpdateDailyData(private val logger: MiraiLogger) : TimerTask(){
    override fun run() {
        BotInfoData.yesterdayMsgNum = BotInfoData.todayMsgNum
        BotInfoData.yesterdayImageNum = BotInfoData.todayImageNum
        BotInfoData.todayMsgNum = 0
        BotInfoData.todayImageNum = 0
        BotInfoData.todayFriendImageNum = 0
        BotInfoData.save()
        logger.info { "定时更新数据成功！" }
    }
}

class DateTime {
    companion object{
        fun getCal(hour: Int, minute: Int, second: Int, millisecond: Int): Date {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, second)
            cal.set(Calendar.MILLISECOND, millisecond)
            return Date(cal.timeInMillis)
        }
    }
}