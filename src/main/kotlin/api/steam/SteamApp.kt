package com.github.evolvedghost.mirai.steamhelper.api.steam

import com.github.evolvedghost.mirai.steamhelper.utils.Logger
import com.github.evolvedghost.mirai.steamhelper.utils.NetworkUtil
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SteamApp {
    data class SteamAppListElement(
        val id: Long,
        val type: String,
        val title: String,
        val review: String,
        val price: String,
        val release: String,
        val image: String,
    )

    fun findByName(keyword: String, region: String): List<SteamAppListElement> {
        val encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
        val url =
            "https://store.steampowered.com/search/?ignore_preferences=1&l=schinese&lang=zh-cn&cc=$region&term=$encodedKeyword"
        val doc = runCatching {
            NetworkUtil.document(url, "SteamApp关键词搜索")
        }.getOrElse { return emptyList() }
        val resultsContainer = doc.getElementById("search_resultsRows") ?: return emptyList()
        val gameElements = resultsContainer.children()

        return gameElements.mapNotNull { element ->
            try {
                // 提取onmouseover属性并解析type和id
                val onMouseOver = element.attr("onmouseover")
                val jsonPart = onMouseOver.substringAfter("{").substringBeforeLast("}")
                val type = jsonPart.substringAfter("\"type\":\"").substringBefore("\"")
                val id = jsonPart.substringAfter("\"id\":").substringBefore(",").toLong()

                // 提取其他信息
                val title = element.select(".title").first()?.text() ?: "无"
                val release = element.select(".search_released").first()?.text() ?: "无"
                val price = element.select(".discount_final_price").first()?.text() ?: "无"
                val review = element.select(".search_review_summary").first()?.attr("data-tooltip-html")
                    ?.substringBefore("<br>") ?: "无"
                val image = element.select(".search_capsule img").first()?.attr("src") ?: ""

                SteamAppListElement(id, type, title, review, price, release, image)
            } catch (e: Exception) {
                Logger.debug("SteamApp关键词搜索", e, "元素解析失败")
                null
            }
        }
    }
}