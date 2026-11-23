package com.example.game.engine

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-5f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, -1f)
    }

    fun distanceTo(o: Vec3): Float = (this - o).length()
}

data class Aabb(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    /**
     * Ray-AABB intersection returning normalized distance t in [0, 1] along ray (origin + dir * maxDist * t), or null if no hit.
     */
    fun intersectRay(origin: Vec3, dirNormalized: Vec3, maxDist: Float): Float? {
        var tMin = 0f
        var tMax = maxDist

        // X slab
        if (kotlin.math.abs(dirNormalized.x) < 1e-6f) {
            if (origin.x < minX || origin.x > maxX) return null
        } else {
            val invD = 1f / dirNormalized.x
            var t1 = (minX - origin.x) * invD
            var t2 = (maxX - origin.x) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        // Y slab
        if (kotlin.math.abs(dirNormalized.y) < 1e-6f) {
            if (origin.y < minY || origin.y > maxY) return null
        } else {
            val invD = 1f / dirNormalized.y
            var t1 = (minY - origin.y) * invD
            var t2 = (maxY - origin.y) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        // Z slab
        if (kotlin.math.abs(dirNormalized.z) < 1e-6f) {
            if (origin.z < minZ || origin.z > maxZ) return null
        } else {
            val invD = 1f / dirNormalized.z
            var t1 = (minZ - origin.z) * invD
            var t2 = (maxZ - origin.z) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        return tMin
    }

    fun resolveCircleXz(x: Float, y: Float, z: Float, radius: Float, height: Float): Pair<Float, Float>? {
        if (y + height <= minY || y >= maxY) return null
        val closestX = x.coerceIn(minX, maxX)
        val closestZ = z.coerceIn(minZ, maxZ)
        val dx = x - closestX
        val dz = z - closestZ
        val distSq = dx * dx + dz * dz
        if (distSq >= radius * radius) return null

        val dist = sqrt(distSq)
        if (dist > 1e-5f) {
            val push = radius - dist
            return Pair(x + (dx / dist) * push, z + (dz / dist) * push)
        }
        // Center is inside box: push out along shallowest axis
        val left = kotlin.math.abs(x - minX)
        val right = kotlin.math.abs(maxX - x)
        val back = kotlin.math.abs(z - minZ)
        val front = kotlin.math.abs(maxZ - z)
        return when (minOf(left, right, back, front)) {
            left -> Pair(minX - radius, z)
            right -> Pair(maxX + radius, z)
            back -> Pair(x, minZ - radius)
            else -> Pair(x, maxZ + radius)
        }
    }
}

object GlTrig {
    fun forwardFromYawPitch(yawRad: Float, pitchRad: Float): Vec3 {
        val cp = cos(pitchRad)
        return Vec3(
            x = sin(yawRad) * cp,
            y = sin(pitchRad),
            z = -cos(yawRad) * cp
        ).normalized()
    }

    fun rightFromYaw(yawRad: Float): Vec3 =
        Vec3(cos(yawRad), 0f, sin(yawRad)).normalized()
}
