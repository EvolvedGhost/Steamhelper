package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import java.text.SimpleDateFormat
import java.util.*

/**
 * Steam每周销量榜单
 * 默认获取源来自Steam官方
 */
class SteamWeek {
    /** Steam每周榜单获取源 */
    var url = SteamhelperPluginSetting.urlWeekly

    /** Steam每周榜单更新时间输出格式，默认格式为：2022-02-20 16:00:00 星期日 */
    var timeFormat = SteamhelperPluginSetting.timeFormat

    /** Steam每周榜单更新时间输出时区，默认格式为：东八区 */
    var timeZone = SteamhelperPluginSetting.timeZone

    /** Steam每周榜单更新时间戳，需要检查是否为null */
    var timestamp: Long = 0

    /** 上次报错的原因，需要检查是否为null */
    var exception = String()

    /** Steam每周榜单游戏名 */
    var titleArr = arrayOfNulls<String>(10)

    /** Steam每周榜单游戏链接 */
    var linkArr = arrayOfNulls<String>(10)

    /** Steam每周榜单是否初始化 */
    var isInit = false

    /**
     * 获取、刷新Steam每周榜单
     * @return Boolean 是否成功
     */
    fun refreshSteamWeeklyTopSellers(): Boolean {
        return try {
            // 连接SteamApi
            val get = SSLHelper().getDocument(url)
            // 获取Steam每周榜单游戏名和链接，并预处理链接
            val items = get.getElementsByTag("item")
            for (i in 0 until 10) {
                titleArr[i] = items[i].getElementsByTag("title").text()
                linkArr[i] = items[i].getElementsByTag("link").text().substringBefore("?t=")
            }
            // 获取Steam每周榜单更新时间
            val textDate = get.getElementsByTag("pubDate").first()?.text()
            // 格式化Steam大促时间为时间戳（格式样例：Sun, 20 Feb 2022 00:00:00 -0800）
            timestamp = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(textDate).time
            isInit = true
            true
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
            exception = e.toString()
            false
        }
    }

    /**
     * 返回Steam每周榜单游戏格式化后更新时间
     * @return String? Steam每周榜单游戏格式化后更新时间，需要检查是否为null
     */
    fun getSteamWeeklyFormattedTime(): String? {
        return if (isInit) {
            val dateFormat = SimpleDateFormat(timeFormat)
            dateFormat.timeZone = TimeZone.getTimeZone(timeZone)
            dateFormat.format(timestamp)
        } else null
    }
}