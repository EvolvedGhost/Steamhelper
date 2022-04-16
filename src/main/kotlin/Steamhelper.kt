package com.evolvedghost.mirai.steamhelper

import com.evolvedghost.mirai.steamhelper.messager.*
import com.evolvedghost.mirai.steamhelper.steamhelper.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import java.util.concurrent.locks.ReentrantLock


object Steamhelper : KotlinPlugin(JvmPluginDescription(
    id = "com.evolvedghost.mirai.steamhelper.steamhelper",
    name = "SteamHelper",
    version = "1.0.3",
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

    override fun onEnable() {
        lockSubscribeMap.lock()
        lockPushMap.lock()
        SteamhelperPluginData.reload()
        lockPushMap.unlock()
        lockSubscribeMap.unlock()
        SteamhelperPluginData.save()
        PERMISSION_EXECUTE_PUSH
        PERMISSION_EXECUTE_SUB
        PERMISSION_EXECUTE_RELOAD
        SteamhelperPluginSetting.reload()
        SteamhelperPluginSetting.save()
        SteamhelperCommand.register()
        reloadPlugin()
        logger.info { "SteamHelper已就绪，数据刷新命令已经发出，会延迟于插件启动，请稍后" }
    }

    override fun onDisable() {
        lockScheduler.lock()
        scheduler.shutdown()
        lockScheduler.unlock()
        SteamhelperPluginData.save()
        SteamhelperCommand.unregister()
        logger.info { "SteamHelper已关闭捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏捏" }
    }
}

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
}

val lockSubscribeMap = ReentrantLock()
val lockPushMap = ReentrantLock()
val lockPushEpicMap = ReentrantLock()

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

object SteamhelperPluginSetting : ReadOnlyPluginConfig("Steamhelper") {
    @ValueDescription("Steam每周榜单获取源")
    val urlWeekly: String by value("https://store.steampowered.com/feeds/weeklytopsellers.xml")

    @ValueDescription("Steam状态与大促情况获取源")
    val urlInfo: String by value("https://keylol.com/")

    @ValueDescription("连接超时时间，单位毫秒")
    val timeout: Int by value(3000)

    @ValueDescription("连接超时时间重试次数")
    val retry: Int by value(3)

    @ValueDescription("推送、订阅信息发送超过指定错误次数后自动删除，成功一次后会重新计数")
    val errors: Int by value(5)

    @ValueDescription("调试模式，一般不会有人开吧")
    val debug: Boolean by value(false)

    @ValueDescription(
        """
        时间输出格式
        默认格式为：2022-02-20 16:00:00 星期日
        时间代码为：yyyy-MM-dd HH:mm:ss EEEE
        详情参考：https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
        """
    )
    val timeFormat: String by value("yyyy-MM-dd HH:mm:ss EEEE")

    @ValueDescription(
        """
        时间输出时区
        默认为：东八区
        """
    )
    val timeZone: String by value("GMT+8:00")

    @ValueDescription(
        """
        大促推送时间，采用Quartz Cron表达式
        可以参考https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
        或者 https://tool.lu/crontab/ 选择类型为Java(Quartz)
        请务必在第二个链接测试后无误再放入配置文件
        默认为0 0 8,20 * * ? ， 意为8点、20点推送
        """
    )
    val timePushSale: String by value("0 0 8,20 * * ?")

    @ValueDescription(
        """周榜推送时间，采用Cron表达式
        默认为0 0 12 ? * MON ， 意为每周一12点发送
    """
    )
    val timePushWeek: String by value("0 0 12 ? * MON")

    @ValueDescription(
        """Epic周免推送时间，采用Cron表达式
        默认为0 0 12 ? * FRI ， 意为每周五12点发送
    """
    )
    val timePushEpic: String by value("0 0 12 ? * FRI")

    @ValueDescription(
        """周榜大促和订阅的游戏优惠数据更新时间间隔，采用Cron表达式
        默认为0 0 * * * ? ， 意为每小时刷新一次（其实Steam数据刷新不频繁不建议高强度刷新）
    """
    )
    val timeRefresh: String by value("0 0 * * * ?")

    @ValueDescription(
        """
        搜索区域设定
        从第一个区域开始，如未找到或错误则在第二个区域搜索
        不要过多，插件搜索理论上最慢速度为超时重试次数*连接超时时间*areasSearch数量
        请严格使用ISO3166双字母缩写
        """
    )
    val areasSearch: MutableList<String> by value(mutableListOf("US", "CN", "JP"))

    @ValueDescription(
        """
        价格区域设定
        比价功能将显示所有在该列表中的区域
        同时第一位将作为插件默认货币以及折扣订阅默认区域
        请严格使用ISO3166双字母缩写
        请务必写作!<大写>!，否则货币方面可能会出问题
        """
    )
    val areasPrice: MutableList<String> by value(mutableListOf("CN", "RU", "TR", "AR"))

    @ValueDescription(
        """
        自定义周榜参数，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <tf>=榜单更新时间（以timeFormat格式）
        <ts>=榜单更新时间（以时间戳的格式）
        <ls>=榜单信息（在下方编辑格式，末尾默认带一个换行）
        <sc>=获取来源
        """
    )
    val messageWeek: String by value("Steam一周销量榜：\n截止时间：<tf>\n<ls>")

    @ValueDescription(
        """
        自定义周榜参数榜单信息，替换messageWeek的<ls>可用参数如下：
        换行请使用\n，其他特殊字符同理，每项都会默认带换行
        <nm>=游戏名称（带排名）
        <lk>=游戏链接
        """
    )
    val messageWeekList: String by value("<nm>")

    @ValueDescription(
        """
        自定义Steam状态信息，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <ss>=商店状态（例：正常）
        <sc>=社区状态（例：正常）
        """
    )
    val messageStatus: String by value("Steam 商店状态 : <ss> | Steam 社区状态 : <sc>")

    @ValueDescription(
        """
        自定义Steam促销信息，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <nm>=促销名称
        <tl>=促销剩余时间（以X天X时X分X秒的格式）
        <tf>=促销时间（以timeFormat格式）
        <ts>=促销时间（以时间戳的格式）
        """
    )
    val messageSale: String by value("<nm><tl>")

    @ValueDescription(
        """
        自定义比价参数，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <id>=SteamAppid
        <nm>=App名称
        <ds>=App介绍
        <pl>=比价价格列表
        """
    )
    val messageCompare: String by value("<nm>(<id>)\n<pl>")

    @ValueDescription(
        """
        自定义比价参数榜单信息，替换messageCompare的<pl>可用参数如下：
        换行请使用\n，其他特殊字符同理，每项都会默认带换行
        <an>=当前区域名称（ISO3166双字母缩写）
        <at>=换算的区域名称（以areasPrice第一位为基准）
        <cr>=货币单位（ISO4217货币代码）
        <ct>=换算的货币单位（以areasPrice第一位为基准）
        <ip>=初始价格
        <it>=换算的初始价格（以areasPrice第一位为基准）
        <ir>=初始价格的相差比例（为(该区域÷areasPrice第一位区域价格)%，不带%号）
        <fp>=最终价格
        <ft>=最终价格换算
        <fr>=最终价格的相差比例（为(该区域÷areasPrice第一位区域价格)%，不带%号）
        <ds>=当前折扣力度
        """
    )
    val messageCompareList: String by value("<an>:<cr><fp>(<ct><ft>)(<fr>%)")

    @ValueDescription(
        """
        自定义比价参数榜单信息(错误信息如免费、锁区、获取错误等)，替换messageCompare的<pl>可用参数如下：
        换行请使用\n，其他特殊字符同理，每项都会默认带换行
        <if>=错误信息如锁区、获取错误、内部错误等
        <an>=当前区域名称（ISO3166双字母缩写）
        <at>=换算的区域名称（以areasPrice第一位为基准）
        <cr>=货币单位（ISO4217货币代码）
        <ct>=换算的货币单位（以areasPrice第一位为基准）
        """
    )
    val messageCompareListError: String by value("<an>:<if>")

    @ValueDescription(
        """
        自定义订阅信息，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <aid>=SteamAppid
        <anm>=App名称
        <ads>=App介绍
        <aif>=App价格变动情况（降价，涨价）
        <aar>=App区域名称（ISO3166双字母缩写）（以areasPrice第一位为基准）
        <acr>=App货币单位（ISO4217货币代码）
        <cip>=当前初始价格
        <cfp>=当前最终价格
        <cds>=当前折扣力度
        <oip>=之前的初始价格
        <ofp>=之前的最终价格
        <ods>=之前的折扣力度
        <rip>=初始价格相差比例（(当前初始价格/之前的初始价格)%，不带%号）
        <rfp>=最终价格相差比例（(当前最终价格/之前的最终价格)%，不带%号）
        """
    )
    val messageSubscribe: String by value("<anm>(<aid>)<aif>\n当前价格：<acr><cfp>(-<cds>%)\n之前价格：<acr><ofp>(-<ods>%)\n相差比例：<rfp>%")

    @ValueDescription(
        """
        自定义搜索参数，可用参数如下：
        换行请使用\n，其他特殊字符同理
        <id>=SteamAppid
        <nm>=App名称
        <ds>=App介绍
        """
    )
    val messageSearch: String by value("<nm>(<id>)\n<ds>")

    @ValueDescription(
        """
        Epic当前限免信息：
        换行请使用\n，其他特殊字符同理
        <cls>=当前限免名单（使用下面messageEpicPromoteList格式）
        <fls>=未来限免名单（使用下面messageEpicPromoteList格式）
        """
    )
    val messageEpicPromote: String by value("Epic本周免费游戏：\n<cls>\n未来免费游戏：\n<fls>")

    @ValueDescription(
        """
        Epic当前限免信息列表：
        换行请使用\n，其他特殊字符同理，每项都会默认带换行
        <nm>=App名称
        <ds>=App介绍
        <tf>=开始时间（以timeFormat格式）
        <ts>=开始时间（以时间戳的格式）
        """
    )
    val messageEpicPromoteList: String by value("<nm>[开始于：<tf>]")
}