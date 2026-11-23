package com.example.game.ai

import com.example.game.engine.Aabb
import com.example.game.engine.Vec3
import com.example.game.localization.AppLanguage
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class EnemyAiState(val labelEn: String, val labelBn: String) {
    IDLE("Idle", "নিষ্ক্রিয়"),
    PATROL("Patrol", "টহল"),
    DETECT("Detect", "শনাক্ত"),
    CHASE("Chase", "ধাওয়া"),
    ATTACK("Attack", "আক্রমণ"),
    RETREAT("Retreat", "পিছু হটা"),
    SEARCH("Search", "অনুসন্ধান"),
    DEAD("Dead", "নিহত");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

enum class AiDifficulty(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val healthMultiplier: Float,
    val damageMultiplier: Float,
    val fireCooldownScale: Float,
    val detectionRange: Float
) {
    EASY("easy", "Recruit (Easy)", "সহজ", 0.75f, 0.65f, 1.35f, 16f),
    NORMAL("normal", "Tactical (Normal)", "স্বাভাবিক", 1.0f, 1.0f, 1.0f, 22f),
    HARD("hard", "Veteran (Hard)", "কঠিন", 1.35f, 1.35f, 0.78f, 28f);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): AiDifficulty = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

data class EnemyAiAgent(
    val id: Int,
    val callsign: String,
    val patrolPoints: List<Vec3>,
    var patrolIndex: Int = 0,
    var pos: Vec3,
    var yawRad: Float = 0f,
    var hp: Int,
    val maxHp: Int,
    var aiState: EnemyAiState = EnemyAiState.PATROL,
    var stateTimerSec: Float = 0f,
    var fireCooldownSec: Float = 1.2f,
    var lastKnownPlayerPos: Vec3? = null,
    var walkPhase: Float = 0f
) {
    val isAlive: Boolean get() = aiState != EnemyAiState.DEAD && hp > 0
}

/**
 * Lightweight 2D NavMesh-equivalent grid over the Dhaka arena for obstacle-aware AI steering & line-of-sight.
 */
class DhakaNavGrid(
    private val colliders: List<Aabb>,
    private val halfSize: Float = 31f,
    private val cellSize: Float = 2.0f
) {
    private val gridDim = ((halfSize * 2f) / cellSize).toInt()
    private val blocked = BooleanArray(gridDim * gridDim)

    init {
        for (gx in 0 until gridDim) {
            for (gz in 0 until gridDim) {
                val wx = -halfSize + (gx + 0.5f) * cellSize
                val wz = -halfSize + (gz + 0.5f) * cellSize
                val hit = colliders.any { c ->
                    c.maxY > 0.6f &&
                        wx >= c.minX - 0.55f && wx <= c.maxX + 0.55f &&
                        wz >= c.minZ - 0.55f && wz <= c.maxZ + 0.55f
                }
                blocked[gz * gridDim + gx] = hit
            }
        }
    }

    fun hasLineOfSight(from: Vec3, to: Vec3): Boolean {
        val delta = to - from
        val dist = delta.length()
        if (dist < 0.5f) return true
        val dir = delta.normalized()
        for (c in colliders) {
            if (c.maxY < 1.1f) continue // AI can see over low waist cover
            val hitT = c.intersectRay(from, dir, dist)
            if (hitT != null && hitT < dist - 0.25f) {
                return false
            }
        }
        return true
    }

    fun isWalkable(wx: Float, wz: Float): Boolean {
        val gx = ((wx + halfSize) / cellSize).toInt().coerceIn(0, gridDim - 1)
        val gz = ((wz + halfSize) / cellSize).toInt().coerceIn(0, gridDim - 1)
        return !blocked[gz * gridDim + gx]
    }

    /**
     * Steers toward target while avoiding blocked NavGrid cells using local tangential probe angles.
     */
    fun computeSteeringDirection(currentPos: Vec3, targetPos: Vec3): Vec3 {
        val toTarget = Vec3(targetPos.x - currentPos.x, 0f, targetPos.z - currentPos.z)
        val dist = toTarget.length()
        if (dist < 0.05f) return Vec3(0f, 0f, 0f)
        val baseYaw = atan2(toTarget.x, -toTarget.z)

        val probeAngles = floatArrayOf(0f, 0.45f, -0.45f, 0.9f, -0.9f, 1.4f, -1.4f)
        for (offset in probeAngles) {
            val yaw = baseYaw + offset
            val dir = Vec3(sin(yaw), 0f, -cos(yaw))
            val probe = currentPos + dir * 1.4f
            if (abs(probe.x) < halfSize && abs(probe.z) < halfSize && isWalkable(probe.x, probe.z)) {
                return dir
            }
        }
        return toTarget.normalized()
    }
}
