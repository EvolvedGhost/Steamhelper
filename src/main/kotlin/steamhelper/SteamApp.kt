package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.google.gson.JsonParser
import java.net.URLEncoder

/**
 * SteamApp相关操作
 */
class SteamApp() {
    /** 上次报错的原因，需要检查是否为null */
    var exception: String? = null

    /** SteamAppid，需要检查是否为null */
    var appid: String? = null

    /** SteamApp名称，需要检查是否为null */
    var appName: String? = null

    /** SteamApp介绍，需要检查是否为null */
    var appDescription: String? = null

    /** SteamApp介绍，需要检查是否为null */
    var appCurrency: String? = null

    /**
     *  SteamApp价格
     *  Int数组结构为（初始价格,最终价格,折扣）[0免费-1此区不存在-2出现错误-3未初始化]
     */
    var appPrice = arrayOf(-3, -3, -3)

    /**
     *  SteamApp过去价格
     *  Int数组结构为（初始价格,最终价格,折扣）[0免费-1此区不存在-2出现错误-3未初始化]
     */
    var oldPrice = arrayOf(-3, -3, -3)

    /**
     * 次级构造函数，可直接填入appid
     * @param id String Steam Appid
     * @constructor
     */
    constructor(id: String) : this() {
        this.appid = id
    }

    /**
     * Steam搜索，搜索结果返回到类中appid
     * @param areas Array<String> SteamApp查询区域
     * @param keyword String 搜索关键词
     * @return Int 搜索状态（0=无结果，1=成功，-1=失败）
     */
    fun search(areas: Array<String>, keyword: String): Int {
        exception = String()
        var counter = 0
        var localAppid: String?
        for (area in areas) {
            try {
                val url =
                    "https://store.steampowered.com/search/?term=" + URLEncoder.encode(keyword, "utf-8") + "&cc=$area"
                val get = SSLHelper().getDocument(url)
                localAppid =
                    get.getElementById("search_resultsRows")?.getElementsByTag("a")?.first()?.attr("data-ds-appid")
                if (!localAppid.isNullOrEmpty()) {
                    appid = localAppid
                    return 1
                }
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                exception += e.toString() + "\n"
                counter++
            }
        }
        return if (counter == areas.size) -1
        else 0
    }

    /**
     * 获取并刷新此SteamApp的相关信息
     * @param areas Array<String> SteamApp查询区域
     * @return Int 刷新状态（0=无结果，1=成功，-1=失败，-2=appid为null程序逻辑存在错误）
     */
    fun refreshInfo(areas: Array<String>): Int {
        if (!appid.isNullOrEmpty()) {
            exception = String()
            var counter = 0
            for (area in areas) {
                try {
                    val url = "https://store.steampowered.com/api/appdetails?appids=$appid&cc=$area"
                    val json = JsonParser.parseString(SSLHelper().getBody(url)).asJsonObject
                    if (json[appid].asJsonObject["success"].asBoolean) {
                        appName = json[appid].asJsonObject["data"].asJsonObject["name"].asString
                        appDescription = json[appid].asJsonObject["data"].asJsonObject["short_description"].asString
                        return 1
                    } else {
                        continue
                    }
                } catch (e: Exception) {
                    if (SteamhelperPluginSetting.debug) e.printStackTrace()
                    exception += e.toString() + "\n"
                    counter++
                }
            }
            return if (counter == areas.size) -1
            else 0
        } else return -2
    }

    /**
     * 获取并刷新此SteamApp的价格信息
     * @param area String SteamApp价格区域
     * @return Int 刷新状态（2=成功且有新的折扣，1=成功，0=成功且有折扣结束，-1=失败（可能锁区)，-2=出现错误，-3=appid为null程序逻辑存在错误）
     */
    fun refreshPriceAndInfo(area: String): Int {
        oldPrice = appPrice
        if (!appid.isNullOrEmpty()) {
            exception = String()
            // 提取json字段中的价格
            try {
                val url = "https://store.steampowered.com/api/appdetails?appids=$appid&cc=$area"
                val json = JsonParser.parseString(SSLHelper().getBody(url)).asJsonObject
                appPrice = if (json[appid].asJsonObject["success"].asBoolean) {
                    appName = json[appid].asJsonObject["data"].asJsonObject["name"].asString
                    appDescription = json[appid].asJsonObject["data"].asJsonObject["short_description"].asString
                    if (json[appid].asJsonObject["data"].asJsonObject["price_overview"] != null) {
                        appCurrency =
                            json[appid].asJsonObject["data"].asJsonObject["price_overview"].asJsonObject["currency"].asString
                        arrayOf(
                            json[appid].asJsonObject["data"].asJsonObject["price_overview"].asJsonObject["initial"].asInt,
                            json[appid].asJsonObject["data"].asJsonObject["price_overview"].asJsonObject["final"].asInt,
                            json[appid].asJsonObject["data"].asJsonObject["price_overview"].asJsonObject["discount_percent"].asInt
                        )
                    } else {
                        appCurrency = area4currency[area]
                        arrayOf(0, 0, 0)
                    }
                } else {
                    appCurrency = area4currency[area]
                    appPrice=arrayOf(-1, -1, -1)
                    return -1
                }
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                exception += e.toString() + "\n"
                appCurrency = area4currency[area]
                appPrice = arrayOf(-2, -2, -2)
                return -2
            }
            // 对比价格
            return if (oldPrice[0] < 0) 1
            else if (oldPrice[1] > appPrice[1]) 2
            else if (oldPrice[1] < appPrice[1]) 0
            else 1
        } else return -3
    }
}