package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * 汇率类
 * 用于进行汇率计算
 */
class Exchange {
    private var currencyMap: HashMap<String, Double>? = null

    private var timestamp: Long = 0

    private var mapArea = String()

    var exception = String()

    /**
     * 刷新汇率
     * @param area String 区域
     * @return Boolean 是否成功
     */
    fun refresh(area: String): Boolean {
        return if (System.currentTimeMillis() < timestamp && currencyMap != null && mapArea == area) {
            true
        } else {
            exception = String()
            try {
                val json =
                    JsonParser.parseString(SSLHelper().getBody("https://open.er-api.com/v6/latest/$area")).asJsonObject
                timestamp = json["time_next_update_unix"].asLong * 1000
                currencyMap = Gson().fromJson(json["rates"], HashMap::class.java) as HashMap<String, Double>
                mapArea = area
                true
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                exception = e.toString()
                false
            }
        }
    }

    /**
     * 计算汇率
     * @param price Int 价格
     * @param currency String? 币种
     * @return String? 换算后价格
     */
    fun calc(price: Int, currency: String?): String? {
        return if (currencyMap != null && currency != null) {
            if (currencyMap!![currency] != null) {
                String.format("%.2f", (price / currencyMap!![currency]!! / 100))
            } else {
                null
            }
        } else null
    }

    /**
     * 计算汇率但不排版
     * @param price Int 价格
     * @param currency String? 币种
     * @return Double? 换算后价格
     */
    fun calcNoFormat(price: Int, currency: String?): Double? {
        return if (currencyMap != null && currency != null) {
            if (currencyMap!![currency] != null) {
                price / currencyMap!![currency]!!
            } else {
                null
            }
        } else null
    }
}