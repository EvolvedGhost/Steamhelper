/**
 * EpicPromotions.kt
 * 用于编写Epic周免相关信息
 */

package com.evolvedghost.mirai.steamhelper.worker

import com.evolvedghost.mirai.steamhelper.utils.SSLHelper
import com.evolvedghost.mirai.steamhelper.utils.pluginExceptionHandler
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.time.Instant

/**
 * Epic游戏数据储存类
 * @property title String 标题
 * @property description String 描述
 * @property effectiveDate Long 生效日期时间戳
 * @constructor
 */
class EpicGame(var title: String, var description: String, var link: String, var effectiveDate: Long)

/**
 * Epic限免类
 */
class EpicPromotions {
    /** 当前限免中的 */
    val current = mutableListOf<EpicGame>()

    /** 未来将限免的 */
    val future = mutableListOf<EpicGame>()

    /** 错误信息 */
    var exception = String()

    /** 添加限免名单 */
    private fun addElement(element: JsonElement, startTime: Long, isToCurrent: Boolean) {
        val mappings =
            if (!element.asJsonObject["productSlug"].isJsonNull && element.asJsonObject["productSlug"].asString != "[]") {
                element.asJsonObject["productSlug"].asString
            } else if (!element.asJsonObject["catalogNs"].asJsonObject["mappings"].isJsonNull &&
                element.asJsonObject["catalogNs"].asJsonObject["mappings"].toString().isNotEmpty()
            ) {
                element.asJsonObject["catalogNs"].asJsonObject["mappings"].asJsonArray.get(0).asJsonObject["pageSlug"].asString
            } else {
                null
            }
        val link = if (mappings == null) "未知"
        else "https://store.epicgames.com/p/$mappings"
        val newPromotion = EpicGame(
            element.asJsonObject["title"].asString,
            element.asJsonObject["description"].asString,
            link,
            startTime
        )
        if (isToCurrent) {
            current.add(newPromotion)
        } else {
            future.add(newPromotion)
        }
    }

    /**
     * 刷新Epic限免项目
     * @return Boolean 是否成功
     */
    fun refresh(): Boolean {
        try {
            val json =
                JsonParser.parseString(SSLHelper().getBody("https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=zh-cn"))
                    .asJsonObject["data"].asJsonObject["Catalog"]
                    .asJsonObject["searchStore"].asJsonObject["elements"].asJsonArray
            current.clear()
            future.clear()
            for (element in json) {
                val elementDate = Instant.parse(element.asJsonObject["effectiveDate"].asString).epochSecond
                val nowDate = Instant.now().epochSecond
                // 判断是否有效
                if (kotlin.math.abs(elementDate - nowDate) < 604800) {
                    if (elementDate > nowDate) {
                        addElement(element, elementDate, false)
                    } else if (element.asJsonObject["price"].asJsonObject["totalPrice"].asJsonObject["discountPrice"].asInt == 0) {
                        addElement(element, elementDate, true)
                    }
                } else {
                    // 特殊限免活动时，API会将其放置到最后
                    // 当一个游戏曾经限免过，前面if判断将失效
                    if (!element.asJsonObject["promotions"].isJsonNull) {
                        var promotionalOffers = element.asJsonObject["promotions"].asJsonObject.get("promotionalOffers")
                        if (promotionalOffers.asJsonArray.isEmpty) {
                            promotionalOffers =
                                element.asJsonObject["promotions"].asJsonObject.get("upcomingPromotionalOffers")
                        }
                        if (!promotionalOffers.asJsonArray.isEmpty) {
                            promotionalOffers =
                                promotionalOffers.asJsonArray[0].asJsonObject["promotionalOffers"].asJsonArray[0]
                            val startDate =
                                Instant.parse(promotionalOffers.asJsonObject["startDate"].asString).epochSecond
                            val endDate = Instant.parse(promotionalOffers.asJsonObject["endDate"].asString).epochSecond
                            if (nowDate in startDate..endDate &&
                                element.asJsonObject["price"].asJsonObject["totalPrice"].asJsonObject["discountPrice"].asInt == 0
                            ) {
                                addElement(element, startDate, true)
                            } else if (nowDate < startDate) {
                                addElement(element, startDate, false)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            pluginExceptionHandler("Epic促销", e)
            exception = e.toString()
            return false
        }
    }
}