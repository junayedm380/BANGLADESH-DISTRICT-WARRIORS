package com.example.game.world

import com.example.game.districts.DistrictCatalog
import com.example.game.engine.DhakaWorldGeometry
import com.example.game.engine.Vec3
import com.example.game.save.GraphicsQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

enum class StreamingState {
    IDLE,
    LOADING,
    LOADED,
    UNLOADING
}

data class StreamingTelemetry(
    val activeDistrictId: String = "dhaka",
    val loadedDistrictIds: List<String> = listOf("dhaka"),
    val cachedCount: Int = 1,
    val status: StreamingState = StreamingState.IDLE,
    val estimatedMemoryMb: Float = 16.0f,
    val budgetMb: Int = 160,
    val activeLod: LodLevel = LodLevel.LOD_MEDIUM,
    val visibleChunkCount: Int = 2,
    val totalChunkCount: Int = 2,
    val totalUnloads: Int = 0,
    val lastLoadTimeMs: Long = 0L
)

/**
 * Manages asynchronous district environment loading, LRU unloading under memory budgets,
 * distance-based LOD selection, and frustum/distance culling so all 64 districts never
 * sit in memory simultaneously.
 */
class WorldStreamingManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, DhakaWorldGeometry>(4, 0.75f, true)

    private var activeQuality: GraphicsQuality = GraphicsQuality.MEDIUM
    private var platformTarget: PlatformTarget = PlatformTarget.ANDROID
    private var budget: StreamingBudgetConfig = StreamingBudgetConfig.forQuality(activeQuality, platformTarget)

    private val _telemetry = MutableStateFlow(StreamingTelemetry())
    val telemetry: StateFlow<StreamingTelemetry> = _telemetry.asStateFlow()

    val loadedDistrictIds: List<String>
        get() = synchronized(cache) { cache.keys.toList() }

    val loadedCount: Int
        get() = synchronized(cache) { cache.size }

    fun configureQuality(quality: GraphicsQuality, platform: PlatformTarget = platformTarget) {
        activeQuality = quality
        platformTarget = platform
        budget = StreamingBudgetConfig.forQuality(quality, platform)
        synchronized(cache) {
            evictToBudgetLocked(preserveDistrictId = _telemetry.value.activeDistrictId)
            publishTelemetryLocked(_telemetry.value.activeDistrictId, StreamingState.LOADED)
        }
    }

    suspend fun streamToDistrict(
        districtId: String,
        quality: GraphicsQuality = activeQuality
    ): DhakaWorldGeometry = mutex.withLock {
        val resolvedId = DistrictCatalog.getById(districtId).id
        activeQuality = quality
        budget = StreamingBudgetConfig.forQuality(quality, platformTarget)
        val startMs = System.currentTimeMillis()

        _telemetry.value = _telemetry.value.copy(status = StreamingState.LOADING)

        val cached = synchronized(cache) { cache[resolvedId] }
        if (cached != null) {
            synchronized(cache) {
                evictToBudgetLocked(preserveDistrictId = resolvedId)
                publishTelemetryLocked(resolvedId, StreamingState.LOADED, loadMs = 1L)
            }
            return cached
        }

        val built = withContext(ioDispatcher) {
            DistrictWorldBuilder.buildForDistrict(resolvedId, budget.baseLod)
        }
        val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)

        synchronized(cache) {
            cache[resolvedId] = built
            evictToBudgetLocked(preserveDistrictId = resolvedId)
            publishTelemetryLocked(resolvedId, StreamingState.LOADED, loadMs = elapsedMs)
        }
        return built
    }

    suspend fun unloadDistrict(districtId: String): Boolean = mutex.withLock {
        val removed = synchronized(cache) { cache.remove(districtId) != null }
        if (removed) {
            synchronized(cache) {
                publishTelemetryLocked(
                    _telemetry.value.activeDistrictId,
                    StreamingState.UNLOADING,
                    extraUnloads = 1
                )
            }
        }
        removed
    }

    suspend fun clearAllExcept(activeDistrictId: String) = mutex.withLock {
        synchronized(cache) {
            val toRemove = cache.keys.filter { it != activeDistrictId }
            toRemove.forEach { cache.remove(it) }
            publishTelemetryLocked(activeDistrictId, StreamingState.LOADED, extraUnloads = toRemove.size)
        }
    }

    fun evaluateChunksVisibility(
        world: DhakaWorldGeometry,
        cameraPos: Vec3,
        cameraForward: Vec3
    ): List<Boolean> {
        val maxDist = budget.baseLod.cullDistanceMeters
        val chunks = world.spatialChunks
        if (chunks.isEmpty()) return emptyList()

        val result = ArrayList<Boolean>(chunks.size)
        var visibleCount = 0
        for (chunk in chunks) {
            if (chunk.isDetailChunk && budget.baseLod == LodLevel.LOD_LOW) {
                result.add(false)
                continue
            }
            val cx = (chunk.bounds.minX + chunk.bounds.maxX) * 0.5f
            val cz = (chunk.bounds.minZ + chunk.bounds.maxZ) * 0.5f
            val dx = cx - cameraPos.x
            val dz = cz - cameraPos.z
            val halfSpan = (chunk.bounds.maxX - chunk.bounds.minX) * 0.5f
            val dist = (sqrt(dx * dx + dz * dz) - halfSpan).coerceAtLeast(0f)
            val inDistance = dist <= maxDist
            val inFrontOrInside = if (!budget.enableFrustumCulling || dist < 12f) {
                true
            } else {
                val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
                val dot = (dx / len) * cameraForward.x + (dz / len) * cameraForward.z
                dot >= -0.35f
            }
            val visible = inDistance && inFrontOrInside
            if (visible) visibleCount++
            result.add(visible)
        }
        val cur = _telemetry.value
        if (cur.visibleChunkCount != visibleCount || cur.totalChunkCount != chunks.size) {
            _telemetry.value = cur.copy(
                visibleChunkCount = visibleCount,
                totalChunkCount = chunks.size
            )
        }
        return result
    }

    private fun evictToBudgetLocked(preserveDistrictId: String): Int {
        var unloaded = 0
        while (cache.size > budget.maxCachedDistricts) {
            val evictKey = cache.keys.firstOrNull { it != preserveDistrictId } ?: break
            cache.remove(evictKey)
            unloaded++
        }
        while (totalMemoryLocked() > budget.memoryBudgetMb && cache.size > 1) {
            val evictKey = cache.keys.firstOrNull { it != preserveDistrictId } ?: break
            cache.remove(evictKey)
            unloaded++
        }
        if (unloaded > 0) {
            _telemetry.value = _telemetry.value.copy(
                totalUnloads = _telemetry.value.totalUnloads + unloaded
            )
        }
        return unloaded
    }

    private fun totalMemoryLocked(): Float =
        cache.values.sumOf { it.estimatedMemoryMb.toDouble() }.toFloat()

    private fun publishTelemetryLocked(
        activeId: String,
        status: StreamingState,
        extraUnloads: Int = 0,
        loadMs: Long = _telemetry.value.lastLoadTimeMs
    ) {
        val activeWorld = cache[activeId]
        _telemetry.value = _telemetry.value.copy(
            activeDistrictId = activeId,
            loadedDistrictIds = cache.keys.toList(),
            cachedCount = cache.size,
            status = status,
            estimatedMemoryMb = totalMemoryLocked(),
            budgetMb = budget.memoryBudgetMb,
            activeLod = budget.baseLod,
            totalChunkCount = activeWorld?.spatialChunks?.size ?: 1,
            visibleChunkCount = activeWorld?.spatialChunks?.size ?: 1,
            totalUnloads = _telemetry.value.totalUnloads + extraUnloads,
            lastLoadTimeMs = loadMs
        )
    }
}
