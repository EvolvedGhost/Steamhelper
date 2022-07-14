package com.evolvedghost.mirai.steamhelper.utils

import com.evolvedghost.mirai.steamhelper.Steamhelper
import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import net.mamoe.mirai.utils.info

fun pluginWarn(info: String, e: String) {
    val sb = StringBuilder(info)
    when (SteamhelperPluginSetting.warningLevel) {
        -1 -> {
            return
        }
        1 -> {
            sb.append(exceptionTranslator(e))
            sb.append("|")
            sb.append(e)
        }
        else -> {
            sb.append(exceptionTranslator(e))
        }
    }
    Steamhelper.logger.info { sb.toString() }
}

fun pluginLogger(info: String) {
    if (SteamhelperPluginSetting.warningLevel != -1) {
        Steamhelper.logger.info { info }
    }
}

fun pluginExceptionHandler(info: String, e: Exception) {
    if (SteamhelperPluginSetting.warningLevel == 2) {
        Steamhelper.logger.info { "$info 发生错误：$e" }
    }
    if (SteamhelperPluginSetting.debug) {
        e.printStackTrace()
    }
}

fun pluginDebugHandler(info: String) {
    if (SteamhelperPluginSetting.debug) {
        println(info)
    }
}

// 提供给一些建议的错误提示
private fun exceptionTranslator(e: String): String {
    return if (e.contains("java.net.SocketTimeoutException: Read timed out"))
        "请求已超时，请检查是否能够流畅访问相关网站"
    else if (e.contains("java.net.ConnectException: Connection refused"))
        "请求被拒绝，请检查网络是否正常或代理是否正常"
    else if (e.contains("unable to find valid certification path to requested target"))
        "证书错误，请检查网络环境或者切换到请求模式1"
    else if (e.contains("java.net.SocketTimeoutException: Connect timed out"))
        "连接已超时，请检查是否能够流畅访问相关网站"
    else if (e.contains("java.net.UnknownHostException"))
        "无法解析域名，请检查获取源是否正确"
    else if (e.contains("java.lang.IllegalArgumentException: Illegal pattern character"))
        "模板格式错误，请检查timeFormat的格式"
    else if (e.contains("java.lang.RuntimeException: CronExpression"))
        "Cron格式错误，请检查Config中所有Cron的格式"
    else "未知错误：$e"
}