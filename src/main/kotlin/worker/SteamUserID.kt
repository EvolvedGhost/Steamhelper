package com.evolvedghost.mirai.steamhelper.worker

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class SteamUserID {
    private var uidFlagA: Int = -1
    private var uidFlagB: Int = -1
    var isInit = false
    fun getSteamAccountId(): Long {
        return (uidFlagB * 2 + uidFlagA).toLong()
    }

    fun getSteam3Id(): String {
        return "[U:1:" + getSteamAccountId() + "]"
    }

    fun getSteam32Id(): String {
        return "STEAM_1:$uidFlagA:$uidFlagB"
    }

    fun getSteam64Id(): Long {
        return 76561197960265728 + (uidFlagB * 2) + uidFlagA
    }

    fun getSteamFiveMId(): String {
        return "steam:" + java.lang.Long.toHexString(getSteam64Id())
    }

    fun getSteamInviteId(): String {
        val hex = java.lang.Long.toHexString(getSteamAccountId())
        val result = StringBuilder()
        val dictionary = mapOf(
            '0' to 'b',
            '1' to 'c',
            '2' to 'd',
            '3' to 'f',
            '4' to 'g',
            '5' to 'h',
            '6' to 'j',
            '7' to 'k',
            '8' to 'm',
            '9' to 'n',
            'a' to 'p',
            'b' to 'q',
            'c' to 'r',
            'd' to 't',
            'e' to 'v',
            'f' to 'w',
        )
        for (i in 0..6) {
            if(i == 3){
                result.append('-')
            }
            val nextChar = dictionary[hex[i]] ?: return ""
            result.append(nextChar)
        }
        return result.toString()
    }

    // Modify From https://github.com/emily33901/go-csfriendcode
    fun getSteamCSGOId(): String {
        val id = getSteamAccountId()
        val arr = ByteBuffer.allocate(java.lang.Long.BYTES).putLong(id or 0x4353474F00000000).array()
        arr.reverse()
        val hash =
            ByteBuffer.wrap(MessageDigest.getInstance("MD5").digest(arr)).order(ByteOrder.LITTLE_ENDIAN).long.toUInt()
        var id64 = getSteam64Id()
        var r: ULong = 0u
        for (i in 0..7) {
            val idNibble = (id64 and 0xF).toByte()
            id64 = id64 shr 4
            val hashNibble = (hash shr i) and 1u
            val a = (r shl 4).toUInt() or idNibble.toUInt()
            r = ((r shr 28).toUInt().toULong() shl 32) or a.toULong()
            r = ((r shr 31).toUInt().toULong() shl 32) or ((a shl 1) or hashNibble).toULong()
        }
        val inputBytes = ByteBuffer.allocate(java.lang.Long.BYTES).putLong(r.toLong()).array()
        inputBytes.reverse()
        var input = ByteBuffer.wrap(inputBytes).long.toULong()
        val result = StringBuilder()
        val alnum = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        for (i in 0..12) {
            if (i == 4 || i == 9) {
                result.append('-')
            }
            result.append(alnum[(input and 31u).toInt()])
            input = input shr 5
        }
        var code = result.toString()
        if (code.startsWith("AAAA-")) {
            code = code.substring(5)
        }
        return code
    }

    fun setSteamAccountId(id: Long): Boolean {
        var tempId = id;
        uidFlagA = (tempId % 2).toInt()
        tempId -= uidFlagA
        uidFlagB = (tempId / 2).toInt()
        return true
    }

    fun setSteamAccountId(id: String): Boolean {
        return setSteamAccountId(id.toLong())
    }

    fun setSteam3Id(id: String): Boolean {
        val value = Regex("(?<=U:1:).*?(?=]|$)").find(id, 0)?.value
        if (value != null) {
            setSteamAccountId(value)
        } else {
            return false
        }
        return true
    }

    fun setSteam32Id(id: String): Boolean {
        val stringFlagA = Regex("(?<=STEAM_0:|STEAM_1:).*?(?=:)").find(id, 0)?.value
        if (stringFlagA != null) {
            val tempFlagA = stringFlagA.toInt()
            if (tempFlagA == 0 || tempFlagA == 1) {
                uidFlagA = tempFlagA
            } else {
                return false
            }
            val stringFlagB = Regex("(?<=STEAM_0:$uidFlagA:|STEAM_1:$uidFlagA:).*?(?=$)").find(id, 0)?.value
            if (stringFlagB != null) {
                uidFlagB = stringFlagB.toInt()
            } else {
                return false
            }
        } else {
            return false
        }
        return true
    }

    fun setSteam64Id(id: String): Boolean {
        return setSteam64Id(id.toLong())
    }

    fun setSteam64Id(id: Long): Boolean {
        uidFlagA = (id % 2).toInt()
        uidFlagB = ((id - uidFlagA - 76561197960265728) / 2).toInt()
        return true
    }

    fun setSteamFiveMId(id: String): Boolean {
        setSteam64Id(java.lang.Long.parseLong(id.replace("steam:", ""), 16))
        return true
    }

    fun setSteamIdAuto(id: String): Boolean {
        return try {
            if (id.contains("U:1")) {
                setSteam3Id(id)
            } else if (id.contains("STEAM_")) {
                setSteam32Id(id)
            } else if (id.contains("steam:")) {
                setSteamFiveMId(id)
            } else if (id.length == 17) {
                setSteam64Id(id)
            } else {
                setSteamAccountId(id)
            }
        } catch (e: Exception) {
            false
        }
    }
}