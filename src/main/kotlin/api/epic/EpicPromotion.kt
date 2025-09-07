package com.github.evolvedghost.mirai.steamhelper.api.epic

import com.github.evolvedghost.mirai.steamhelper.utils.Logger
import com.github.evolvedghost.mirai.steamhelper.utils.NetworkUtil
import kotlinx.serialization.Serializable
import java.time.Instant

class EpicPromotion {
    private val current = mutableListOf<EpicGame>()
    private val future = mutableListOf<EpicGame>()

    @Serializable
    data class EpicGame(
        val title: String,
        val description: String,
        val link: String,
        val image: String,
        val effectiveDate: Long
    )

    @Serializable
    data class EpicPromotionData(val data: Data)

    @Serializable
    data class Data(val Catalog: Catalog)

    @Serializable
    data class Catalog(val searchStore: SearchStore)

    @Serializable
    data class SearchStore(val elements: List<Element>)

    @Serializable
    data class Element(
        val title: String,
        val description: String,
        val effectiveDate: String,
        val productSlug: String? = null,
        val catalogNs: CatalogNs? = null,
        val price: Price,
        val promotions: Promotions? = null,
        val keyImages: List<KeyImage>,
    )

    @Serializable
    data class KeyImage(val type: String, val url: String)

    @Serializable
    data class CatalogNs(val mappings: List<Mapping>? = null)

    @Serializable
    data class Mapping(val pageSlug: String)

    @Serializable
    data class Price(val totalPrice: TotalPrice)

    @Serializable
    data class TotalPrice(val discountPrice: Int)

    @Serializable
    data class Promotions(
        val promotionalOffers: List<PromotionalOfferGroup>? = null,
        val upcomingPromotionalOffers: List<PromotionalOfferGroup>? = null
    )

    @Serializable
    data class PromotionalOfferGroup(val promotionalOffers: List<PromotionalOffer>)

    @Serializable
    data class PromotionalOffer(val startDate: String, val endDate: String)

    fun update(): Boolean {
        return try {
            val data = NetworkUtil.json<EpicPromotionData>(
                "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=zh-cn"
            )
            val elements = data.data.Catalog.searchStore.elements

            val newCurrent = mutableListOf<EpicGame>()
            val newFuture = mutableListOf<EpicGame>()
            val now = Instant.now().epochSecond

            for (element in elements) {
                val effectiveDate = Instant.parse(element.effectiveDate).epochSecond
                val link = buildLink(element)
                val imgLink = element.keyImages.firstOrNull()?.url ?: ""

                // 如果在一周内，按时间判断是否为当前或未来的促销
                if (kotlin.math.abs(effectiveDate - now) < 604800) {
                    when {
                        effectiveDate > now -> {
                            newFuture.add(EpicGame(element.title, element.description, link, imgLink, effectiveDate))
                        }

                        element.price.totalPrice.discountPrice == 0 -> {
                            newCurrent.add(EpicGame(element.title, element.description, link, imgLink, effectiveDate))
                        }
                    }
                } else {
                    // 否则从促销信息中提取
                    val promotions = element.promotions ?: continue
                    val offers = promotions.promotionalOffers ?: promotions.upcomingPromotionalOffers ?: continue
                    if (offers.isEmpty()) continue

                    val offer = offers.firstOrNull()?.promotionalOffers?.firstOrNull() ?: continue
                    val start = Instant.parse(offer.startDate).epochSecond
                    val end = Instant.parse(offer.endDate).epochSecond

                    when {
                        now in start..end && element.price.totalPrice.discountPrice == 0 -> {
                            newCurrent.add(EpicGame(element.title, element.description, link, imgLink, start))
                        }

                        now < start -> {
                            newFuture.add(EpicGame(element.title, element.description, link, imgLink, start))
                        }
                    }
                }
            }

            // 只有在成功解析后才更新数据
            current.clear()
            current.addAll(newCurrent)
            future.clear()
            future.addAll(newFuture)

            true
        } catch (e: Exception) {
            Logger.warn("Epic促销获取", e)
            false
        }
    }

    fun getFutureList(): List<EpicGame> = future.toList()
    fun getCurrentList(): List<EpicGame> = current.toList()
    fun hasData(): Boolean = current.isNotEmpty() || future.isNotEmpty()

    private fun buildLink(element: Element): String {
        val slug = element.productSlug?.takeIf { it.isNotBlank() && it != "[]" }
            ?: element.catalogNs?.mappings?.firstOrNull()?.pageSlug

        return slug?.let { "https://store.epicgames.com/p/$it" } ?: "未知"
    }
}