package com.github.evolvedghost.mirai.steamhelper

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin


object Steamhelper : KotlinPlugin(JvmPluginDescription(
    id = "com.github.evolvedghost.mirai.steamhelper.steamhelper",
    name = "SteamHelper",
    version = "2.0.0",
) {
    author("EvolvedGhost")
}) {
    override fun onEnable() {
    }

    override fun onDisable() {
    }
}