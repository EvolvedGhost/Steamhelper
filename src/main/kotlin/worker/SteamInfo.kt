package com.evolvedghost.mirai.steamhelper.worker

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.evolvedghost.mirai.steamhelper.utils.SSLHelper
import com.evolvedghost.mirai.steamhelper.utils.pluginExceptionHandler
import java.text.SimpleDateFormat

/**
 * Steam相关信息
 * 包含Steam状态与大促情况
 * 默认获取源来自Keylol（其乐）
 */
class SteamInfo {
    /** Steam状态与大促情况获取源 */
    var url = SteamhelperPluginSetting.urlInfo

    /** 上次报错的原因 */
    var exception = String()

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
            isSaleStart = saleTitle?.contains("正在进行中") ?: false
            if (saleTitle?.contains("促销已经结束") == true) {
                saleTimestamp = -1
            } else {
                textDate =
                    textDate.substringAfter('"').substringBefore('"') + textDate.substringAfter('+').substringAfter('"')
                        .substringBefore('"')
                // 格式化Steam大促时间为时间戳（格式样例：2022-03-01 02:00:00.000+08:00）
                saleTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX").parse(textDate).time
            }
            isInit = true
            true
        } catch (e: Exception) {
            pluginExceptionHandler("Steam大促获取", e)
            exception = e.toString()
            false
        }
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
            } else if (saleTimestamp!! < 0) {
                return "已结束"
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