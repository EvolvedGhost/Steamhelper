/**
 * SteamUtils.kt
 * 储存插件后台所需的Job和线程
 * 以及一些有用的函数
 */
package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.*
import com.evolvedghost.mirai.steamhelper.Steamhelper.reload
import com.evolvedghost.mirai.steamhelper.Steamhelper.save
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantLock


val lockSteamWeek = ReentrantLock()
val steamWeek = SteamWeek()
val lockSteamInfo = ReentrantLock()
val steamInfo = SteamInfo()
val lockExchange = ReentrantLock()
val exchange = Exchange()
val lockScheduler = ReentrantLock()
var mapSub = mutableMapOf<Int, SteamApp>()
var lockMapSub = ReentrantLock()
var scheduler: Scheduler = StdSchedulerFactory().scheduler

/** 重载插件 */
fun reloadPlugin() {
    lockSteamWeek.lock()
    lockSteamInfo.lock()
    lockMapSub.lock()
    try {
        SteamhelperPluginSetting.reload()
    } catch (e: Exception) {
        if (SteamhelperPluginSetting.debug) e.printStackTrace()
    }
    if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
    if (lockSteamWeek.isHeldByCurrentThread) lockSteamWeek.unlock()
    if (lockSteamInfo.isHeldByCurrentThread) lockSteamInfo.unlock()
    RefreshThread().start()

    lockScheduler.lock()
    try {
        if (scheduler.isStarted) {
            scheduler.shutdown()
        }
        scheduler = StdSchedulerFactory().scheduler
        CronTrigger().run()
    } catch (e: Exception) {
        if (SteamhelperPluginSetting.debug) e.printStackTrace()
    }
    if (lockScheduler.isHeldByCurrentThread) lockScheduler.unlock()
}

/** 发送信息 */
@OptIn(ConsoleExperimentalApi::class)
suspend fun send(bot: Long, contact: Long, message: String): Boolean {
    return try {
        Bot.getInstance(bot).getContact(contact, true).sendMessage(message)
        true
    } catch (e: Exception) {
        if (SteamhelperPluginSetting.debug) e.printStackTrace()
        false
    }
}

/** 增加错误次数 */
fun addErrors(appid: Int, bot: Long, contact: Long) {
    lockSubscribeMap.lock()
    try {
        var count = SteamhelperPluginData.subscribeMap[appid]?.get(bot)?.get(contact)
        if (count != null) {
            count++
            if (count >= SteamhelperPluginSetting.errors) {
                SteamhelperPluginData.subscribeMap[appid]!![bot]!!.remove(contact)
                Steamhelper.logger.info("BotID:$bot,AppID:$appid,联系人ID:$contact 已经出现" + SteamhelperPluginSetting.errors + "次报错，其订阅已被删除")
            } else {
                SteamhelperPluginData.subscribeMap[appid]!![bot]!![contact] = count
            }
        }
    } catch (e: Exception) {
        if (SteamhelperPluginSetting.debug) e.printStackTrace()
    }
    if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
}

/** 发送订阅信息 */
@OptIn(DelicateCoroutinesApi::class)
fun sendSubscribe(appid: Int, mutableMap: MutableMap<Long, MutableMap<Long, Int>>, message: String) {
    // 发送信息需要使用协程
    GlobalScope.launch {
        for (bot in mutableMap.keys) {
            if (Bot.getInstanceOrNull(bot) != null) {
                // Bot在线，正常发送
                for (contact in mutableMap[bot]!!.keys) {
                    if (!send(bot, contact, message)) {
                        // 发送失败，增加错误次数
                        addErrors(appid, bot, contact)
                    } else if (mutableMap[bot]!![contact] != 0) {
                        // 发送成功且有错误次数，清零错误次数
                        lockSubscribeMap.lock()
                        try {
                            if (SteamhelperPluginData.subscribeMap[appid]?.get(bot)?.get(contact) != null) {
                                SteamhelperPluginData.subscribeMap[appid]!![bot]!![contact] = 0
                            }
                        } catch (e: Exception) {
                            if (SteamhelperPluginSetting.debug) e.printStackTrace()
                        }
                        if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
                    }
                    // 防止发送过快风控
                    delay((500..2000).random().toLong())
                }
            } else {
                // Bot不在线
                Steamhelper.logger.info("BotID:$bot 获取为null，可能未上线，订阅推送失败")
            }
        }
    }
}

/** 信息刷新线程 */
class RefreshThread : Thread() {
    override fun run() {
        // 刷新周榜
        lockSteamWeek.lock()
        if (!steamWeek.refreshSteamWeeklyTopSellers()) {
            Steamhelper.logger.info("Steam周榜刷新失败：")
            Steamhelper.logger.info(steamWeek.exception)
        }
        if (lockSteamWeek.isHeldByCurrentThread) lockSteamWeek.unlock()
        // 刷新信息
        lockSteamInfo.lock()
        if (!steamInfo.refreshSteamInfo()) {
            Steamhelper.logger.info("Steam信息刷新失败：")
            Steamhelper.logger.info(steamInfo.exception)
        }
        if (lockSteamInfo.isHeldByCurrentThread) lockSteamInfo.unlock()
        // 刷新汇率信息
        if (!area4currency[SteamhelperPluginSetting.areasPrice[0]].isNullOrEmpty()) {
            lockExchange.lock()
            try {
                if (!exchange.refresh(area4currency[SteamhelperPluginSetting.areasPrice[0]]!!)) {
                    Steamhelper.logger.info("Steam汇率信息更新失败：")
                    Steamhelper.logger.info(exchange.exception)
                }
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
            }
            if (lockExchange.isHeldByCurrentThread) lockExchange.unlock()
        } else {
            Steamhelper.logger.info("Steam汇率信息更新失败：请检查价格区域格式是否正确")
        }
        // 清理为空的数据储存Map
        lockSubscribeMap.lock()
        try {
            val iterator = SteamhelperPluginData.subscribeMap.iterator()
            while (iterator.hasNext()) {
                val curr = iterator.next()
                if (curr.value.isEmpty()) iterator.remove()
                else {
                    val subIterator = curr.value.iterator()
                    while (subIterator.hasNext()) {
                        val subCurr = subIterator.next()
                        if (subCurr.value.isEmpty()) subIterator.remove()
                    }
                    if (curr.value.isEmpty()) iterator.remove()
                }
            }
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
        }
        if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
        // 刷新订阅的游戏信息
        // 检验当前内存订阅的游戏信息与数据中订阅的游戏信息是否一致
        lockSubscribeMap.lock()
        lockMapSub.lock()
        try {
            if (mapSub.size != SteamhelperPluginData.subscribeMap.size) {
                val tempMapSub = mutableMapOf<Int, SteamApp>()
                for (i in SteamhelperPluginData.subscribeMap.keys) {
                    if (mapSub.contains(i) && mapSub[i] != null) {
                        // 有就继承原来的
                        tempMapSub[i] = mapSub[i]!!
                    } else {
                        // 没有就新建一个
                        tempMapSub[i] = SteamApp(i.toString())
                    }
                }
                // 替换mapSub
                mapSub = tempMapSub
            }
        } catch (e: Exception) {
            if (SteamhelperPluginSetting.debug) e.printStackTrace()
        }
        if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
        if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
        // 刷新订阅的游戏信息
        if (!area4currency[SteamhelperPluginSetting.areasPrice[0]].isNullOrEmpty()) {
            lockMapSub.lock()
            val tempMapSub = mapSub
            if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
            for (i in tempMapSub.keys) {
                Thread {
                    if (tempMapSub[i] != null) {
                        val flag = tempMapSub[i]!!.refreshPriceAndInfo(SteamhelperPluginSetting.areasPrice[0])
                        if (flag > -2) {
                            // 游戏信息有更新，写入mapSub
                            lockMapSub.lock()
                            mapSub[i] = tempMapSub[i]!!
                            if (lockMapSub.isHeldByCurrentThread) lockMapSub.unlock()
                            // 游戏价格变化，需要发送订阅信息
                            if (flag == 2 || flag == 0) {
                                val message = getSubscribe(tempMapSub[i]!!, flag)
                                // 复制一份订阅人表用于发送
                                lockSubscribeMap.lock()
                                val map = SteamhelperPluginData.subscribeMap
                                if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
                                if (!map[i].isNullOrEmpty()) {
                                    // 发送订阅信息
                                    sendSubscribe(i, map[i]!!, message)
                                }
                            } else if (flag == -1) {
                                // 锁区游戏订阅没意义
                                // 当然可能是Steam方面错误
                                // 因此采用增加错误数量的方法
                                // 复制一份订阅人表用于增加错误数量
                                lockSubscribeMap.lock()
                                val map = SteamhelperPluginData.subscribeMap[i]
                                if (lockSubscribeMap.isHeldByCurrentThread) lockSubscribeMap.unlock()
                                if (!map.isNullOrEmpty()) {
                                    for (bot in map.keys) {
                                        for (contact in map[bot]!!.keys) {
                                            addErrors(i, bot, contact)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.start()
            }
        } else {
            Steamhelper.logger.info("Steam订阅刷新失败：请检查价格区域格式是否正确")
        }
        SteamhelperPluginData.save()
        Steamhelper.logger.info("信息刷新线程执行完毕")
    }
}

/** 大促信息推送Job */
class SaleJob : Job {
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        lockPushMap.lock()
        val map = SteamhelperPluginData.pushMap
        if (lockPushMap.isHeldByCurrentThread) lockPushMap.unlock()
        for (bot in map.keys) {
            for (contact in map[bot]!!.keys) {
                GlobalScope.launch { send(bot, contact, getSale()) }
                // 防止发送过快风控，不会阻塞Mirai线程
                Thread.sleep((500..2000).random().toLong())
            }
        }
        Steamhelper.logger.info("大促信息推送完毕")
    }
}

/** 周榜信息推送Job */
class WeekJob : Job {
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        lockPushMap.lock()
        val map = SteamhelperPluginData.pushMap
        if (lockPushMap.isHeldByCurrentThread) lockPushMap.unlock()
        for (bot in map.keys) {
            for (contact in map[bot]!!.keys) {
                GlobalScope.launch { send(bot, contact, getWeek()) }
                // 防止发送过快风控，不会阻塞Mirai线程
                Thread.sleep((500..2000).random().toLong())
            }
        }
        Steamhelper.logger.info("周榜信息推送完毕")
    }
}

/** 信息刷新任务Job */
class FreshJob : Job {
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        RefreshThread().start()
    }
}

/** 插件所有的调度 */
class CronTrigger {
    @Throws(SchedulerException::class)
    fun run() {
        // 调度器的对象的实例化
        val jobSale = JobBuilder.newJob(SaleJob::class.java).withIdentity("jobSale", "group1").build()
        val jobWeek = JobBuilder.newJob(WeekJob::class.java).withIdentity("jobWeek", "group1").build()
        val jobFresh = JobBuilder.newJob(FreshJob::class.java).withIdentity("jobFresh", "group1").build()
        val triggerSale = TriggerBuilder.newTrigger().withIdentity("triggerSale", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timePushSale)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobSale).build()
        val triggerWeek = TriggerBuilder.newTrigger().withIdentity("triggerWeek", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timePushWeek)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobWeek).build()
        val triggerFresh = TriggerBuilder.newTrigger().withIdentity("triggerFresh", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timeRefresh)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobFresh).build()
        scheduler.scheduleJob(jobSale, triggerSale)
        scheduler.scheduleJob(jobWeek, triggerWeek)
        scheduler.scheduleJob(jobFresh, triggerFresh)
        scheduler.start()
    }
}