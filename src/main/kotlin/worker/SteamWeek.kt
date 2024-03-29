package com.evolvedghost.mirai.steamhelper.worker

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.evolvedghost.mirai.steamhelper.utils.SSLHelper
import com.evolvedghost.mirai.steamhelper.utils.pluginExceptionHandler
import java.text.SimpleDateFormat
import java.util.*

/**
 * Steam每周销量榜单
 * 默认获取源来自Steam官方
 */
class SteamWeek {
    /** Steam每周榜单获取源 */
    var url = SteamhelperPluginSetting.urlWeekly

    /** Steam每周榜单更新时间戳，需要检查是否为null */
    var timestamp: Long = 0

    /** 上次报错的原因，需要检查是否为null */
    var exception = String()

    /** Steam每周榜单游戏名 */
    var titleArr = mutableListOf<String>()

    /** Steam每周榜单游戏链接 */
    var linkArr = mutableListOf<String>()

    /** Steam每周榜单是否初始化 */
    var isInit = false

    /**
     * 获取、刷新Steam每周榜单
     * @return Boolean 是否成功
     */
    fun refreshSteamWeeklyTopSellers(): Boolean {
        val tempTitleArr = mutableListOf<String>()
        val tempLinkArr = mutableListOf<String>()
        return try {
            // 连接SteamApi
            val get = SSLHelper().getDocument(url)
            // 获取Steam每周榜单游戏名和链接，并预处理链接
            val items = get.getElementsByTag("item")
            for (i in 0 until items.size) {
                tempTitleArr.add(items[i].getElementsByTag("title").text())
                tempLinkArr.add(items[i].getElementsByTag("link").text().substringBefore("?t="))
            }
            // 获取Steam每周榜单更新时间
            val textDate = get.getElementsByTag("pubDate").first()?.text()
            // 格式化Steam大促时间为时间戳（格式样例：Sun, 20 Feb 2022 00:00:00 -0800）
            timestamp = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(textDate).time
            isInit = true
            linkArr = tempLinkArr
            titleArr = tempTitleArr
            true
        } catch (e: Exception) {
            pluginExceptionHandler("Steam周促获取", e)
            exception = e.toString()
            false
        }
    }
}