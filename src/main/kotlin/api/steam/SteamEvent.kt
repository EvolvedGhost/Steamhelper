package com.github.evolvedghost.mirai.steamhelper.api.steam

import com.github.evolvedghost.mirai.steamhelper.utils.Logger
import com.github.evolvedghost.mirai.steamhelper.utils.NetworkUtil
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SteamEvent {
    data class Event(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val title: String
    )

    private fun completeDateString(dateStr: String, referenceDate: LocalDate): String {
        return when {
            "年" in dateStr -> dateStr
            "月" in dateStr -> "${referenceDate.year}年$dateStr"
            else -> "${referenceDate.year}年${referenceDate.monthValue}月$dateStr"
        }
    }

    fun fetch(): List<Event> {
        val doc = runCatching {
            NetworkUtil.document(
                "https://partner.steamgames.com/doc/marketing/upcoming_events?l=schinese",
                "SteamEvent活动检索"
            )
        }.getOrElse { return emptyList() }


        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
        val events = mutableListOf<Event>()

        /**
         * 处理重大活动
         */
        val subEventsYear = mutableListOf<Int>()
        val sections = doc.select(".bb_subsection")
        for (section in sections) {
            val text = section.text().replace(" ", "")
            try {
                if ("|" in text) {
                    val parts = text.split("|").map { it.trim() }
                    if (parts.size >= 2) {
                        val title = parts[0].trim()
                        val datePart = parts[1].substringBefore("（").trim()
                        val dates = datePart.split("-").map { it.trim() }
                        if (dates.size == 2) {
                            val startDate = LocalDate.parse(dates[0], formatter)
                            val endDateStr = completeDateString(dates[1], startDate)
                            val endDate = LocalDate.parse(endDateStr, formatter)
                            events.add(Event(startDate, endDate, title))
                        }
                    }
                } else if ("各游戏节" in text) {
                    subEventsYear.add(text.substringBefore("年各游戏节").replace(" ", "").toInt())
                }
            } catch (e: Exception) {
                Logger.debug("SteamEvent主要活动检索", e, "元素解析失败: $text")
            }
        }

        /**
         * 处理次要活动
         */
        val tables = doc.select("table")
        var tableIndex = -1
        for (table in tables) {
            tableIndex++

            val rows = table.select("tr")

            for (row in rows.drop(1)) {
                try {
                    val cols = row.select("td")
                    if (cols.size < 2) continue

                    val dateText = cols[0].html().replace(" ", "").replace("<br>", "(br)").replace("<br/>", "(br)")
                    val dateTexts = Jsoup.parse(dateText).text().substringBefore("（").split("(br)").map { it.trim() }
                    if (dateTexts.size < 2) continue

                    val title = cols[1].text().trim()

                    var yearIdx = tableIndex;
                    if (yearIdx > subEventsYear.size) yearIdx = subEventsYear.size - 1

                    var startDateStr = dateTexts[0]
                    if (!startDateStr.contains("年")) {
                        startDateStr = "${subEventsYear[yearIdx]}年${startDateStr}"
                    }
                    val startDate = LocalDate.parse(startDateStr, formatter)

                    val endDateStr = completeDateString(dateTexts[1], startDate)
                    val endDate = LocalDate.parse(endDateStr, formatter)

                    val finalEndDate = if (startDate.isAfter(endDate)) endDate.plusYears(1) else endDate
                    events.add(Event(startDate, finalEndDate, title))
                } catch (e: Exception) {
                    Logger.debug("SteamEvent次要活动检索", e, "元素解析失败${row.text()}")
                }
            }
        }

        events.sortBy { it.startDate }
        return events
    }
}