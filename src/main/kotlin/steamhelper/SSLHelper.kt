package com.evolvedghost.mirai.steamhelper.steamhelper

import com.evolvedghost.mirai.steamhelper.SteamhelperPluginSetting
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
 */
class SSLHelper {
    fun getDocument(url: String): Document {
        for (i in 1..SteamhelperPluginSetting.retry) {
            try {
                return Jsoup.connect(url).ignoreHttpErrors(true).sslSocketFactory(socketFactory())
                    .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout).get()
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                if (i == SteamhelperPluginSetting.retry) throw e
            }
        }
        return Jsoup.connect(url).ignoreHttpErrors(true).sslSocketFactory(socketFactory())
            .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout).get()
    }

    fun getBody(url: String): String {
        for (i in 1..SteamhelperPluginSetting.retry) {
            try {
                return Jsoup.connect(url).ignoreHttpErrors(true).sslSocketFactory(socketFactory())
                    .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout)
                    .ignoreContentType(true).execute().body()
            } catch (e: Exception) {
                if (SteamhelperPluginSetting.debug) e.printStackTrace()
                if (i == SteamhelperPluginSetting.retry) throw e
            }
        }
        return Jsoup.connect(url).ignoreHttpErrors(true).sslSocketFactory(socketFactory())
            .header("Accept-Language", "zh-cn").timeout(SteamhelperPluginSetting.timeout).ignoreContentType(true)
            .execute().body()
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