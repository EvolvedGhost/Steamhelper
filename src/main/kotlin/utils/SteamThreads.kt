/**
 * SteamThreads.kt
 * 储存插件后台所需的Job和线程
 */
package com.evolvedghost.mirai.steamhelper.utils

import com.evolvedghost.mirai.steamhelper.*
import com.evolvedghost.mirai.steamhelper.Steamhelper.reload
import com.evolvedghost.mirai.steamhelper.Steamhelper.save
import com.evolvedghost.mirai.steamhelper.messager.getEpic
import com.evolvedghost.mirai.steamhelper.messager.getSale
import com.evolvedghost.mirai.steamhelper.messager.getSubscribe
import com.evolvedghost.mirai.steamhelper.messager.getWeek
import com.evolvedghost.mirai.steamhelper.steamhelper.area4currency
import com.evolvedghost.mirai.steamhelper.worker.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.io.FileOutputStream
import java.util.*


val lockSteamWeek = Mutex()
val steamWeek = SteamWeek()
val lockSteamInfo = Mutex()
val steamInfo = SteamInfo()
val lockExchange = Mutex()
val epicPromote = EpicPromotions()
val lockEpicPromote = Mutex()
val exchange = Exchange()
val lockScheduler = Mutex()
var mapSub = mutableMapOf<Int, SteamApp>()
var lockMapSub = Mutex()
var scheduler: Scheduler = StdSchedulerFactory().scheduler


/** 重载插件 */
suspend fun reloadPlugin() {
    lockEpicPromote.withLock {
        lockSteamWeek.withLock {
            lockSteamInfo.withLock {
                lockMapSub.withLock {
                    try {
                        SteamhelperPluginSetting.reload()
                    } catch (e: Exception) {
                        pluginExceptionHandler("重载插件配置", e)
                    }
                }
            }
        }
    }
    lockScheduler.withLock {
        try {
            if (scheduler.isStarted) {
                scheduler.shutdown()
            }
            // 防止与其他使用Quartz的插件冲突，同样其他使用Quartz的插件也需要配置自己的Properties
            val file = Steamhelper.configFolder.resolve("quartz.properties")
            val p = Properties()
            p.setProperty("org.quartz.scheduler.instanceName", "EvolvedGhostMiraiSteamhelperScheduler")
            p.setProperty("org.quartz.threadPool.threadCount", "4")
            if (!file.exists()) {
                try {
                    val fos = FileOutputStream(file)
                    p.store(fos, null)
                    fos.close()
                } catch (e: Exception) {
                    pluginExceptionHandler("properties写入", e)
                }
                scheduler = StdSchedulerFactory(p).scheduler
            } else {
                scheduler = try {
                    StdSchedulerFactory(file.path).scheduler
                } catch (e: Exception) {
                    pluginExceptionHandler("properties读取", e)
                    StdSchedulerFactory(p).scheduler
                }
            }
            CronTrigger().run()
        } catch (e: Exception) {
            pluginExceptionHandler("重载计划任务", e)
        }
    }
    RefreshThread().start()
}

/** 发送信息 */
@OptIn(ConsoleExperimentalApi::class)
suspend fun send(bot: Long, contact: Long, message: String): Boolean {
    return try {
        Bot.getInstance(bot).getContact(contact, true).sendMessage(message)
        true
    } catch (e: Exception) {
        pluginExceptionHandler("消息发送", e)
        false
    }
}

/** 增加错误次数 */
suspend fun addErrors(appid: Int, bot: Long, contact: Long) {
    lockSubscribeMap.withLock {
        try {
            var count = SteamhelperPluginData.subscribeMap[appid]?.get(bot)?.get(contact)
            if (count != null) {
                count++
                if (count >= SteamhelperPluginSetting.errors) {
                    SteamhelperPluginData.subscribeMap[appid]!![bot]!!.remove(contact)
                    pluginLogger("BotID:$bot,AppID:$appid,联系人ID:$contact 已经出现" + SteamhelperPluginSetting.errors + "次报错，其订阅已被删除")
                } else {
                    SteamhelperPluginData.subscribeMap[appid]!![bot]!![contact] = count
                }
            }
        } catch (e: Exception) {
            pluginExceptionHandler("错误计数", e)
        }
    }
}

/** 发送订阅信息 */
suspend fun sendSubscribe(appid: Int, mutableMap: MutableMap<Long, MutableMap<Long, Int>>, message: String) {
    for (bot in mutableMap.keys) {
        if (Bot.getInstanceOrNull(bot) != null) {
            // Bot在线，正常发送
            for (contact in mutableMap[bot]!!.keys) {
                if (!send(bot, contact, message)) {
                    // 发送失败，增加错误次数
                    addErrors(appid, bot, contact)
                } else if (mutableMap[bot]!![contact] != 0) {
                    // 发送成功且有错误次数，清零错误次数
                    lockSubscribeMap.withLock {
                        try {
                            if (SteamhelperPluginData.subscribeMap[appid]?.get(bot)?.get(contact) != null) {
                                SteamhelperPluginData.subscribeMap[appid]!![bot]!![contact] = 0
                            }
                        } catch (e: Exception) {
                            pluginExceptionHandler("错误次数清零", e)
                        }
                    }
                }
                // 防止发送过快风控
                delay((500..2000).random().toLong())
            }
        } else {
            // Bot不在线
            pluginLogger("BotID:$bot 获取为null，可能未上线，订阅推送失败")
        }
    }
}

/** 信息刷新线程 */
class RefreshThread : Thread() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun run() {
        GlobalScope.launch {
            // 刷新周榜
            lockSteamWeek.withLock {
                if (!steamWeek.refreshSteamWeeklyTopSellers()) {
                    pluginWarn("Steam周榜刷新失败：", steamWeek.exception)
                }
            }
            // 刷新信息
            lockSteamInfo.withLock {
                if (!steamInfo.refreshSteamInfo()) {
                    pluginWarn("Steam信息刷新失败：", steamInfo.exception)
                }
            }
            // 刷新信息
            lockEpicPromote.withLock {
                if (!epicPromote.refresh()) {
                    pluginWarn("Epic周免刷新失败：", epicPromote.exception)
                }
            }
            // 刷新汇率信息
            if (!area4currency[SteamhelperPluginSetting.areasPrice[0]].isNullOrEmpty()) {
                lockExchange.withLock {
                    try {
                        if (!exchange.refresh(area4currency[SteamhelperPluginSetting.areasPrice[0]]!!)) {
                            pluginWarn("Steam汇率信息更新失败：", exchange.exception)
                        }
                    } catch (e: Exception) {
                        pluginExceptionHandler("刷新汇率", e)
                    }
                }
            } else {
                pluginLogger("Steam汇率信息更新失败：请检查价格区域格式是否正确")
            }
            // 清理为空的数据储存Map
            lockSubscribeMap.withLock {
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
                    pluginExceptionHandler("清空内存空的数据", e)
                }
            }
            // 刷新订阅的游戏信息
            // 检验当前内存订阅的游戏信息与数据中订阅的游戏信息是否一致
            lockSubscribeMap.withLock {
                lockMapSub.withLock {
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
                        pluginExceptionHandler("验证内存储存游戏信息", e)
                    }
                }
            }
            // 刷新订阅的游戏信息
            if (!area4currency[SteamhelperPluginSetting.areasPrice[0]].isNullOrEmpty()) {
                val tempMapSub = lockMapSub.withLock { mapSub.toMutableMap() }
                for (i in tempMapSub.keys) {
                    GlobalScope.launch {
                        if (tempMapSub[i] != null) {
                            val flag = tempMapSub[i]!!.refreshPriceAndInfo(SteamhelperPluginSetting.areasPrice[0])
                            if (flag > -2) {
                                // 游戏信息有更新，写入mapSub
                                lockMapSub.withLock { mapSub[i] = tempMapSub[i]!! }
                                // 游戏价格变化，需要发送订阅信息
                                if (flag == 2 || flag == 0) {
                                    val message = getSubscribe(tempMapSub[i]!!, flag)
                                    // 复制一份订阅人表用于发送
                                    val map =
                                        lockSubscribeMap.withLock { SteamhelperPluginData.subscribeMap.toMutableMap() }
                                    if (!map[i].isNullOrEmpty()) {
                                        // 发送订阅信息
                                        sendSubscribe(i, map[i]!!, message)
                                    }
                                } else if (flag == -1) {
                                    // 锁区游戏订阅没意义
                                    // 当然可能是Steam方面错误
                                    // 因此采用增加错误数量的方法
                                    // 复制一份订阅人表用于增加错误数量
                                    val map =
                                        lockSubscribeMap.withLock { SteamhelperPluginData.subscribeMap[i]?.toMutableMap() }
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
                pluginLogger("Steam订阅刷新失败：请检查价格区域格式是否正确")
            }
            SteamhelperPluginData.save()
            pluginLogger("信息刷新线程执行完毕")
        }
    }
}

/** 大促信息推送Job */
class SaleJob : Job {
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        GlobalScope.launch {
            val map = lockPushMap.withLock { SteamhelperPluginData.pushMap.toMutableMap() }
            val saleMessage = getSale()
            for (bot in map.keys) {
                for (contact in map[bot]!!.keys) {
                    send(bot, contact, saleMessage)
                    delay((500..2000).random().toLong())
                }
            }
            pluginLogger("Steam大促信息推送完毕")
        }
    }
}

/** Steam周榜信息推送Job */
class WeekJob : Job {
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        GlobalScope.launch {
            val map = lockPushMap.withLock { SteamhelperPluginData.pushMap.toMutableMap() }
            val weekMessage = getWeek()
            for (bot in map.keys) {
                for (contact in map[bot]!!.keys) {
                    send(bot, contact, weekMessage)
                    delay((500..2000).random().toLong())
                }
            }
            pluginLogger("Steam周榜信息推送完毕")
        }
    }
}

/** Epic周免信息推送Job */
class EpicJob : Job {
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(JobExecutionException::class)
    override fun execute(arg0: JobExecutionContext) {
        GlobalScope.launch {
            val map = lockPushEpicMap.withLock { SteamhelperPluginData.pushMap.toMutableMap() }
            val epicMessage = getEpic()
            for (bot in map.keys) {
                for (contact in map[bot]!!.keys) {
                    send(bot, contact, epicMessage)
                    delay((500..2000).random().toLong())
                }
            }
            pluginLogger("Epic周免信息推送完毕")
        }
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
        val jobEpic = JobBuilder.newJob(EpicJob::class.java).withIdentity("jobEpic", "group1").build()
        val jobFresh = JobBuilder.newJob(FreshJob::class.java).withIdentity("jobFresh", "group1").build()
        val triggerSale = TriggerBuilder.newTrigger().withIdentity("triggerSale", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timePushSale)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobSale).build()
        val triggerWeek = TriggerBuilder.newTrigger().withIdentity("triggerWeek", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timePushWeek)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobWeek).build()
        val triggerEpic = TriggerBuilder.newTrigger().withIdentity("triggerEpic", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timePushEpic)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobEpic).build()
        val triggerFresh = TriggerBuilder.newTrigger().withIdentity("triggerFresh", "group1").withSchedule(
            CronScheduleBuilder.cronSchedule(SteamhelperPluginSetting.timeRefresh)
                .inTimeZone(TimeZone.getTimeZone(SteamhelperPluginSetting.timeZone))
        ).forJob(jobFresh).build()
        scheduler.scheduleJob(jobSale, triggerSale)
        scheduler.scheduleJob(jobWeek, triggerWeek)
        scheduler.scheduleJob(jobEpic, triggerEpic)
        scheduler.scheduleJob(jobFresh, triggerFresh)
        scheduler.start()
    }
}