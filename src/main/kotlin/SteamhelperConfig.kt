package com.evolvedghost.mirai.steamhelper

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object SteamhelperPluginSetting : ReadOnlyPluginConfig("Steamhelper") {
    @ValueDescription("Steam每周榜单获取源")
    val urlWeekly: String by value("https://store.steampowered.com/feeds/weeklytopsellers.xml")

    @ValueDescription("Steam状态与大促情况获取源")
    val urlInfo: String by value("https://keylol.com/")

    @ValueDescription(
        """
        网络请求模式
        0=正常的网络连接，适用于可流畅访问Steam的网络（可能比1更安全）
        1=忽略证书错误的网络连接，适用于使用例如Steamcommunity302的网络连接
        2=代理的网络连接，需要在下方配置HTTP代理
        """
    )
    val requestMode: Int by value(1)

    @ValueDescription("HTTP代理的链接[网络请求模式2下使用]")
    val proxyUrl: String by value("127.0.0.1")

    @ValueDescription("HTTP代理的端口[网络请求模式2下使用]")
    val proxyPort: Int by value(10809)

    @ValueDescription("连接超时时间，单位毫秒")
    val timeout: Int by value(3000)

    @ValueDescription("连接超时时间重试次数")
    val retry: Int by value(3)

    @ValueDescription("推送、订阅信息发送超过指定错误次数后自动删除，成功一次后会重新计数")
    val errors: Int by value(5)

    @ValueDescription(
        """
        插件控制台提醒级别
        -1=关闭提示，插件将完全静默不输出任何信息
        0=一般的提示，会给出错误等可能的解决方法
        1=在0的基础上提供Exception头
        2=在0的基础上对每一次错误都输出Exception头
        """
    )
    val warningLevel: Int by value(1)

    @ValueDescription("调试模式，会输出所有错误的printStackTrace，如非对插件debug请勿打开")
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
        时间输出语言，采用ISO639格式
        默认为：中文
        """
    )
    val timeLanguage: String by value("zh")

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
        <lk>=App链接（如果为神秘游戏会输出未知）
        <tf>=开始时间（以timeFormat格式）
        <ts>=开始时间（以时间戳的格式）
        """
    )
    val messageEpicPromoteList: String by value("<nm>[开始于：<tf>]")
}