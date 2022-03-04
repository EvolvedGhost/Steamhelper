/**
 * 用于快速生成area4currency表
 */
import com.google.gson.JsonParser
import org.jsoup.Jsoup

fun main() {
    val url =
        "https://pkgstore.datahub.io/core/country-codes/country-codes_json/data/616b1fb83cbfd4eb6d9e7d52924bb00a/country-codes_json.json"
    val jsoup = Jsoup.connect(url)
    val json = JsonParser.parseString(jsoup.ignoreContentType(true).execute().body()).asJsonArray
    var i = 0
    while (i < json.size()) {
        val sb = StringBuilder()
        sb.append('"')
        if (!json[i].asJsonObject["ISO3166-1-Alpha-2"].isJsonNull) sb.append(json[i].asJsonObject["ISO3166-1-Alpha-2"].asString)
        else sb.append("null")
        sb.append("\" to \"")
        if (!json[i].asJsonObject["ISO4217-currency_alphabetic_code"].isJsonNull)
            if (json[i].asJsonObject["ISO4217-currency_alphabetic_code"].asString.contains(","))
                sb.append(json[i].asJsonObject["ISO4217-currency_alphabetic_code"].asString.substringBefore(","))
            else sb.append(json[i].asJsonObject["ISO4217-currency_alphabetic_code"].asString)
        else sb.append("null")
        sb.append("\", // ")
        if (!json[i].asJsonObject["official_name_cn"].isJsonNull) sb.append(json[i].asJsonObject["official_name_cn"].asString)
        else sb.append("null")
        println(sb.toString())
        i++
    }
}