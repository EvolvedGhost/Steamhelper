/**
 * PushHandler.kt
 * 用于快速设置Push
 */

package com.evolvedghost.mirai.steamhelper.messager.handler

import com.evolvedghost.mirai.steamhelper.utils.pluginExceptionHandler
import com.evolvedghost.mirai.steamhelper.utils.pluginWarn
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import java.util.concurrent.locks.ReentrantLock

@OptIn(ConsoleExperimentalApi::class)
suspend fun setPush(
    cs: CommandSender, map: MutableMap<Long, MutableMap<Long, Int>>, locker: ReentrantLock, message: String
) {
    if (cs.bot?.id != null && cs.subject?.id != null) {
        val botID = cs.bot?.id!!
        val contactID = cs.subject?.id!!
        try {
            val contact = Bot.getInstance(botID).getContact(contactID, true)
            // 进行一次试发送，如失败则不允许订阅
            contact.sendMessage("查询您的推送状态……")
            locker.lock()
            try {
                if (!map.contains(botID)) {
                    map[botID] = mutableMapOf(contactID to 0)
                } else if (!map[botID]!!.contains(contactID)) {
                    map[botID]!![contactID] = 0
                } else {
                    map[botID]!!.remove(contactID)
                    cs.sendMessage("您的" + message + "推送已关闭")
                    if (locker.isHeldByCurrentThread) locker.unlock()
                    return
                }
            } catch (e: Exception) {
                pluginExceptionHandler("推送", e)
                pluginWarn("开启/关闭推送失败，操作记录时出现了一个问题：", e.toString())
            }
            if (locker.isHeldByCurrentThread) locker.unlock()
            cs.sendMessage("您的" + message + "推送已开启")
        } catch (e: Exception) {
            pluginExceptionHandler("推送", e)
            pluginWarn("开启/关闭推送失败，机器人无法给您发送信息：", e.toString())
        }
    } else {
        cs.sendMessage("暂不支持该渠道" + message + "推送操作")
    }
}
