package com.github.evolvedghost.mirai.steamhelper.utils

import com.github.evolvedghost.mirai.steamhelper.data.GeneralConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.*

object NetworkUtil {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0"
    private const val ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9"

    fun data(url: String, location: String = "NetworkUtil.data"): String {
        val proxy = createProxy(location)

        repeat(GeneralConfig.retry) { attempt ->
            try {
                val connection = if (proxy != null) {
                    URL(url).openConnection(proxy)
                } else {
                    URL(url).openConnection()
                }.apply {
                    connectTimeout = GeneralConfig.timeout
                    readTimeout = GeneralConfig.timeout
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
                }

                val html = connection.getInputStream().bufferedReader().use { it.readText() }
                return html
            } catch (e: ConnectException) {
                handleException(e, attempt + 1, location, proxy, "连接错误")
            } catch (e: SocketException) {
                handleException(e, attempt + 1, location, proxy, "套接字错误")
            } catch (e: UnknownHostException) {
                handleException(e, attempt + 1, location, proxy, "DNS解析错误")
            } catch (e: Exception) {
                if (attempt + 1 == GeneralConfig.retry) {
                    Logger.warn(location, e, "无法访问，已尝试${attempt + 1}次")
                }
            }
        }

        throw IOException("$location: 连接失败，已重试${GeneralConfig.retry}次")
    }

    inline fun<reified T> json(url: String, location: String = "NetworkUtil.json"): T {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        return json.decodeFromString(serializer<T>(), data(url, location))
    }

    fun document(url: String, location: String = "NetworkUtil.document"): Document {
        val html = data(url, location)
        return Jsoup.parse(html, url)
    }

    private fun createProxy(location: String): Proxy? {
        if (GeneralConfig.proxy.isBlank()) return null

        return try {
            val uri = URI(GeneralConfig.proxy)
            val scheme = uri.scheme?.lowercase() ?: throw IllegalArgumentException("缺少协议")
            val hostname = uri.host ?: throw IllegalArgumentException("缺少主机名")
            val port = uri.port.takeIf { it != -1 } ?: throw IllegalArgumentException("缺少端口")

            when (scheme) {
                "http" -> Proxy(Proxy.Type.HTTP, InetSocketAddress(hostname, port))
                "socks" -> Proxy(Proxy.Type.SOCKS, InetSocketAddress(hostname, port))
                else -> throw IllegalArgumentException("不支持的代理类型: $scheme")
            }
        } catch (e: Exception) {
            Logger.error(location, e, "代理配置疑似错误")
            null
        }
    }

    private fun handleException(
        e: Exception,
        attempt: Int,
        location: String,
        proxy: Proxy?,
        baseMessage: String
    ) {
        if (attempt != GeneralConfig.retry) return

        val proxyHint = when (proxy?.type()) {
            Proxy.Type.HTTP -> "，可能为HTTP代理错误"
            Proxy.Type.SOCKS -> "，可能为SOCKS代理错误"
            else -> ""
        }

        val dnsHint = if (e is UnknownHostException) "，请检查网络连接和DNS设置" else ""

        val message = buildString {
            append(baseMessage)
            append("，已尝试${attempt}次")
            append(proxyHint)
            append(dnsHint)
        }

        Logger.error(location, e, message)
    }
}