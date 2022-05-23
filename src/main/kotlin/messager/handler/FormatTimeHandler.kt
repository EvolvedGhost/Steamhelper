package com.evolvedghost.mirai.steamhelper.messager.handler

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import java.text.SimpleDateFormat
import java.util.*

/**
 * 获取格式化后的时间
 * @param timestamp Long 时间戳
 * @return String 格式化后的时间
 */
fun getFormattedTime(timestamp: Long?): String {
    return if (timestamp == null) "错误"
    else if (timestamp < 0) "已结束"
    else {
        val dateFormat = SimpleDateFormat(SteamhelperPluginSetting.timeFormat, Locale(SteamhelperPluginSetting.timeLanguage))
        dateFormat.timeZone = TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone)
        dateFormat.format(timestamp)
    }
}