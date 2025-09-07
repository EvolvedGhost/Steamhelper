package com.github.evolvedghost.mirai.steamhelper.api.steam

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern

/**
 * Kotlin's representation of a SteamID, providing conversion between common formats.
 * Based on the PHP SteamID library by xPaw (https://github.com/xPaw/SteamID.php).
 *
 * @property data The internal 64-bit representation as a BigInteger.
 */
class SteamUidUtils(value: String? = null) {

    private var data: BigInteger = BigInteger.ZERO

    init {
        if (value != null) {
            parseInput(value)
        }
    }

    /**
     * Parses the input string to initialize the SteamID.
     */
    private fun parseInput(value: String) {
        // Try Steam2 format: STEAM_X:Y:Z
        val steam2Matcher = STEAM2_REGEX.matcher(value)
        if (steam2Matcher.matches()) {
            val universeStr = steam2Matcher.group("universe") ?: "1"
            val authServerStr = steam2Matcher.group("authServer") ?: "0"
            val idStr = steam2Matcher.group("id") ?: "0"

            var universe = universeStr.toIntOrNull() ?: UniversePublic
            if (universe == UniverseInvalid) {
                universe = UniversePublic // Handle legacy universe 0
            }

            val authServer = authServerStr.toIntOrNull() ?: 0
            val accountId = idStr.toLongOrNull() ?: 0L

            // Check for max unsigned 32-bit number (approximated with Long)
            if (accountId > 0xFFFFFFFFL) {
                throw IllegalArgumentException("Provided SteamID exceeds max unsigned 32-bit integer.")
            }

            val shiftedAccountId = (accountId shl 1) or authServer.toLong()

            setAccountUniverse(universe)
            setAccountInstance(DesktopInstance)
            setAccountType(TypeIndividual)
            setAccountID(shiftedAccountId)
            return
        }

        // Try Steam3 format: [T:U:A] or [T:U:A:I]
        val steam3Matcher = STEAM3_REGEX.matcher(value)
        if (steam3Matcher.matches()) {
            val typeChar = steam3Matcher.group("type") ?: "i"
            val universeStr = steam3Matcher.group("universe") ?: "0"
            val idStr = steam3Matcher.group("id") ?: "0"
            val instanceStr = steam3Matcher.group("instance")

            val accountId = idStr.toLongOrNull() ?: 0L
            if (accountId > 0xFFFFFFFFL) {
                throw IllegalArgumentException("Provided SteamID exceeds max unsigned 32-bit integer.")
            }

            val universe = universeStr.toIntOrNull() ?: UniverseInvalid

            var instanceId: Int
            var accountType: Int

            when (typeChar) {
                "c" -> {
                    instanceId = InstanceFlagClan
                    accountType = TypeChat
                }
                "L" -> {
                    instanceId = InstanceFlagLobby
                    accountType = TypeChat
                }
                else -> {
                    accountType = ACCOUNT_TYPE_CHARS.entries.find { it.value.toString() == typeChar }?.key ?: TypeInvalid
                    instanceId = when {
                        typeChar == "T" || typeChar == "g" -> AllInstances
                        instanceStr != null -> instanceStr.toIntOrNull() ?: AllInstances
                        typeChar == "U" -> DesktopInstance
                        else -> AllInstances
                    }
                }
            }

            setAccountType(accountType)
            setAccountUniverse(universe)
            setAccountInstance(instanceId)
            setAccountID(accountId)
            return
        }

        // Try numeric (SteamID64)
        if (isNumeric(value)) {
            this.data = BigInteger(value)
            return
        }

        throw IllegalArgumentException("Provided SteamID is invalid: $value")
    }

    /**
     * Renders this instance into its Steam2 "STEAM_" representation.
     */
    fun renderSteam2(): String {
        return when (getAccountType()) {
            TypeInvalid, TypeIndividual -> {
                val universe = getAccountUniverse()
                val accountId = getAccountID().toLong() // Assuming it fits for Steam2
                "STEAM_$universe:${accountId and 1}:${accountId shr 1}"
            }
            else -> toUint64String()
        }
    }

    /**
     * Renders this instance into its Steam3 representation.
     */
    fun renderSteam3(): String {
        val accountInstance = getAccountInstance()
        val accountType = getAccountType()
        val accountTypeChar = ACCOUNT_TYPE_CHARS[accountType] ?: "i"

        var renderInstanceFlag = false
        var finalTypeChar = accountTypeChar

        when (accountType) {
            TypeChat -> {
                when {
                    (accountInstance and InstanceFlagClan) != 0 -> finalTypeChar = 'c'
                    (accountInstance and InstanceFlagLobby) != 0 -> finalTypeChar = 'L'
                    // Add MMS Lobby if needed
                }
            }
            TypeAnonGameServer, TypeMultiseat -> renderInstanceFlag = true
        }

        val base = "[$finalTypeChar:${getAccountUniverse()}:${getAccountID()}"

        return if (renderInstanceFlag) {
            "$base:${accountInstance}]"
        } else {
            "${base}]"
        }
    }

    /**
     * Renders this instance into Steam's new invite code.
     *
     * Invites can be formatted as:
     * http://s.team/p/%s
     * https://steamcommunity.com/user/%s
     *
     * @return A Steam invite code which can be used in a URL.
     *
     * @throws IllegalArgumentException if the account type is not Individual.
     */
    fun renderSteamInvite(): String {
        val accountType = getAccountType()
        if (accountType != TypeInvalid && accountType != TypeIndividual) {
            throw IllegalArgumentException("This can only be used on Individual SteamID.")
        }

        val accountId = getAccountID().toLong()
        var codeHex = accountId.toString(16).lowercase(Locale.getDefault())

        // Translate using the dictionary (simulate strtr)
        val reversedDict = STEAM_INVITE_DICTIONARY.entries.associate { (k, v) -> v to k } // For decoding if needed
        codeHex = codeHex.map { char ->
            STEAM_INVITE_DICTIONARY[char.toString()] ?: char.toString()
        }.joinToString("")

        val length = codeHex.length
        // TODO: We don't know exactly when Valve starts inserting the dash
        // The logic here mimics the PHP example: insert dash around the middle
        if (length > 3) {
            val midPoint = length / 2
            // Using StringBuilder for efficient string manipulation
            val sb = StringBuilder(codeHex)
            sb.insert(midPoint, '-')
            return sb.toString()
        }

        return codeHex
    }


    private fun gmpIntVal(bi: BigInteger): Int {
        return bi.toInt() // BigInteger.toInt() handles truncation if needed
    }

    /**
     * Renders this instance into friend code used by CS:GO.
     * Looks like SUCVS-FADA.
     *
     * Based on <https://github.com/emily33901/go-csfriendcode>
     * and looking at CSGO's client.dll.
     *
     * @return A friend code which can be used in CS:GO.
     *
     * @throws IllegalArgumentException if the account type is not Individual or Invalid.
     */
    fun renderCsgoFriendCode(): String {
        val accountType = getAccountType()
        if (accountType != TypeInvalid && accountType != TypeIndividual) {
            throw IllegalArgumentException("This can only be used on Individual SteamID.")
        }

        val accountId = getAccountID().toLong()

        // Shift by string "CSGO" (0x4353474F00000000)
        // In Kotlin/Java, we work with signed longs, but bitwise ops are the same for lower bits.
        // 0x4353474F00000000L is the correct representation.
        val shiftedAccountId = (0x4353474F00000000L or accountId)

        // Convert it to little-endian byte array (8 bytes)
        val buffer = ByteBuffer.allocate(8)
        buffer.order(ByteOrder.LITTLE_ENDIAN) // Set order to little-endian
        buffer.putLong(shiftedAccountId)
        val littleEndianBytes = buffer.array() // This is the 8-byte LE representation

        // Hash the exported bytes using MD5
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(littleEndianBytes) // MD5 hash of the LE bytes

        // Take the first 4 bytes and convert it back to a number (as little-endian)
        val first4Buffer = ByteBuffer.wrap(hashBytes, 0, 4)
        first4Buffer.order(ByteOrder.LITTLE_ENDIAN)
        val hashValueInt = first4Buffer.int // First 4 bytes interpreted as LE int
        var hashValue = BigInteger.valueOf(hashValueInt.toLong()) // Convert to BigInteger for operations

        var result = BigInteger.ZERO

        for (i in 0 until 8) {
            // Extract 4 bits (a nibble) from accountId at position i
            val idNibble = ((accountId ushr (4 * i)) and 0xFL).toInt()
            val idNibbleBi = BigInteger.valueOf(idNibble.toLong())

            // Extract 1 bit from hashValue at position i
            val hashBit = ((hashValue.toInt() ushr i) and 1)
            val hashBitBi = BigInteger.valueOf(hashBit.toLong())

            // Perform the complex bitwise operations as in the PHP code
            // $a = gmp_or( self::ShiftLeft( $Result, 4 ), $IdNibble );
            val a = result.shiftLeft(4).or(idNibbleBi)

            // $Result = gmp_or( self::ShiftLeft( self::ShiftRight( $Result, 28 ), 32 ), $a );
            val temp1 = result.shiftRight(28).shiftLeft(32)
            result = temp1.or(a)

            // $Result = gmp_or( self::ShiftLeft( self::ShiftRight( $Result, 31 ), 32 ), gmp_or( self::ShiftLeft( $a, 1 ), $HashNibble ) );
            val temp2 = result.shiftRight(31).shiftLeft(32)
            val temp3 = a.shiftLeft(1).or(hashBitBi)
            result = temp2.or(temp3)
        }

        // Is there a better way of doing this in Java/Kotlin?
        // Convert result (BigInteger) to byte array in big-endian, then import as little-endian
        // gmp_export( $Result, 8, GMP_BIG_ENDIAN )
        val resultBytesBE = result.toByteArray() // This is big-endian byte array, possibly with sign byte
        // Ensure it's exactly 8 bytes, padding with leading zeros if necessary, and handle sign
        val paddedBytesBE = if (resultBytesBE.size > 8) {
            // Truncate if too big (shouldn't happen easily with 64-bit operations, but safe)
            resultBytesBE.sliceArray(resultBytesBE.size - 8 until resultBytesBE.size)
        } else {
            // Pad with leading zeros if too small
            ByteArray(8) { idx ->
                if (idx < 8 - resultBytesBE.size) 0 else resultBytesBE[idx - (8 - resultBytesBE.size)]
            }
        }
        // gmp_import( ..., 8, GMP_LITTLE_ENDIAN )
        val resultBytesLE = paddedBytesBE.reversedArray() // Reverse BE to get LE
        result = BigInteger(1, resultBytesLE) // Import as unsigned (signum=1) little-endian bytes

        val base32Chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var friendCode = StringBuilder()

        for (i in 0 until 13) {
            if (i == 4 || i == 9) {
                friendCode.append('-')
            }

            val index = result.and(BigInteger.valueOf(31)).toInt() // gmp_and( $Result, 31 )
            friendCode.append(base32Chars[index])
            result = result.shiftRight(5) // self::ShiftRight( $Result, 5 )
        }

        // Strip the AAAA- prefix (characters 0 to 5: "AAAA-A")
        // The loop produces 13 chars + 2 dashes = 15 chars. Prefix is AAAA-A (0-4 indices)
        // So we take substring from index 5.
        val finalCode = friendCode.substring(5)

        return finalCode
    }



    // --- Getters and Setters for components (using bit manipulation) ---

    fun getAccountID(): Int = get(0, BigInteger("4294967295")).toInt() // 0xFFFFFFFF
    fun getAccountInstance(): Int = get(32, BigInteger("1048575")).toInt() // 0xFFFFF
    fun getAccountType(): Int = get(52, BigInteger("15")).toInt() // 0xF
    fun getAccountUniverse(): Int = get(56, BigInteger("255")).toInt() // 0xFF

    fun setAccountID(value: Long) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw IllegalArgumentException("Account ID must be between 0 and 0xFFFFFFFF")
        }
        set(0, BigInteger("4294967295"), BigInteger.valueOf(value)) // 0xFFFFFFFF
    }

    fun setAccountInstance(value: Int) {
        if (value < 0 || value > 0xFFFFF) { // 0xFFFFF
            throw IllegalArgumentException("Account Instance must be between 0 and 0xFFFFF")
        }
        set(32, BigInteger("1048575"), BigInteger.valueOf(value.toLong())) // 0xFFFFF
    }

    fun setAccountType(value: Int) {
        if (value < 0 || value > 0xF) { // 0xF
            throw IllegalArgumentException("Account Type must be between 0 and 0xF")
        }
        set(52, BigInteger("15"), BigInteger.valueOf(value.toLong())) // 0xF
    }

    fun setAccountUniverse(value: Int) {
        if (value < 0 || value > 0xFF) { // 0xFF
            throw IllegalArgumentException("Account Universe must be between 0 and 0xFF")
        }
        set(56, BigInteger("255"), BigInteger.valueOf(value.toLong())) // 0xFF
    }

    /**
     * Checks if the current SteamID is considered valid.
     */
    fun isValid(): Boolean {
        val accountType = getAccountType()
        val accountUniverse = getAccountUniverse()
        val accountId = getAccountID()
        val accountInstance = getAccountInstance()

        if (accountType <= TypeInvalid || accountType > TypeAnonUser) return false
        if (accountUniverse <= UniverseInvalid || accountUniverse > UniverseDev) return false

        return when (accountType) {
            TypeIndividual -> accountId != 0 && accountInstance <= WebInstance
            TypeClan -> accountId != 0 && accountInstance == 0
            TypeGameServer -> accountId != 0
            else -> true // Basic check for other types
        }
    }

    /**
     * Converts this SteamID to its 64-bit string representation.
     */
    fun toUint64String(): String = data.toString()

    /**
     * Sets the SteamID from a 64-bit integer string.
     */
    fun setFromUint64(value: String) {
        if (isNumeric(value)) {
            this.data = BigInteger(value)
        } else {
            throw IllegalArgumentException("Provided SteamID is not numeric.")
        }
    }

    // --- Private bit manipulation helpers ---
    private fun get(bitOffset: Int, valueMask: BigInteger): BigInteger {
        return data.shiftRight(bitOffset).and(valueMask)
    }

    private fun set(bitOffset: Int, valueMask: BigInteger, value: BigInteger) {
        // data = (data & ~(valueMask << bitOffset)) | ((value & valueMask) << bitOffset)
        val maskShifted = valueMask.shiftLeft(bitOffset)
        val invertedMask = maskShifted.not() // Assuming two's complement, this inverts bits
        val clearBits = data.and(invertedMask)
        val setBits = value.and(valueMask).shiftLeft(bitOffset)
        this.data = clearBits.or(setBits)
    }

    // --- Companion Object for Constants and Static Methods ---
    companion object {
        // --- Constants ---
        const val UniverseInvalid = 0
        const val UniversePublic = 1
        const val UniverseBeta = 2
        const val UniverseInternal = 3
        const val UniverseDev = 4

        const val TypeInvalid = 0
        const val TypeIndividual = 1
        const val TypeMultiseat = 2
        const val TypeGameServer = 3
        const val TypeAnonGameServer = 4
        const val TypePending = 5
        const val TypeContentServer = 6
        const val TypeClan = 7
        const val TypeChat = 8
        const val TypeP2PSuperSeeder = 9
        const val TypeAnonUser = 10

        const val AllInstances = 0
        const val DesktopInstance = 1
        const val ConsoleInstance = 2
        const val WebInstance = 4

        const val InstanceFlagClan = 524288 // (k_unSteamAccountInstanceMask + 1) >> 1
        const val InstanceFlagLobby = 262144 // (k_unSteamAccountInstanceMask + 1) >> 2
        const val InstanceFlagMMSLobby = 131072 // (k_unSteamAccountInstanceMask + 1) >> 3

        // Regex patterns
        private val STEAM2_REGEX = Pattern.compile("""^STEAM_(?<universe>[0-4]):(?<authServer>[0-1]):(?<id>0|[1-9][0-9]{0,9})$""")
        private val STEAM3_REGEX = Pattern.compile(
            """^\[(?<type>[AGMPCgcLTIUai]):(?<universe>[0-4]):(?<id>0|[1-9][0-9]{0,9})(?::(?<instance>[0-9]+))?\]$"""
        )

        // Mapping for Steam3 characters
        private val ACCOUNT_TYPE_CHARS = mapOf(
            TypeAnonGameServer to 'A',
            TypeGameServer to 'G',
            TypeMultiseat to 'M',
            TypePending to 'P',
            TypeContentServer to 'C',
            TypeClan to 'g',
            TypeChat to 'T', // Lobby chat is 'L', Clan chat is 'c'
            TypeInvalid to 'I',
            TypeIndividual to 'U',
            TypeAnonUser to 'a',
        )

        // List of replacement hex characters used in /user/ URLs
        private val STEAM_INVITE_DICTIONARY = mapOf(
            "0" to "b",
            "1" to "c",
            "2" to "d",
            "3" to "f",
            "4" to "g",
            "5" to "h",
            "6" to "j",
            "7" to "k",
            "8" to "m",
            "9" to "n",
            "a" to "p",
            "b" to "q",
            "c" to "r",
            "d" to "t",
            "e" to "v",
            "f" to "w",
        )

        /**
         * Checks if a string represents a positive integer.
         */
        private fun isNumeric(n: String): Boolean {
            return n.matches(Regex("^[1-9][0-9]{0,19}$")) // Approximate check
        }

        /**
         * Constructs a SteamID from an Account ID, setting default values for Individual type in Public universe.
         */
        fun fromAccountID(accountId: Long): SteamUidUtils {
            return SteamUidUtils().apply {
                setAccountID(accountId)
                setAccountUniverse(UniversePublic)
                setAccountInstance(DesktopInstance)
                setAccountType(TypeIndividual)
            }
        }

        /**
         * Static method to convert an Account ID directly to SteamID64 string.
         */
        fun accountIdToUint64(accountId: Long): String {
            return fromAccountID(accountId).toUint64String()
        }

        /**
         * Static method to convert an Account ID directly to Steam3 string.
         */
        fun renderAccountId(accountId: Long): String {
            return fromAccountID(accountId).renderSteam3()
        }
    }
}

//// Example usage (can be placed in a main function or test)
//fun main() {
//    try {
//        // From SteamID64
//        val sid64 = SteamUidUtils("76561198138515863")
//        println("From 64: ${sid64.renderSteam2()}, ${sid64.renderSteam3()}")
//
//        // From Steam2
//        val sid2 = SteamUidUtils("STEAM_1:1:89125067")
//        println("From Steam2: ${sid2.toUint64String()}, ${sid2.renderSteam3()}")
//
//        // From Steam3
//        val sid3 = SteamUidUtils("[U:1:178250135]")
//        println("From Steam3: ${sid3.toUint64String()}, ${sid3.renderSteam2()}")
//
//        // From Account ID
//        val sidFromAcc = SteamUidUtils.fromAccountID(178250135)
//        println("From AccID: ${sidFromAcc.toUint64String()}, ${sidFromAcc.renderSteam2()}")
//
//        // Static methods
//        println("Static AccID->64: ${SteamUidUtils.accountIdToUint64(178250135)}")
//        println("Static AccID->3: ${SteamUidUtils.renderAccountId(178250135)}")
//
//        // Validation
//        println("Is Valid (64): ${sid64.isValid()}")
//        println("Is Valid (2): ${sid2.isValid()}")
//        println("Is Valid (3): ${sid3.isValid()}")
//
//        println(sid64.renderCsgoFriendCode())
//        println(sid64.renderSteamInvite())
//
//    } catch (e: Exception) {
//        println("Error: ${e.message}")
//    }
//}