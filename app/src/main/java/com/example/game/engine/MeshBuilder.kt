package com.example.game.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Procedural mesh builder producing interleaved vertices:
 * [x, y, z, nx, ny, nz, r, g, b] (9 floats per vertex)
 */
class MeshBuilder {
    private val data = ArrayList<Float>(16384)

    val vertexCount: Int
        get() = data.size / 9

    fun addBox(
        cx: Float,
        cy: Float,
        cz: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // Front (+Z)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r, g, b)
        // Back (-Z)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r * 0.94f, g * 0.94f, b * 0.94f)
        // Left (-X)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
        // Right (+X)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.97f, g * 0.97f, b * 0.97f)
        // Top (+Y)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, (r * 1.08f).coerceAtMost(1f), (g * 1.08f).coerceAtMost(1f), (b * 1.08f).coerceAtMost(1f))
        // Bottom (-Y)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.7f, g * 0.7f, b * 0.7f)
    }

    fun addGroundRect(
        cx: Float,
        y: Float,
        cz: Float,
        sx: Float,
        sz: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        val hx = sx * 0.5f
        val hz = sz * 0.5f
        addQuad(
            cx - hx, y, cz + hz,
            cx + hx, y, cz + hz,
            cx + hx, y, cz - hz,
            cx - hx, y, cz - hz,
            0f, 1f, 0f,
            r, g, b
        )
    }

    private fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        pushVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        pushVertex(x1, y1, z1, nx, ny, nz, r, g, b)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b)

        pushVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b)
        pushVertex(x3, y3, z3, nx, ny, nz, r, g, b)
    }

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
    }

    fun toFloatBuffer(): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        for (f in data) fb.put(f)
        fb.position(0)
        return fb
    }

    companion object {
        fun createUnitCube(): Pair<FloatBuffer, Int> {
            val builder = MeshBuilder()
            builder.addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f)
            return Pair(builder.toFloatBuffer(), builder.vertexCount)
        }
    }
}
