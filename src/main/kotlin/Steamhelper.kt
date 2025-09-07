package com.github.evolvedghost.mirai.steamhelper

import com.github.evolvedghost.mirai.steamhelper.data.GeneralConfig
import com.github.evolvedghost.mirai.steamhelper.data.SteamConfig
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


object Steamhelper : KotlinPlugin(JvmPluginDescription(
    id = "com.github.evolvedghost.mirai.steamhelper.steamhelper",
    name = "SteamHelper",
    version = "2.0.0-Beta01",
) {
    author("EvolvedGhost")
}) {
    override fun onEnable() {
        GeneralConfig.reload()
        GeneralConfig.save()
        SteamConfig.reload()
        SteamConfig.save()
    }

    override fun onDisable() {
    }
}