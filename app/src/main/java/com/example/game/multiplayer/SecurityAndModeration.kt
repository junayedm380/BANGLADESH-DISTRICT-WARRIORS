package com.example.game.multiplayer

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.sqrt

data class ServerAuditLogEntry(
    val timestampMs: Long,
    val category: String,
    val playerId: String,
    val detail: String
)

enum class ReportReason(val id: String, val labelEn: String, val labelBn: String) {
    ABUSIVE_CHAT("abuse_chat", "Abusive / Toxic Chat", "অপমানজনক চ্যাট"),
    CHEATING_SPEED("cheating", "Suspected Cheating / Speedhack", "প্রতারণা / স্পিডহ্যাক"),
    HARASSMENT("harassment", "Harassment / Griefing", "হয়রানি"),
    SPAM("spam", "Spam / Flooding", "স্প্যাম")
}

object PasswordHasher {
    private val secureRandom = SecureRandom()
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return try {
            val spec = PBEKeySpec(password.toCharArray(), hexToBytes(saltHex), ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            // Fallback SHA-256 salted hash if algorithm unavailable in minimal JVM test profile
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(saltHex.toByteArray())
            digest.update(password.toByteArray())
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val actual = hashPassword(password, saltHex)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHashHex.toByteArray())
    }

    private fun hexToBytes(hex: String): ByteArray {
        if (hex.length % 2 != 0) return hex.toByteArray()
        return ByteArray(hex.length / 2) { idx ->
            hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }
}

object InputSanitizer {
    private val usernameRegex = Regex("^[a-zA-Z0-9_\\-]{3,20}$")
    private val roomCodeRegex = Regex("^[A-Z0-9\\-]{4,10}$")
    private val blockedTerms = listOf("cheattool", "aimbot", "hackserver", "slur_placeholder")

    fun isValidUsername(raw: String): Boolean = usernameRegex.matches(raw.trim())

    fun isValidPassword(raw: String): Boolean = raw.length in 6..64

    fun isValidRoomCode(raw: String): Boolean = roomCodeRegex.matches(raw.trim().uppercase())

    fun sanitizeChatMessage(raw: String): String {
        var cleaned = raw.replace("\n", " ").replace("\r", " ").trim().take(160)
        for (term in blockedTerms) {
            if (cleaned.contains(term, ignoreCase = true)) {
                cleaned = cleaned.replace(term, "***", ignoreCase = true)
            }
        }
        return cleaned
    }

    fun containsBlockedContent(raw: String): Boolean =
        blockedTerms.any { raw.contains(it, ignoreCase = true) }
}

class FixedWindowRateLimiter(
    private val minIntervalMs: Long
) {
    private val lastActionMap = ConcurrentHashMap<String, Long>()

    fun allow(key: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val prev = lastActionMap[key] ?: 0L
        return if (nowMs - prev >= minIntervalMs) {
            lastActionMap[key] = nowMs
            true
        } else {
            false
        }
    }
}

/**
 * Server-side authority validator ensuring clients can never forge damage, XP, credits, or movement teleports.
 */
object ServerAuthorityValidator {
    const val MAX_PLAYER_SPEED_MPS = 9.5f
    const val MAX_ARENA_COORD = 31.4f
    const val MAX_REWARD_XP_PER_MATCH = 600
    const val MAX_REWARD_TAKA_PER_MATCH = 1500

    fun validateStepMovement(
        prevX: Float,
        prevZ: Float,
        requestedX: Float,
        requestedZ: Float,
        dtSec: Float
    ): Pair<Float, Float> {
        val clampedDt = dtSec.coerceIn(0.01f, 0.25f)
        val maxDist = MAX_PLAYER_SPEED_MPS * clampedDt + 0.35f
        val dx = requestedX - prevX
        val dz = requestedZ - prevZ
        val dist = sqrt(dx * dx + dz * dz)
        val (safeX, safeZ) = if (dist > maxDist && dist > 1e-4f) {
            val scale = maxDist / dist
            Pair(prevX + dx * scale, prevZ + dz * scale)
        } else {
            Pair(requestedX, requestedZ)
        }
        return Pair(
            safeX.coerceIn(-MAX_ARENA_COORD, MAX_ARENA_COORD),
            safeZ.coerceIn(-MAX_ARENA_COORD, MAX_ARENA_COORD)
        )
    }

    fun clampVerifiedMatchReward(xp: Int, taka: Int, won: Boolean): Pair<Int, Int> {
        val baseXp = if (won) xp else (xp / 3)
        val baseTaka = if (won) taka else (taka / 4)
        return Pair(
            baseXp.coerceIn(0, MAX_REWARD_XP_PER_MATCH),
            baseTaka.coerceIn(0, MAX_REWARD_TAKA_PER_MATCH)
        )
    }
}
