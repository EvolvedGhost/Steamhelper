package com.evolvedghost.mirai.steamhelper

import com.evolvedghost.mirai.steamhelper.messager.*
import com.evolvedghost.mirai.steamhelper.utils.reloadPlugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission

object SteamhelperCommand : CompositeCommand(
    Steamhelper,
    "sh", "#sh",
    description = "Steamhelper指令",
) {
    @SubCommand("week", "周榜")
    @Description("获取Steam每周销量榜单")
    suspend fun CommandSender.week() {
        sendMessage(getWeek())
    }

    @SubCommand("stat", "状态")
    @Description("获取最近的Steam状态")
    suspend fun CommandSender.stat() {
        sendMessage(getStat())
    }

    @SubCommand("sale", "促销")
    @Description("获取最近的Steam促销")
    suspend fun CommandSender.sale() {
        sendMessage(getSale())
    }

    @SubCommand("cp", "比价")
    @Description("对比某SteamApp各区域的价格")
    suspend fun CommandSender.compare(vararg AppNameOrAppid: String) {
        sendMessage(getCompare(AppNameOrAppid))
    }

    @SubCommand("sub", "订阅")
    @Description("订阅一个SteamApp的价格变化")
    suspend fun CommandSender.subscribe(vararg AppNameOrAppid: String) {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_SUB)) {
            getSubscribe(true, this, AppNameOrAppid)
        } else {
            sendMessage("你没有订阅的权限")
        }
    }

    @SubCommand("unsub", "取消订阅")
    @Description("取消订阅一个SteamApp")
    suspend fun CommandSender.unsub(vararg AppNameOrAppid: String) {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_SUB)) {
            getSubscribe(false, this, AppNameOrAppid)
        } else {
            sendMessage("你没有订阅的权限")
        }
    }

    @SubCommand("list", "查看订阅")
    @Description("查看该会话下的所有订阅")
    suspend fun CommandSender.list() {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_SUB)) {
            getAllSubscribe(true, this)
        } else {
            sendMessage("你没有订阅的权限")
        }
    }

    @SubCommand("unall", "取消全部订阅")
    @Description("取消该会话下的所有订阅")
    suspend fun CommandSender.unall() {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_SUB)) {
            getAllSubscribe(false, this)
        } else {
            sendMessage("你没有订阅的权限")
        }
    }

    @SubCommand("push", "推送")
    @Description("定时推送大促、周榜信息")
    suspend fun CommandSender.push() {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_PUSH)) {
            getPush(this)
        } else {
            sendMessage("你没有推送的权限")
        }
    }

    @SubCommand("sr", "搜索")
    @Description("搜索一个SteamApp")
    suspend fun CommandSender.search(vararg AppNameOrAppid: String) {
        sendMessage(getSearch(AppNameOrAppid))
    }

    @SubCommand("reload", "重载")
    @Description("重载并刷新Steamhelper")
    suspend fun CommandSender.reload() {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_RELOAD)) {
            reloadPlugin()
            sendMessage("插件重载完成，数据刷新命令已发出")
        } else {
            sendMessage("你没有重载的权限")
        }
    }

    @SubCommand("epic")
    @Description("获取epic周免信息")
    suspend fun CommandSender.epic() {
        sendMessage(getEpic())
    }

    @SubCommand("pushepic", "推送epic")
    @Description("开启epic周免信息推送")
    suspend fun CommandSender.pushepic() {
        if (this.hasPermission(Steamhelper.PERMISSION_EXECUTE_PUSH)) {
            getEpicPush(this)
        } else {
            sendMessage("你没有推送的权限")
        }
    }

    @SubCommand("uid")
    @Description("解析各种格式的SteamUID")
    suspend fun CommandSender.uid(SteamUID: String) {
        sendMessage(getSteamUID(SteamUID))
    }
}