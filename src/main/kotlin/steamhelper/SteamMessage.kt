/**
 * SteamMessage.kt
 * 用于编写插件最终发出的信息的整理与格式化
 */

package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.*
import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import net.mamoe.mirai.utils.info

/**
 * 用于替换文本串
 * @param text String 要被替换的文本
 * @param keywords Array<String> 替换关键字
 * @param targets Array<String?> 替换结果
 * @return String
 */
fun replace(text: String, keywords: Array<String>, targets: Array<String?>): String {
    return if (keywords.size == targets.size) {
        var finalText = text
        for (i in keywords.indices) {
            finalText = finalText.replace(keywords[i], targets[i].toString())
        }
        finalText
    } else "错误"
}

/** 周榜信息替换关键字 */
val keywordsWeek = arrayOf("<tf>", "<ts>", "<ls>", "<sc>")

/** 周榜信息榜单替换关键字 */
val keywordsWeekList = arrayOf("<nm>", "<lk>")

/** 获取周榜 */
fun getWeek(): String {
    return if (steamWeek.isInit) {
        // 复制steamWeek
        lockSteamWeek.lock()
        val tmp = steamWeek
        if (lockSteamWeek.isHeldByCurrentThread) lockSteamWeek.unlock()
        // 处理messageWeekList
        val messageList = StringBuilder()
        for (i in tmp.titleArr.indices) {
            messageList.append(
                replace(
                    SteamhelperPluginSetting.messageWeekList, keywordsWeekList, arrayOf(
                        tmp.titleArr[i], tmp.linkArr[i]
                    )
                ) + "\n"
            )
        }
        // 处理messageWeek并返回
        replace(
            SteamhelperPluginSetting.messageWeek, keywordsWeek, arrayOf(
                tmp.getSteamWeeklyFormattedTime(), tmp.timestamp.toString(), messageList.toString(), tmp.url
            )
        )
    } else {
        "周榜信息暂未初始化请稍后，如长期出现此信息请检查日志和机器人网络状况"
    }
}

/** 状态信息替换关键字 */
val keywordsStat = arrayOf("<ss>", "<sc>")

/** 获取状态 */
fun getStat(): String {
    return if (steamInfo.isInit) {
        // 复制steamInfo
        lockSteamInfo.lock()
        val tmp = steamInfo
        if (lockSteamInfo.isHeldByCurrentThread) lockSteamInfo.unlock()
        // 处理messageStatus
        replace(
            SteamhelperPluginSetting.messageStatus, keywordsStat, arrayOf(tmp.statusStore, tmp.statusCommunity)
        )
    } else {
        "相关信息暂未初始化请稍后，如长期出现此信息请检查日志和机器人网络状况"
    }
}

/** 促销信息替换关键字 */
val keywordsSale = arrayOf("<nm>", "<tl>", "<tf>", "<ts>")

/** 获取促销 */
fun getSale(): String {
    return if (steamInfo.isInit) {
        // 复制steamInfo
        lockSteamInfo.lock()
        val tmp = steamInfo
        if (lockSteamInfo.isHeldByCurrentThread) lockSteamInfo.unlock()
        // 处理messageSale
        replace(
            SteamhelperPluginSetting.messageSale, keywordsSale, arrayOf(
                tmp.saleTitle, tmp.getSteamSaleDiffTime(), tmp.getSteamSaleFormattedTime(), tmp.saleTimestamp.toString()
            )
        )
    } else {
        "相关信息暂未初始化请稍后，如长期出现此信息请检查日志和机器人网络状况"
    }
}

/** 比价信息替换关键字 */
val keywordsCompare = arrayOf("<id>", "<nm>", "<ds>", "<pl>")

/** 比价信息榜单替换关键字 */
val keywordsCompareList = arrayOf("<an>", "<at>", "<cr>", "<ct>", "<ip>", "<it>", "<fp>", "<ft>", "<ds>")

/** 比价信息榜单错误替换关键字 */
val keywordsCompareListError = arrayOf("<if>", "<an>", "<at>", "<cr>", "<ct>")

/** 获取比价 */
suspend fun getCompare(AppNameOrAppid: Array<out String>): String {
    val search = SteamApp()
    // 判断是否为纯数字
    if (AppNameOrAppid.size == 1 && AppNameOrAppid[0].matches("^[0-9]*$".toRegex())) {
        search.appid = AppNameOrAppid[0]
    } else {
        val keyword = AppNameOrAppid.joinToString(" ")
        when (search.search(SteamhelperPluginSetting.areasSearch.toTypedArray(), keyword)) {
            1 -> {
                //成功
            }
            0 -> {
                return "未找到相应的App"
            }
            else -> {
                Steamhelper.logger.info { "SteamHelper在搜索的时候得到了一个错误：" }
                Steamhelper.logger.info { search.exception }
                return "搜索发生错误，如长期出现此信息请检查日志和机器人网络状况"
            }
        }
    }
    if (search.appid == null) {
        return "比价失败，如长期出现此信息请检查日志和机器人网络状况"
    }
    // 获取价格，创建所需要区域数量的SteamApp来进行
    val arr = Array(SteamhelperPluginSetting.areasPrice.size) { SteamApp(search.appid!!) }
    // 判断是否完成搜索
    val arrBool = Array(SteamhelperPluginSetting.areasPrice.size) { false }
    // 开启多线程以便于快速遍历全部区域的价格
    for (i in SteamhelperPluginSetting.areasPrice.indices) {
        Thread {
            arr[i].refreshPriceAndInfo(SteamhelperPluginSetting.areasPrice[i])
            arrBool[i] = true
        }.start()
    }
    var timer = 0
    var count = 0
    while (true) {
        if (!arrBool[count]) {
            delay(100)
            timer++
            // 防止线程崩了半天跳不出来
            if (timer * 100 > SteamhelperPluginSetting.timeout * SteamhelperPluginSetting.retry) break
            else continue
        } else {
            count++
            // 意为全部完成价格获取
            if (count >= SteamhelperPluginSetting.areasPrice.size) break
        }
    }
    // 检查是否全部失败，直接返回避免污染信息，同时寻找一个成功获取info的数组成员
    count = 0
    var success = 0
    for (i in SteamhelperPluginSetting.areasPrice.indices) {
        if (arr[i].appPrice[0] < -1) count++
        else {
            success = i
            break
        }
    }
    if (count == SteamhelperPluginSetting.areasPrice.size) {
        return "比价失败，如长期出现此信息请检查日志和机器人网络状况"
    }
    // 文本替换
    val messageList = StringBuilder()
    for (i in SteamhelperPluginSetting.areasPrice.indices) {
        // 此处处理messageCompareListError
        if (arr[i].appPrice[0] <= 0) {
            val tmp = if (arr[i].appPrice[0] == -3) "内部错误"
            else if (arr[i].appPrice[0] == -2) "获取错误"
            else if (arr[i].appPrice[0] == -1) "锁区"
            else "免费"
            messageList.append(
                replace(
                    SteamhelperPluginSetting.messageCompareListError, keywordsCompareListError, arrayOf(
                        tmp,
                        SteamhelperPluginSetting.areasPrice[i],
                        SteamhelperPluginSetting.areasPrice[0],
                        arr[i].appCurrency,
                        arr[0].appCurrency
                    )
                ) + "\n"
            )
        } // 此处处理messageCompareList
        else {
            messageList.append(
                replace(
                    SteamhelperPluginSetting.messageCompareList, keywordsCompareList, arrayOf(
                        SteamhelperPluginSetting.areasPrice[i],
                        SteamhelperPluginSetting.areasPrice[0],
                        arr[i].appCurrency,
                        arr[0].appCurrency,
                        String.format("%.2f", (arr[i].appPrice[0] / 100).toDouble()),
                        exchange.calc(arr[i].appPrice[0], arr[i].appCurrency),
                        String.format("%.2f", (arr[i].appPrice[1] / 100).toDouble()),
                        exchange.calc(arr[i].appPrice[1], arr[i].appCurrency),
                        arr[i].appPrice[2].toString()
                    )
                ) + "\n"
            )
        }
    }
    // 此处处理messageCompare
    return replace(
        SteamhelperPluginSetting.messageCompare, keywordsCompare, arrayOf(
            arr[success].appid, arr[success].appName, arr[success].appDescription, messageList.toString()
        )
    )

}

/** 订阅信息替换关键字 */
val keywordsSubscribe = arrayOf(
    "<aid>", "<anm>", "<ads>", "<aif>", "<aar>", "<acr>", "<cip>", "<cfp>", "<cds>", "<oip>", "<ofp>", "<ods>"
)

/** 获取订阅 */
fun getSubscribe(app: SteamApp, flag: Int): String {
    val tmp = when (flag) {
        0 -> {
            "涨价"
        }
        2 -> {
            "降价"
        }
        else -> {
            return "内部错误"
        }
    }
    return replace(
        SteamhelperPluginSetting.messageSubscribe, keywordsSubscribe, arrayOf(
            app.appid,
            app.appName,
            app.appDescription,
            tmp,
            SteamhelperPluginSetting.areasPrice[0],
            app.appCurrency,
            String.format("%.2f", (app.appPrice[0] / 100).toDouble()),
            String.format("%.2f", (app.appPrice[1] / 100).toDouble()),
            app.appPrice[2].toString(),
            String.format("%.2f", (app.oldPrice[0] / 100).toDouble()),
            String.format("%.2f", (app.oldPrice[1] / 100).toDouble()),
            app.oldPrice[2].toString()
        )
    )
}

/**
 * 订阅操作
 * @param flag Boolean true=新增，false=删除
 * @param cs CommandSender
 * @param AppNameOrAppid Array<out String>
 */
@OptIn(ConsoleExperimentalApi::class)
suspend fun getSubscribe(flag: Boolean, cs: CommandSender, AppNameOrAppid: Array<out String>) {
    if (cs is ConsoleCommandSender) {
        ConsoleCommandSender.sendMessage("暂不支持控制台订阅")
    } else {
        if (cs.bot?.id != null && cs.subject?.id != null) {
            val botID = cs.bot?.id!!
            val contactID = cs.subject?.id!!
            if (flag) {
                try {
                    val contact = Bot.getInstance(botID).getContact(contactID, true)
                    // 进行一次试发送，如失败则不允许订阅
                    contact.sendMessage("订阅中……")
                } catch (e: Exception) {
                    if (SteamhelperPluginSetting.debug) e.printStackTrace()
                    cs.sendMessage("订阅失败，机器人无法给您发送信息，错误原因：\n$e")
                }
            }
            val appid: Int
            val search = SteamApp()
            // 判断是否为纯数字
            if (AppNameOrAppid.size == 1 && AppNameOrAppid[0].matches("^[0-9]*$".toRegex())) {
                search.appid = AppNameOrAppid[0]
                appid = AppNameOrAppid[0].toInt()
            } else {
                val keyword = AppNameOrAppid.joinToString(" ")
                when (search.search(arrayOf(SteamhelperPluginSetting.areasPrice[0]), keyword)) {
                    1 -> {
                        //成功
                        appid = search.appid!!.toInt()
                    }
                    0 -> {
                        cs.sendMessage("未找到相应的App")
                        return
                    }
                    else -> {
                        Steamhelper.logger.info { "SteamHelper在搜索的时候得到了一个错误：" }
                        Steamhelper.logger.info { search.exception }
                        cs.sendMessage("搜索发生错误，如长期出现此信息请检查日志和机器人网络状况")
                        return
                    }
                }
            }
            val result = search.refreshPriceAndInfo(SteamhelperPluginSetting.areasPrice[0])
            if (flag) {
                // 此处开始写新增订阅
                if (result == -1) {
                    cs.sendMessage(search.appName + '(' + search.appid + ")游戏锁区，无法订阅")
                    return
                } else if (result == -2 || result == -3) {
                    cs.sendMessage("获取失败，无法订阅")
                    return
                } else if (search.appPrice[0] < 0) {
                    cs.sendMessage("游戏锁区或者获取错误，无法订阅")
                    return
                }
                // 如果库中无此游戏则添加
                lockSubscribeMap.lock()
                lockMapSub.lock()
                if (!mapSub.contains(appid)) {
                    mapSub[appid] = search
                }
                if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
                // 对subscribeMap中进行添加
                try {
                    if (!SteamhelperPluginData.subscribeMap.contains(appid)) {
                        SteamhelperPluginData.subscribeMap[appid] = mutableMapOf(botID to mutableMapOf(contactID to 0))
                    } else if (!SteamhelperPluginData.subscribeMap[appid]!!.contains(botID)) {
                        SteamhelperPluginData.subscribeMap[appid]!![botID] = mutableMapOf(contactID to 0)
                    } else if (!SteamhelperPluginData.subscribeMap[appid]!![botID]!!.contains(contactID)) {
                        SteamhelperPluginData.subscribeMap[appid]!![botID]!![contactID] = 0
                    } else {
                        if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
                        cs.sendMessage(search.appName + '(' + search.appid + ")您已订阅此游戏")
                        return
                    }
                } catch (e: Exception) {
                    if (SteamhelperPluginSetting.debug) e.printStackTrace()
                }
                if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
                cs.sendMessage(search.appName + '(' + search.appid + ')' + "订阅成功")
            } else {
                // 此处开始写取消订阅
                lockSubscribeMap.lock()
                try {
                    if (SteamhelperPluginData.subscribeMap[appid]?.get(botID)?.get(contactID) != null) {
                        SteamhelperPluginData.subscribeMap[appid]?.get(botID)?.remove(contactID)
                        cs.sendMessage(search.appName + '(' + search.appid + ")已取消订阅")
                    } else {
                        cs.sendMessage(search.appName + '(' + search.appid + ")您没有订阅此游戏")
                    }
                } catch (e: Exception) {
                    if (SteamhelperPluginSetting.debug) e.printStackTrace()
                }
                if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
            }
        } else {
            cs.sendMessage("暂不支持该渠道订阅操作")
        }
    }
}

/**
 * 对全部订阅的操作
 * @param flag Boolean true为查看false为删除
 * @param cs CommandSender
 */
suspend fun getAllSubscribe(flag: Boolean, cs: CommandSender) {
    if (cs.bot?.id != null && cs.subject?.id != null) {
        lockSubscribeMap.lock()
        lockMapSub.lock()
        val sb = StringBuilder()
        try {
            val botID = cs.bot?.id!!
            val contactID = cs.subject?.id!!
            if (flag) sb.append("您目前的订阅有：\n")
            else sb.append("取消了您的订阅：\n")
            for (app in SteamhelperPluginData.subscribeMap.keys) {
                if (SteamhelperPluginData.subscribeMap[app]?.get(botID)?.get(contactID) != null) {
                    sb.append(mapSub[app]?.appName.toString() + '(' + mapSub[app]?.appid.toString() + ")\n")
                    if (!flag) {
                        SteamhelperPluginData.subscribeMap[app]?.get(botID)?.remove(contactID)
                    }
                }
            }
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
        }
        if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
        if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
        cs.sendMessage(sb.toString())
    } else {
        cs.sendMessage("暂不支持该渠道订阅操作")
    }
}

/** 获取推送信息 */
@OptIn(ConsoleExperimentalApi::class)
suspend fun getPush(cs: CommandSender) {
    if (cs.bot?.id != null && cs.subject?.id != null) {
        val botID = cs.bot?.id!!
        val contactID = cs.subject?.id!!
        try {
            val contact = Bot.getInstance(botID).getContact(contactID, true)
            // 进行一次试发送，如失败则不允许订阅
            contact.sendMessage("查询您的推送状态……")
            lockPushMap.lock()
            try {
                if (!SteamhelperPluginData.pushMap.contains(botID)) {
                    SteamhelperPluginData.pushMap[botID] = mutableMapOf(contactID to 0)
                } else if (!SteamhelperPluginData.pushMap[botID]!!.contains(contactID)) {
                    SteamhelperPluginData.pushMap[botID]!![contactID] = 0
                } else {
                    SteamhelperPluginData.pushMap[botID]!!.remove(contactID)
                    cs.sendMessage("您的推送已关闭")
                    if (lockPushMap.isHeldByCurrentThread) lockPushMap.unlock()
                    return
                }
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
            }
            if (lockPushMap.isHeldByCurrentThread) lockPushMap.unlock()
            cs.sendMessage("您的推送已开启")
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
            cs.sendMessage("操作推送失败，机器人无法给您发送信息，错误原因：\n$e")
        }
    } else {
        cs.sendMessage("暂不支持该渠道推送操作")
    }
}

/** 搜索信息替换关键字 */
val keywordsSearch = arrayOf("<id>", "<nm>", "<ds>")

fun getSearch(AppNameOrAppid: Array<out String>): String {
    val search = SteamApp()
    // 判断是否为纯数字
    if (AppNameOrAppid.size == 1 && AppNameOrAppid[0].matches("^[0-9]*$".toRegex())) {
        search.appid = AppNameOrAppid[0]
    } else {
        val keyword = AppNameOrAppid.joinToString(" ")
        when (search.search(SteamhelperPluginSetting.areasSearch.toTypedArray(), keyword)) {
            1 -> {
                //成功
            }
            0 -> {
                return "未找到相应的App"
            }
            else -> {
                Steamhelper.logger.info { "SteamHelper在搜索的时候得到了一个错误：" }
                Steamhelper.logger.info { search.exception }
                return "搜索发生错误，如长期出现此信息请检查日志和机器人网络状况"
            }
        }
    }
    if (search.appid == null) {
        return "搜索失败，如长期出现此信息请检查日志和机器人网络状况"
    }
    return when (search.refreshInfo(SteamhelperPluginSetting.areasSearch.toTypedArray())) {
        1 -> {
            //成功
            replace(
                SteamhelperPluginSetting.messageSearch,
                keywordsSearch,
                arrayOf(search.appid, search.appName, search.appDescription)
            )
        }
        0 -> "搜索无结果"
        -1 -> "搜索失败，如长期出现此信息请检查日志和机器人网络状况"
        else -> "内部错误"
    }
}