package com.evolvedghost.mirai.steamhelper

import com.evolvedghost.mirai.steamhelper.utils.lockScheduler
import com.evolvedghost.mirai.steamhelper.utils.pluginLogger
import com.evolvedghost.mirai.steamhelper.utils.reloadPlugin
import com.evolvedghost.mirai.steamhelper.utils.scheduler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


object Steamhelper : KotlinPlugin(JvmPluginDescription(
    id = "com.evolvedghost.mirai.steamhelper.steamhelper",
    name = "SteamHelper",
    version = "1.0.7",
) {
    author("EvolvedGhost")
}) {
    val PERMISSION_EXECUTE_PUSH by lazy {
        PermissionService.INSTANCE.register(permissionId("push"), "推送权限，可防止有人乱开关")
    }
    val PERMISSION_EXECUTE_SUB by lazy {
        PermissionService.INSTANCE.register(permissionId("sub"), "订阅权限，可防止消息过多过吵")
    }
    val PERMISSION_EXECUTE_RELOAD by lazy {
        PermissionService.INSTANCE.register(permissionId("reload"), "重载权限")
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnable() {
        GlobalScope.launch {
            lockSubscribeMap.withLock {
                lockPushMap.withLock {
                    lockPushEpicMap.withLock {
                        SteamhelperPluginData.reload()
                    }
                }
            }
            SteamhelperPluginData.save()
            PERMISSION_EXECUTE_PUSH
            PERMISSION_EXECUTE_SUB
            PERMISSION_EXECUTE_RELOAD
            SteamhelperPluginSetting.reload()
            SteamhelperPluginSetting.save()
            SteamhelperCommand.register()
            reloadPlugin()
            pluginLogger("SteamHelper已就绪，数据刷新命令已经发出，会延迟于插件启动，请稍后")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDisable() {
        GlobalScope.launch {
            lockScheduler.withLock {
                scheduler.shutdown()
            }
            SteamhelperPluginData.save()
            SteamhelperCommand.unregister()
            pluginLogger("SteamHelper已关闭捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏")
        }
    }
}

val lockSubscribeMap = Mutex()
val lockPushMap = Mutex()
val lockPushEpicMap = Mutex()

object SteamhelperPluginData : AutoSavePluginData("Steamhelper") { // "name" 是保存的文件名 (不带后缀)
    /**
     * 存储订阅表
     * 储存结构为：Map<SteamAppid,Map<机器人QQ, Map<联系人id, 错误次数>>>
     */
    val subscribeMap: MutableMap<Int, MutableMap<Long, MutableMap<Long, Int>>> by value()

    /** 存储推送表，储存结构为：Map<机器人QQ,Map<联系人id，错误次数>> */
    val pushMap: MutableMap<Long, MutableMap<Long, Int>> by value()

    /** 存储Epic推送表，储存结构为：Map<机器人QQ,Map<联系人id，错误次数>> */
    val pushEpicMap: MutableMap<Long, MutableMap<Long, Int>> by value()
}

