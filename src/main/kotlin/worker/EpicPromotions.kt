/**
 * EpicPromotions.kt
 * 用于编写Epic周免相关信息
 */

package com.evolvedghost.mirai.steamhelper.worker

import com.evolvedghost.mirai.steamhelper.utils.SSLHelper
import com.evolvedghost.mirai.steamhelper.utils.pluginExceptionHandler
import com.google.gson.JsonParser
import java.time.Instant

/**
 * Epic游戏数据储存类
 * @property title String 标题
 * @property description String 描述
 * @property effectiveDate Long 生效日期时间戳
 * @constructor
 */
class EpicGame(var title: String, var description: String, var effectiveDate: Long)

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

    /**
     * 刷新Epic限免项目
     * @return Boolean 是否成功
     */
    fun refresh(): Boolean {
        try {
            val json =
                JsonParser.parseString(SSLHelper().getBody("https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=zh-cn"))
                    .asJsonObject["data"].asJsonObject["Catalog"].asJsonObject["searchStore"].asJsonObject["elements"].asJsonArray
            current.clear()
            future.clear()
            for (element in json) {
                val elementDate = Instant.parse(element.asJsonObject["effectiveDate"].asString).epochSecond
                val nowDate = Instant.now().epochSecond
                // 判断是否有效
                if (kotlin.math.abs(elementDate - nowDate) < 604800) {
                    // 判断是否在限免中（避免周年庆每天一个的情况）
                    if (element.asJsonObject["price"].asJsonObject["totalPrice"].asJsonObject["discountPrice"].asInt == 0) {
                        current.add(
                            EpicGame(
                                element.asJsonObject["title"].asString,
                                element.asJsonObject["description"].asString,
                                elementDate
                            )
                        )
                    } else {
                        // 判断是否是未来限免
                        if (elementDate > nowDate) {
                            future.add(
                                EpicGame(
                                    element.asJsonObject["title"].asString,
                                    element.asJsonObject["description"].asString,
                                    elementDate
                                )
                            )
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