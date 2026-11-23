package com.example.game.world

import com.example.game.engine.Aabb
import com.example.game.engine.MeshBuilder

internal class BiomeMeshCollector(private val includeDetails: Boolean) {
    val baseMesh = MeshBuilder()
    val detailMesh = MeshBuilder()
    val colliders = ArrayList<Aabb>()
    val vaultables = ArrayList<Aabb>()

    fun addSolidBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        isVaultable: Boolean = false
    ) {
        baseMesh.addBox(cx, cy, cz, sx, sy, sz, r, g, b)
        val box = Aabb(
            minX = cx - sx * 0.5f,
            minY = cy - sy * 0.5f,
            minZ = cz - sz * 0.5f,
            maxX = cx + sx * 0.5f,
            maxY = cy + sy * 0.5f,
            maxZ = cz + sz * 0.5f
        )
        colliders.add(box)
        if (isVaultable) vaultables.add(box)
    }

    fun addPalmOrTree(x: Float, z: Float, height: Float = 4.6f, canopyColor: Triple<Float, Float, Float>) {
        addSolidBox(x, height * 0.35f, z, 0.55f, height * 0.7f, 0.55f, 0.36f, 0.24f, 0.14f)
        val target = if (includeDetails) detailMesh else baseMesh
        target.addBox(x, height * 0.85f, z, 2.8f, 1.5f, 2.8f, canopyColor.first, canopyColor.second, canopyColor.third)
        if (includeDetails) {
            detailMesh.addBox(x, height * 1.05f, z, 1.8f, 0.9f, 1.8f, canopyColor.first * 1.08f, canopyColor.second * 1.08f, canopyColor.third)
        }
    }

    fun addSteppedHill(cx: Float, cz: Float, baseW: Float, baseD: Float, height: Float, r: Float, g: Float, b: Float) {
        addSolidBox(cx, height * 0.25f, cz, baseW, height * 0.5f, baseD, r, g, b)
        if (includeDetails) {
            detailMesh.addBox(cx, height * 0.68f, cz, baseW * 0.72f, height * 0.38f, baseD * 0.72f, r * 1.08f, g * 1.08f, b * 0.96f)
            detailMesh.addBox(cx, height * 0.98f, cz, baseW * 0.42f, height * 0.24f, baseD * 0.42f, r * 1.12f, g * 1.14f, b * 0.94f)
        }
    }

    fun addShippingContainer(cx: Float, cz: Float, rotated: Boolean, r: Float, g: Float, b: Float) {
        val sx = if (rotated) 2.5f else 6.2f
        val sz = if (rotated) 6.2f else 2.5f
        addSolidBox(cx, 1.35f, cz, sx, 2.7f, sz, r, g, b)
        if (includeDetails) {
            detailMesh.addBox(cx, 2.75f, cz, sx * 0.95f, 0.12f, sz * 0.95f, r * 0.85f, g * 0.85f, b * 0.85f)
        }
    }

    fun addWoodenBoatProp(cx: Float, cz: Float, hullColor: Triple<Float, Float, Float>) {
        val target = if (includeDetails) detailMesh else baseMesh
        target.addBox(cx, 0.22f, cz, 1.8f, 0.4f, 4.6f, hullColor.first, hullColor.second, hullColor.third)
        target.addBox(cx, 0.65f, cz, 1.3f, 0.5f, 2.2f, 0.82f, 0.68f, 0.38f)
    }

    fun addCottage(cx: Float, cz: Float, w: Float, h: Float, d: Float, wallRgb: Triple<Float, Float, Float>, roofRgb: Triple<Float, Float, Float>) {
        addSolidBox(cx, h * 0.5f, cz, w, h, d, wallRgb.first, wallRgb.second, wallRgb.third)
        baseMesh.addBox(cx, h + 0.35f, cz, w + 0.6f, 0.65f, d + 0.6f, roofRgb.first, roofRgb.second, roofRgb.third)
    }

    fun addBridgeSpan(cx: Float, cz: Float, width: Float, length: Float, deckRgb: Triple<Float, Float, Float>, archRgb: Triple<Float, Float, Float>) {
        baseMesh.addGroundRect(cx, 0.15f, cz, width, length, deckRgb.first, deckRgb.second, deckRgb.third)
        addSolidBox(cx - width * 0.48f, 0.65f, cz, 0.45f, 1.1f, length, archRgb.first, archRgb.second, archRgb.third, isVaultable = true)
        addSolidBox(cx + width * 0.48f, 0.65f, cz, 0.45f, 1.1f, length, archRgb.first, archRgb.second, archRgb.third, isVaultable = true)
    }
}
