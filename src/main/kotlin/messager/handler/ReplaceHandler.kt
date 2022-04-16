package com.evolvedghost.mirai.steamhelper.messager.handler

/**
 * 用于替换文本串
 * @param text String 要被替换的文本
 * @param keywords Array<String> 替换关键字
 * @param targets Array<String?> 替换结果
 * @return String
 */
fun replace(text: String, keywords: Array<String>, targets: Array<String?>): String {
    return if (keywords.size == targets.size) {
        var finalText = text
        for (i in keywords.indices) {
            finalText = finalText.replace(keywords[i], targets[i].toString())
        }
        finalText
    } else "错误"
}