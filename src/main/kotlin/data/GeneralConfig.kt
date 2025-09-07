package com.github.evolvedghost.mirai.steamhelper.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object GeneralConfig : ReadOnlyPluginConfig("GeneralConfig") {
    @ValueDescription("""
        插件日志等级，越高日志越多
        但仍然受到Mirai的Logger.yml控制（方便调试罢了）
        0=不通知，1=error，2=warning，3=info，4=debug
    """)
    val logLevel by value(1)

    @ValueDescription("""
        Api获取的超时时间，单位: ms
    """)
    val timeout by value(4000)

    @ValueDescription("""
        Api获取的重试次数，最大等待时间为timeout×retry
    """)
    val retry by value(3)

    @ValueDescription("""
        可用的代理链接，留空则不使用代理
        鉴于Steam的链接情况建议您使用代理
        HTTP代理示例：http://127.0.0.1:8080
        SOCKS代理示例：socks://127.0.0.1:1080
    """)
    val proxy by value("")
}