package com.github.evolvedghost.mirai.steamhelper.utils

import com.github.evolvedghost.mirai.steamhelper.Steamhelper
import com.github.evolvedghost.mirai.steamhelper.data.GeneralConfig.logLevel

object Logger {
    fun debug(location: String, message: Any, extraInfo: String = "") {
        if (logLevel >= 4) {
            Steamhelper.logger.debug("[$location] $extraInfo : $message")
        }
    }

    fun info(location: String, message: Any, extraInfo: String = "") {
        if (logLevel >= 3) {
            Steamhelper.logger.info("[$location] $extraInfo : $message")
        }
    }

    fun warn(location: String, message: Any, extraInfo: String = "") {
        if (logLevel >= 2) {
            Steamhelper.logger.warning("[$location] $extraInfo : $message")
        }
    }

    fun error(location: String, message: Any, extraInfo: String = "") {
        if (logLevel >= 1) {
            Steamhelper.logger.error("[$location] $extraInfo : $message")
        }
    }
}