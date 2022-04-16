/**
 * EpicMessage.kt
 * 用于编写插件Epic相关最终发出的信息的整理与格式化
 */

package com.evolvedghost.mirai.steamhelper.messager

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginData
import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import com.evolvedghost.mirai.steamhelper.lockPushEpicMap
import com.evolvedghost.mirai.steamhelper.messager.handler.getFormattedTime
import com.evolvedghost.mirai.steamhelper.messager.handler.replace
import com.evolvedghost.mirai.steamhelper.messager.handler.setPush
import com.evolvedghost.mirai.steamhelper.steamhelper.epicPromote
import com.evolvedghost.mirai.steamhelper.steamhelper.lockEpicPromote
import net.mamoe.mirai.console.command.CommandSender

/** Epic周免替换关键字 */
val keywordsEpicPromote = arrayOf("<cls>", "<fls>")

/** Epic周免榜单替换关键字 */
val keywordsEpicPromoteList = arrayOf("<nm>", "<ds>", "<tf>", "<ts>")

/** 获取Epic周免信息 */
fun getEpic(): String {
    return if (epicPromote.current.isNotEmpty()) {
        // 复制epicPromote
        lockEpicPromote.lock()
        val current = epicPromote.current.toMutableList()
        val future = epicPromote.future.toMutableList()
        if (lockEpicPromote.isHeldByCurrentThread) lockEpicPromote.unlock()
        // 处理messageEpicPromoteList
        val currentSB = StringBuilder()
        for (element in current) {
            currentSB.append(
                replace(
                    SteamhelperPluginSetting.messageEpicPromoteList, keywordsEpicPromoteList, arrayOf(
                        element.title,
                        element.description,
                        getFormattedTime(element.effectiveDate * 1000),
                        element.effectiveDate.toString()
                    )
                ) + "\n"
            )
        }
        val futureSB = StringBuilder()
        if (future.isNotEmpty()) {
            for (element in future) {
                futureSB.append(
                    replace(
                        SteamhelperPluginSetting.messageEpicPromoteList, keywordsEpicPromoteList, arrayOf(
                            element.title,
                            element.description,
                            getFormattedTime(element.effectiveDate * 1000),
                            element.effectiveDate.toString()
                        )
                    ) + "\n"
                )
            }
        } else futureSB.append("暂无\n")
        // 处理messageEpicPromote
        replace(
            SteamhelperPluginSetting.messageEpicPromote, keywordsEpicPromote, arrayOf(
                currentSB.toString(), futureSB.toString()
            )
        )
    } else {
        "相关信息暂未初始化请稍后，如长期出现此信息请检查日志和机器人网络状况"
    }
}

/** 获取推送信息 */
suspend fun getEpicPush(cs: CommandSender) {
    setPush(cs, SteamhelperPluginData.pushEpicMap, lockPushEpicMap, "Epic周免")
}