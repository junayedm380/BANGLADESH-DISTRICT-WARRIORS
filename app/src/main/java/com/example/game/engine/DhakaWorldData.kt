package com.example.game.engine

import com.example.game.districts.DistrictBiomeType
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.GroundLootItem
import com.example.game.inventory.GroundLootKind
import com.example.game.vehicles.VehicleEntity
import com.example.game.vehicles.VehicleType
import java.nio.FloatBuffer

data class RangeTarget(
    val id: String,
    val basePos: Vec3,
    val isMoving: Boolean,
    var currentPos: Vec3 = basePos,
    var phase: Float = 0f,
    var hitCooldownSec: Float = 0f
)

data class WorldRenderChunk(
    val id: String,
    val bounds: Aabb,
    val buffer: FloatBuffer,
    val vertexCount: Int,
    val isDetailChunk: Boolean = false
)

data class DhakaWorldGeometry(
    val staticBuffer: FloatBuffer,
    val staticVertexCount: Int,
    val colliders: List<Aabb>,
    val vaultables: List<Aabb>,
    val waterZone: Aabb,
    val initialRangeTargets: List<RangeTarget>,
    val initialVehicles: List<VehicleEntity>,
    val initialLoot: List<GroundLootItem>,
    val npcCommanderPos: Vec3,
    val weaponBenchPos: Vec3,
    val ammoStationPos: Vec3,
    val aiPracticePos: Vec3,
    val missionBeaconPos: Vec3,
    val streetLightPositions: List<Vec3>,
    val districtId: String = "dhaka",
    val biome: DistrictBiomeType = DistrictBiomeType.DHAKA_METROPOLIS,
    val skyRgb: Triple<Float, Float, Float> = Triple(0.11f, 0.16f, 0.24f),
    val missionObjectivePos: Vec3 = Vec3(-12.5f, 0f, 15.0f),
    val travelPortalPos: Vec3 = Vec3(0f, 0f, 27.5f),
    val spatialChunks: List<WorldRenderChunk> = emptyList(),
    val estimatedMemoryMb: Float = 16.0f
)

object DhakaWorldData {
    const val ARENA_HALF_SIZE = 32f

    fun build(includeDetails: Boolean = true): DhakaWorldGeometry {
        val baseMesh = MeshBuilder()
        val detailMesh = MeshBuilder()
        val colliders = ArrayList<Aabb>()
        val vaultables = ArrayList<Aabb>()
        val lightPositions = ArrayList<Vec3>()

        fun addSolidBox(
            cx: Float, cy: Float, cz: Float,
            sx: Float, sy: Float, sz: Float,
            r: Float, g: Float, b: Float,
            isVaultable: Boolean = false,
            targetMesh: MeshBuilder = baseMesh
        ) {
            targetMesh.addBox(cx, cy, cz, sx, sy, sz, r, g, b)
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

        // 1. Base Urban Ground & Plaza
        baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.16f, 0.21f, 0.19f)
        baseMesh.addGroundRect(0f, 0.01f, 0f, 22f, 22f, 0.27f, 0.29f, 0.32f)

        // 2. Buriganga Canal / Dhaka Lake Water Zone (West Sidewalk Edge)
        val waterZone = Aabb(-31.2f, -0.5f, -6.5f, -19.2f, 0.45f, 12.5f)
        baseMesh.addGroundRect(-25.2f, 0.04f, 3.0f, 12.0f, 19.0f, 0.06f, 0.38f, 0.56f)
        baseMesh.addBox(-19.0f, 0.12f, 3.0f, 0.5f, 0.24f, 19.2f, 0.42f, 0.45f, 0.48f)

        // 3. Intersecting Asphalt Roads & Lane Markings
        baseMesh.addGroundRect(0f, 0.02f, 0f, 8.5f, 64f, 0.12f, 0.13f, 0.15f)
        baseMesh.addGroundRect(0f, 0.02f, 0f, 64f, 8.5f, 0.12f, 0.13f, 0.15f)
        if (includeDetails) {
            for (i in -26..26 step 4) {
                if (kotlin.math.abs(i) > 5) {
                    detailMesh.addGroundRect(0f, 0.03f, i.toFloat(), 0.28f, 1.8f, 0.92f, 0.86f, 0.45f)
                    detailMesh.addGroundRect(i.toFloat(), 0.03f, 0f, 1.8f, 0.28f, 0.92f, 0.86f, 0.45f)
                }
            }
            for (offset in -3..3 step 1) {
                detailMesh.addGroundRect(offset * 1.1f, 0.035f, -5.2f, 0.55f, 2.0f, 0.90f, 0.92f, 0.94f)
                detailMesh.addGroundRect(offset * 1.1f, 0.035f, 5.2f, 0.55f, 2.0f, 0.90f, 0.92f, 0.94f)
            }
        }

        // 4. Perimeter Boundary Curb Walls
        addSolidBox(0f, 1.0f, -32.5f, 65f, 2.0f, 1.2f, 0.22f, 0.25f, 0.29f)
        addSolidBox(0f, 1.0f, 32.5f, 65f, 2.0f, 1.2f, 0.22f, 0.25f, 0.29f)
        addSolidBox(-32.5f, 1.0f, 0f, 1.2f, 2.0f, 65f, 0.22f, 0.25f, 0.29f)
        addSolidBox(32.5f, 1.0f, 0f, 1.2f, 2.0f, 65f, 0.22f, 0.25f, 0.29f)

        // 5. Dhaka Residential & Commercial Buildings
        fun addDhakaBuilding(cx: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
            addSolidBox(cx, sy * 0.5f, cz, sx, sy, sz, r, g, b)
            if (includeDetails) {
                detailMesh.addBox(cx, sy + 0.25f, cz, sx + 0.25f, 0.5f, sz + 0.25f, r * 0.78f, g * 0.78f, b * 0.78f)
                detailMesh.addBox(cx + sx * 0.22f, sy + 1.1f, cz - sz * 0.2f, 1.3f, 1.4f, 1.3f, 0.12f, 0.45f, 0.68f)
                val floors = (sy / 3f).toInt().coerceAtLeast(2)
                for (f in 1 until floors) {
                    val wy = f * 2.8f
                    detailMesh.addBox(cx, wy, cz + sz * 0.5f + 0.04f, sx * 0.65f, 0.65f, 0.08f, 0.98f, 0.84f, 0.48f)
                    detailMesh.addBox(cx, wy, cz - sz * 0.5f - 0.04f, sx * 0.65f, 0.65f, 0.08f, 0.98f, 0.84f, 0.48f)
                }
            }
        }

        addDhakaBuilding(-18f, -18f, 8f, 12f, 7f, 0.72f, 0.38f, 0.28f)
        addDhakaBuilding(-26f, -14f, 6f, 9f, 6f, 0.82f, 0.68f, 0.42f)
        addDhakaBuilding(23f, -21f, 8f, 14f, 7f, 0.42f, 0.64f, 0.56f)
        addDhakaBuilding(24f, 22f, 8f, 15f, 8f, 0.48f, 0.53f, 0.60f)
        addDhakaBuilding(-24f, 22f, 7f, 11f, 7f, 0.75f, 0.46f, 0.36f)

        // 6. Small Dhaka Marketplace ("Chawk Bazar Block")
        baseMesh.addGroundRect(-13.5f, 0.025f, 15.5f, 12f, 10f, 0.34f, 0.29f, 0.25f)
        fun addMarketStall(cx: Float, cz: Float, canopyR: Float, canopyG: Float, canopyB: Float) {
            addSolidBox(cx, 0.55f, cz, 2.8f, 1.1f, 1.6f, 0.48f, 0.33f, 0.20f, isVaultable = true)
            if (includeDetails) {
                detailMesh.addBox(cx - 1.2f, 1.4f, cz - 0.6f, 0.12f, 2.6f, 0.12f, 0.3f, 0.3f, 0.32f)
                detailMesh.addBox(cx + 1.2f, 1.4f, cz - 0.6f, 0.12f, 2.6f, 0.12f, 0.3f, 0.3f, 0.32f)
                detailMesh.addBox(cx - 1.2f, 1.4f, cz + 0.6f, 0.12f, 2.6f, 0.12f, 0.3f, 0.3f, 0.32f)
                detailMesh.addBox(cx + 1.2f, 1.4f, cz + 0.6f, 0.12f, 2.6f, 0.12f, 0.3f, 0.3f, 0.32f)
            }
            baseMesh.addBox(cx, 2.7f, cz, 3.2f, 0.22f, 2.1f, canopyR, canopyG, canopyB)
        }
        addMarketStall(-11f, 12f, 0.08f, 0.68f, 0.42f)
        addMarketStall(-16f, 12f, 0.85f, 0.20f, 0.22f)
        addMarketStall(-11f, 18f, 0.92f, 0.62f, 0.12f)
        addMarketStall(-16f, 18f, 0.15f, 0.56f, 0.78f)
        addSolidBox(-13.5f, 0.45f, 15f, 1.2f, 0.9f, 1.2f, 0.62f, 0.44f, 0.24f, isVaultable = true)

        // 7. Vaultable Tactical Cover & Parkour Barriers in Open Combat Plaza
        addSolidBox(-6.5f, 0.60f, -6.5f, 2.4f, 1.2f, 1.0f, 0.36f, 0.41f, 0.46f, isVaultable = true)
        addSolidBox(6.5f, 0.60f, 6.5f, 2.4f, 1.2f, 1.0f, 0.36f, 0.41f, 0.46f, isVaultable = true)
        addSolidBox(-7.5f, 0.60f, 5.5f, 1.2f, 1.2f, 2.6f, 0.42f, 0.40f, 0.35f, isVaultable = true)
        addSolidBox(6.0f, 0.50f, -6.0f, 2.2f, 1.0f, 2.2f, 0.32f, 0.44f, 0.38f, isVaultable = true)

        // 8. Training Ground Zone — North-East Quadrant
        baseMesh.addGroundRect(14f, 0.025f, -12f, 15f, 13f, 0.24f, 0.28f, 0.25f)
        addSolidBox(11f, 0.55f, -10.5f, 5.5f, 1.1f, 0.9f, 0.64f, 0.55f, 0.38f, isVaultable = true)
        addSolidBox(8.5f, 0.48f, -7.8f, 2.0f, 0.96f, 1.0f, 0.14f, 0.52f, 0.42f, isVaultable = true)
        addSolidBox(14.5f, 0.48f, -7.8f, 1.6f, 0.96f, 1.0f, 0.82f, 0.56f, 0.14f, isVaultable = true)
        addSolidBox(15f, 1.8f, -18.5f, 12f, 3.6f, 1.0f, 0.35f, 0.35f, 0.37f)

        // 9. Vehicle Testing Area — South-East Quadrant (Track Pad)
        baseMesh.addGroundRect(16f, 0.025f, 14f, 16f, 14f, 0.22f, 0.25f, 0.29f)
        baseMesh.addGroundRect(16f, 0.032f, 14f, 13f, 0.28f, 0.95f, 0.65f, 0.12f)

        // 10. Street Lights
        val lampCoords = listOf(
            Vec3(-5.0f, 0f, -15f),
            Vec3(5.0f, 0f, -15f),
            Vec3(-5.0f, 0f, 15f),
            Vec3(5.0f, 0f, 15f),
            Vec3(-15f, 0f, -5.0f),
            Vec3(15f, 0f, 5.0f)
        )
        for (lp in lampCoords) {
            addSolidBox(lp.x, 2.3f, lp.z, 0.24f, 4.6f, 0.24f, 0.25f, 0.28f, 0.33f)
            baseMesh.addBox(lp.x, 4.7f, lp.z, 0.6f, 0.22f, 0.6f, 1.0f, 0.85f, 0.42f)
            lightPositions.add(Vec3(lp.x, 4.7f, lp.z))
        }

        // 11. Mission Starting Pad, Objective & Highway Travel Portal Pad
        baseMesh.addBox(0f, 0.08f, -11.5f, 3.0f, 0.16f, 3.0f, 0.05f, 0.72f, 0.45f)
        baseMesh.addBox(-12.5f, 0.09f, 15.0f, 2.4f, 0.18f, 2.4f, 0.10f, 0.82f, 0.92f)
        baseMesh.addBox(0f, 0.09f, 27.5f, 5.6f, 0.18f, 3.2f, 0.94f, 0.66f, 0.12f)

        val chunks = buildList {
            add(
                WorldRenderChunk(
                    id = "dhaka_base",
                    bounds = Aabb(-34f, 0f, -34f, 34f, 18f, 34f),
                    buffer = baseMesh.toFloatBuffer(),
                    vertexCount = baseMesh.vertexCount,
                    isDetailChunk = false
                )
            )
            if (detailMesh.vertexCount > 0) {
                add(
                    WorldRenderChunk(
                        id = "dhaka_detail",
                        bounds = Aabb(-30f, 0f, -26f, 30f, 16f, 26f),
                        buffer = detailMesh.toFloatBuffer(),
                        vertexCount = detailMesh.vertexCount,
                        isDetailChunk = true
                    )
                )
            }
        }

        val targets = listOf(
            RangeTarget("target_1", Vec3(11.0f, 1.3f, -16.8f), isMoving = false),
            RangeTarget("target_2", Vec3(13.5f, 1.3f, -16.8f), isMoving = false),
            RangeTarget("target_3", Vec3(16.0f, 1.3f, -16.8f), isMoving = false),
            RangeTarget("target_moving_1", Vec3(14.0f, 1.7f, -14.0f), isMoving = true),
            RangeTarget("target_moving_2", Vec3(14.0f, 2.3f, -15.4f), isMoving = true)
        )

        val vehicles = listOf(
            VehicleEntity("veh_car_1", VehicleType.CAR, Vec3(11.5f, 0f, 11.5f), yawRad = 0.0f),
            VehicleEntity("veh_moto_1", VehicleType.MOTORCYCLE, Vec3(15.5f, 0f, 10.5f), yawRad = -0.35f),
            VehicleEntity("veh_bus_1", VehicleType.BUS, Vec3(19.5f, 0f, 15.0f), yawRad = 1.57f),
            VehicleEntity("veh_truck_1", VehicleType.TRUCK, Vec3(12.0f, 0f, 18.0f), yawRad = 0.2f),
            VehicleEntity("veh_boat_1", VehicleType.BOAT, Vec3(-24.5f, 0.05f, 3.5f), yawRad = 0.0f)
        )

        val loot = listOf(
            GroundLootItem("loot_akm", GroundLootKind.WEAPON_CRATE, 7.2f, 0.35f, -7.8f, weaponId = "ar_bengal_akm"),
            GroundLootItem("loot_awm", GroundLootKind.WEAPON_CRATE, 9.8f, 0.35f, -7.8f, weaponId = "sr_awm"),
            GroundLootItem("loot_pump", GroundLootKind.WEAPON_CRATE, -5.5f, 0.35f, 5.2f, weaponId = "sg_pump"),
            GroundLootItem("loot_vector", GroundLootKind.WEAPON_CRATE, -12.5f, 0.35f, 14.5f, weaponId = "smg_vector"),
            GroundLootItem("loot_m249", GroundLootKind.WEAPON_CRATE, 5.5f, 0.35f, 5.0f, weaponId = "lmg_m249"),
            GroundLootItem("loot_pan", GroundLootKind.WEAPON_CRATE, -14.8f, 0.35f, 14.5f, weaponId = "melee_pan"),
            GroundLootItem("loot_ammo_1", GroundLootKind.AMMO_BOX, 14.5f, 0.35f, -6.5f, ammoAmount = 60),
            GroundLootItem("loot_ammo_2", GroundLootKind.AMMO_BOX, 2.5f, 0.35f, -4.5f, ammoAmount = 60),
            GroundLootItem("loot_med_1", GroundLootKind.MEDKIT_DROP, -6.0f, 0.35f, -5.0f, consumable = ConsumableType.FIRST_AID_KIT),
            GroundLootItem("loot_med_2", GroundLootKind.MEDKIT_DROP, 13.5f, 0.35f, -9.2f, consumable = ConsumableType.BANDAGE),
            GroundLootItem("loot_armor_1", GroundLootKind.ARMOR_DROP, -9.5f, 0.35f, 11.0f)
        )

        val estMem = ((baseMesh.vertexCount + detailMesh.vertexCount) * 36f) / (1024f * 1024f) + 14.2f
        return DhakaWorldGeometry(
            staticBuffer = baseMesh.toFloatBuffer(),
            staticVertexCount = baseMesh.vertexCount,
            colliders = colliders,
            vaultables = vaultables,
            waterZone = waterZone,
            initialRangeTargets = targets,
            initialVehicles = vehicles,
            initialLoot = loot,
            npcCommanderPos = Vec3(7.0f, 0f, -10.5f),
            weaponBenchPos = Vec3(8.5f, 0f, -7.8f),
            ammoStationPos = Vec3(14.5f, 0f, -7.8f),
            aiPracticePos = Vec3(17.5f, 0f, -9.5f),
            missionBeaconPos = Vec3(0f, 0f, -11.5f),
            streetLightPositions = lightPositions,
            districtId = "dhaka",
            biome = DistrictBiomeType.DHAKA_METROPOLIS,
            skyRgb = Triple(0.11f, 0.16f, 0.24f),
            missionObjectivePos = Vec3(-12.5f, 0f, 15.0f),
            travelPortalPos = Vec3(0f, 0f, 27.5f),
            spatialChunks = chunks,
            estimatedMemoryMb = estMem
        )
    }
}
