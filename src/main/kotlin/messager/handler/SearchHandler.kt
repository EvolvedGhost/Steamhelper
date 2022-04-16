/**
 * SearchHandler.kt
 * 用于搜索
 */

package com.evolvedghost.mirai.steamhelper.messager.handler

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.evolvedghost.mirai.steamhelper.worker.SteamApp

fun setSearch(search: SteamApp, AppNameOrAppid: Array<out String>): Int {
    // 判断是否为纯数字
    return if (AppNameOrAppid.size == 1 && AppNameOrAppid[0].matches("^[0-9]*$".toRegex())) {
        search.appid = AppNameOrAppid[0]
        1
    } else {
        val keyword = AppNameOrAppid.joinToString(" ")
        when (search.search(SteamhelperPluginSetting.areasSearch.toTypedArray(), keyword)) {
            1 -> 1
            0 -> 0
            else -> -1
        }
    }
}
