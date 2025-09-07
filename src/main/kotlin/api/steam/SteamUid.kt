package com.github.evolvedghost.mirai.steamhelper.api.steam

import com.github.evolvedghost.mirai.steamhelper.data.SteamConfig.steamWebApi
import com.github.evolvedghost.mirai.steamhelper.utils.NetworkUtil
import kotlinx.serialization.Serializable

class SteamUid(id: String) {
    private var uidUtils: SteamUidUtils

    /**
     * VALID = 1
     * NO_MSG = 0
     * INVALID = -1
     * NOT_FOUND = -2
     * TOKEN_ERR = -3
     * INTERNAL_ERR = -4
     */
    private var valid = 0

    @Serializable
    data class SteamApiResponse(
        val response: ResponseData
    )

    @Serializable
    data class ResponseData(
        val steamid: String? = null,
        val success: Int,
        val message: String? = null
    )

    init {
        uidUtils = runCatching {
            SteamUidUtils(id)
        }.getOrElse {
            SteamUidUtils()
        }

        if (!uidUtils.isValid()) {
            val url =
                "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=${steamWebApi}&vanityurl=${id}"
            val data = runCatching {
                NetworkUtil.json<SteamApiResponse>(url, "SteamUid查询")
            }.getOrNull()

            if (data == null) {
                valid = -3
            } else {
                if (data.response.success == 1 && data.response.steamid != null) {
                    uidUtils = SteamUidUtils(data.response.steamid)
                    if (uidUtils.isValid()) {
                        valid = 1
                    } else {
                        valid = -4
                    }
                } else if (data.response.success == 42) {
                    valid = -2
                } else {
                    valid = -4
                }
            }
        } else {
            valid = 1
        }
    }

    private fun ensureValid(): SteamUidUtils {
        when (valid) {
            -1 -> throw Exception("无效的ID")
            -2 -> throw Exception("找不到该ID")
            -3 -> throw Exception("SteamWebApikey有误")
            -4 -> throw Exception("内部错误")
        }
        return uidUtils
    }

    fun getSteam64(): String {
        return ensureValid().toUint64String()
    }

    fun getSteam3(): String {
        return ensureValid().renderSteam3()
    }

    fun getSteam2(): String {
        return ensureValid().renderSteam2()
    }

    fun getAccountId(): String {
        return ensureValid().getAccountID().toString()
    }

    fun getInviteCode(): String {
        return ensureValid().renderSteamInvite()
    }

    fun getCsCode(): String {
        return ensureValid().renderCsgoFriendCode()
    }

    fun getProfileLink(): String {
        return "https://steamcommunity.com/profiles/${getSteam64()}"
    }

    fun getInviteLink(): String {
        return "https://s.team/p/${getInviteCode()}"
    }

    fun getUserLink(): String {
        return "https://steamcommunity.com/user/${getInviteCode()}"
    }
}