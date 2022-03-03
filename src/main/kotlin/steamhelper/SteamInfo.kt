package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import java.text.SimpleDateFormat
import java.util.*

/**
 * Steam相关信息
 * 包含Steam状态与大促情况
 * 默认获取源来自Keylol（其乐）
 */
class SteamInfo {
    /** Steam状态与大促情况获取源 */
    var url = SteamhelperPluginSetting.urlInfo

    /** Steam大促时间输出格式，默认格式为：2022-02-20 16:00:00 星期日 */
    var timeFormat = SteamhelperPluginSetting.timeFormat

    /** Steam大促时间输出时区，默认格式为：东八区 */
    var timeZone = SteamhelperPluginSetting.timeZone

    /** 上次报错的原因，需要检查是否为null */
    var exception: String? = null

    /** Steam商店状态，需要检查是否为null */
    var statusStore: String? = null

    /** Steam社区状态，需要检查是否为null */
    var statusCommunity: String? = null

    /** Steam大促标题，需要检查是否为null */
    var saleTitle: String? = null

    /** Steam大促时间戳，需要检查是否为null */
    var saleTimestamp: Long? = null

    /** Steam大促已经开始 */
    var isSaleStart = false

    /** SteamInfo是否初始化 */
    var isInit = false

    /**
     * 获取、刷新Steam状态与大促情况
     * @return Boolean 是否成功
     */
    fun refreshSteamInfo(): Boolean {
        return try {
            // 连接Keylol
            val get = SSLHelper().getDocument(url)
            // 获取Steam状态信息
            statusStore = get.getElementById("steam_monitor_store")?.text()
            statusCommunity = get.getElementById("steam_monitor_community")?.text()
            // 获取Steam大促名称（去除默认的...）
            saleTitle = get.getElementById("steam_monitor")?.getElementsByTag("a")?.get(2)?.text()?.replace("...", "")
            // 获取Steam大促时间
            var textDate = get.select("script:containsData(count_down_date)").html()
            isSaleStart = textDate.contains("正在进行中")
            textDate =
                textDate.substringAfter('"').substringBefore('"') + textDate.substringAfter('+').substringAfter('"')
                    .substringBefore('"')
            // 格式化Steam大促时间为时间戳（格式样例：2022-03-01 02:00:00.000+08:00）
            saleTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX").parse(textDate).time
            isInit = true
            true
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
            exception = e.toString()
            false
        }
    }

    /**
     * 返回Steam大促格式化后时间
     * @return String? Steam大促格式化后时间，需要检查是否为null
     */
    fun getSteamSaleFormattedTime(): String? {
        return if (saleTimestamp != null) {
            val dateFormat = SimpleDateFormat(timeFormat)
            dateFormat.timeZone = TimeZone.getTimeZone(timeZone)
            dateFormat.format(saleTimestamp)
        } else null
    }

    /**
     * 返回Steam大促格式化后相差时间
     * @return String? Steam大促格式化后相差时间，需要检查是否为null
     */
    fun getSteamSaleDiffTime(): String? {
        return if (saleTimestamp != null) {
            val dateNow = System.currentTimeMillis()
            var timeDiff: Long
            var timeText: String
            // 判断是否开始
            if (saleTimestamp!! > dateNow) {
                timeDiff = (saleTimestamp!! - dateNow) / 1000
                timeText = if (isSaleStart) "预计结束"
                else "还有"
            } else {
                timeDiff = (dateNow - saleTimestamp!!) / 1000
                timeText = "已进行"
            }
            // 计算时间
            var temp = timeDiff / (24 * 60 * 60)
            timeDiff -= temp * (24 * 60 * 60)
            timeText = timeText + temp.toString() + "天"
            temp = timeDiff / (60 * 60)
            timeDiff -= temp * (60 * 60)
            timeText = timeText + temp.toString() + "时"
            temp = timeDiff / (60)
            timeDiff -= temp * (60)
            timeText = timeText + temp.toString() + "分" + timeDiff.toString() + "秒"
            timeText
        } else null
    }
}