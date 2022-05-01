package com.evolvedghost.mirai.steamhelper.utils

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 代码修改自https://stackoverflow.com/questions/40742380/
 * 目的是为了解决使用SteamCommunity 302的情况下的证书错误问题
 * 以及代理网络的处理
 */
class SSLHelper {
    fun getDocument(url: String): Document {
        for (i in 1..SteamhelperPluginSetting.retry) {
            try {
                return request(url).get()
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                if (i == SteamhelperPluginSetting.retry) throw e
            }
        }
        return request(url).get()
    }

    fun getBody(url: String): String {
        for (i in 1..SteamhelperPluginSetting.retry) {
            try {
                return request(url).ignoreContentType(true).execute().body()
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                if (i == SteamhelperPluginSetting.retry) throw e
            }
        }
        return request(url).ignoreContentType(true).execute().body()
    }

    private fun request(url: String): Connection {
        return when (SteamhelperPluginSetting.requestMode) {
            1 -> { // 忽略证书错误
                Jsoup.connect(url).ignoreHttpErrors(true).sslSocketFactory(socketFactory())
                    .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout)
            }
            2 -> { // 使用网络代理
                Jsoup.connect(url).proxy(SteamhelperPluginSetting.proxyUrl, SteamhelperPluginSetting.proxyPort)
                    .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout)
            }
            else -> { // 正常的网络连接
                Jsoup.connect(url).header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout)
            }
        }
    }

    companion object {
        @JvmStatic
        fun socketFactory(): SSLSocketFactory {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })
            return try {
                val ssl = SSLContext.getInstance("SSL")
                ssl.init(null, trustAllCerts, SecureRandom())
                ssl.socketFactory
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                throw e
            }
        }
    }
}