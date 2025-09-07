package com.github.evolvedghost.mirai.steamhelper.data

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object SteamConfig : ReadOnlyPluginConfig("SteamConfig") {
    @ValueDescription("""
        Steam的搜索区域，修改可用于寻找锁区App
        常用区域：中国-cn，香港-hk，日本-jp，美国-us
        更多区域查看：https://github.com/EvolvedGhost/Steamhelper/blob/v2-main/docs/area.md
    """)
    val region by value("cn")

    @ValueDescription("""
        Steam的Web API 密钥
        获取链接：https://steamcommunity.com/dev/apikey
    """)
    val steamWebApi by value("")
}