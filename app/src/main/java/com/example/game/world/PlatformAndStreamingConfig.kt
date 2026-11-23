package com.example.game.world

import com.example.game.save.GraphicsQuality

enum class PlatformTarget(
    val id: String,
    val displayName: String,
    val memoryBudgetMb: Int,
    val recommendedMaxLod: LodLevel
) {
    ANDROID("android", "Android (GLES 2.0 / Mobile Tier)", 160, LodLevel.LOD_MEDIUM),
    WINDOWS("windows", "Windows Desktop (PC Tier)", 512, LodLevel.LOD_HIGH),
    IOS("ios", "iOS / iPadOS (Metal Tier)", 220, LodLevel.LOD_HIGH)
}

enum class LodLevel(val tier: Int, val label: String, val cullDistanceMeters: Float) {
    LOD_HIGH(0, "LOD0 HIGH", 96f),
    LOD_MEDIUM(1, "LOD1 MED", 64f),
    LOD_LOW(2, "LOD2 LOW", 42f);

    companion object {
        fun forQuality(quality: GraphicsQuality): LodLevel = when (quality) {
            GraphicsQuality.LOW -> LOD_LOW
            GraphicsQuality.MEDIUM -> LOD_MEDIUM
            GraphicsQuality.HIGH -> LOD_HIGH
        }
    }
}

data class StreamingBudgetConfig(
    val platform: PlatformTarget = PlatformTarget.ANDROID,
    val maxActiveDistricts: Int = 1,
    val maxCachedDistricts: Int = 2,
    val memoryBudgetMb: Int = 160,
    val baseLod: LodLevel = LodLevel.LOD_MEDIUM,
    val enableFrustumCulling: Boolean = true
) {
    companion object {
        fun forQuality(
            quality: GraphicsQuality,
            platform: PlatformTarget = PlatformTarget.ANDROID
        ): StreamingBudgetConfig = when (quality) {
            GraphicsQuality.LOW -> StreamingBudgetConfig(
                platform = platform,
                maxActiveDistricts = 1,
                maxCachedDistricts = 1,
                memoryBudgetMb = (platform.memoryBudgetMb * 0.6f).toInt(),
                baseLod = LodLevel.LOD_LOW,
                enableFrustumCulling = true
            )
            GraphicsQuality.MEDIUM -> StreamingBudgetConfig(
                platform = platform,
                maxActiveDistricts = 1,
                maxCachedDistricts = 2,
                memoryBudgetMb = platform.memoryBudgetMb,
                baseLod = LodLevel.LOD_MEDIUM,
                enableFrustumCulling = true
            )
            GraphicsQuality.HIGH -> StreamingBudgetConfig(
                platform = platform,
                maxActiveDistricts = 1,
                maxCachedDistricts = 2,
                memoryBudgetMb = (platform.memoryBudgetMb * 1.4f).toInt(),
                baseLod = LodLevel.LOD_HIGH,
                enableFrustumCulling = true
            )
        }
    }
}

/**
 * Reusable allocation-friendly object pool for projectiles, sparks, and pooled gameplay entities.
 */
class ObjectPool<T>(
    private val maxCapacity: Int = 96,
    private val factory: () -> T,
    private val onReset: (T) -> Unit = {}
) {
    private val freeList = ArrayDeque<T>(maxCapacity)

    var totalCreated: Int = 0
        private set
    var totalRecycledHits: Int = 0
        private set
    val availableCount: Int
        get() = freeList.size

    fun obtain(): T {
        val recycled = freeList.removeLastOrNull()
        return if (recycled != null) {
            totalRecycledHits += 1
            recycled
        } else {
            totalCreated += 1
            factory()
        }
    }

    fun recycle(instance: T) {
        onReset(instance)
        if (freeList.size < maxCapacity) {
            freeList.addLast(instance)
        }
    }

    fun clear() {
        freeList.clear()
    }
}
