package com.example.game.world

import com.example.game.districts.DistrictBiomeType
import com.example.game.districts.DistrictCatalog
import com.example.game.engine.Aabb
import com.example.game.engine.DhakaWorldData
import com.example.game.engine.DhakaWorldGeometry
import com.example.game.engine.RangeTarget
import com.example.game.engine.Vec3
import com.example.game.engine.WorldRenderChunk
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.GroundLootItem
import com.example.game.inventory.GroundLootKind
import com.example.game.vehicles.VehicleEntity
import com.example.game.vehicles.VehicleType

object DistrictWorldBuilder {

    fun buildForDistrict(
        districtId: String,
        lodLevel: LodLevel = LodLevel.LOD_HIGH
    ): DhakaWorldGeometry {
        val entry = DistrictCatalog.getById(districtId)
        val includeDetails = lodLevel != LodLevel.LOD_LOW

        if (entry.id == "dhaka") {
            return DhakaWorldData.build(includeDetails = includeDetails)
        }

        val c = BiomeMeshCollector(includeDetails = includeDetails)
        val lightPositions = ArrayList<Vec3>()

        // Perimeter curbs common to every bounded arena
        c.addSolidBox(0f, 1.0f, -32.5f, 65f, 2.0f, 1.2f, 0.24f, 0.27f, 0.30f)
        c.addSolidBox(0f, 1.0f, 32.5f, 65f, 2.0f, 1.2f, 0.24f, 0.27f, 0.30f)
        c.addSolidBox(-32.5f, 1.0f, 0f, 1.2f, 2.0f, 65f, 0.24f, 0.27f, 0.30f)
        c.addSolidBox(32.5f, 1.0f, 0f, 1.2f, 2.0f, 65f, 0.24f, 0.27f, 0.30f)

        var skyRgb = Triple(0.11f, 0.16f, 0.24f)
        var waterZone = Aabb(-31f, -0.5f, -8f, -20f, 0.45f, 10f)
        var boatSpawn = Vec3(-24.5f, 0.05f, 2.0f)

        when (entry.biome) {
            DistrictBiomeType.CHATTOGRAM_PORT_HILLS -> {
                skyRgb = Triple(0.14f, 0.22f, 0.30f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.24f, 0.27f, 0.28f)
                // Coastal asphalt road & Karnaphuli harbor channel
                c.baseMesh.addGroundRect(4f, 0.02f, 0f, 9f, 64f, 0.14f, 0.15f, 0.18f)
                waterZone = Aabb(-31f, -0.5f, -18f, -17f, 0.45f, 22f)
                c.baseMesh.addGroundRect(-24f, 0.04f, 2f, 14f, 40f, 0.06f, 0.36f, 0.56f)
                // Container port stacks & crane pillars
                c.addShippingContainer(-11f, -8f, false, 0.85f, 0.22f, 0.18f)
                c.addShippingContainer(-11f, -12f, false, 0.12f, 0.55f, 0.78f)
                c.addShippingContainer(-10f, 10f, true, 0.92f, 0.64f, 0.14f)
                c.addShippingContainer(16f, 8f, false, 0.14f, 0.65f, 0.44f)
                // Port gantry crane
                c.addSolidBox(-15f, 5.5f, 0f, 1.4f, 11f, 1.4f, 0.92f, 0.62f, 0.12f)
                if (includeDetails) {
                    c.detailMesh.addBox(-11.5f, 10.5f, 0f, 9f, 0.9f, 1.2f, 0.92f, 0.62f, 0.12f)
                }
                // Green Batali hills on eastern flank
                c.addSteppedHill(22f, -18f, 14f, 14f, 9.5f, 0.16f, 0.48f, 0.24f)
                c.addSteppedHill(23f, 20f, 13f, 12f, 8.0f, 0.18f, 0.45f, 0.22f)
                boatSpawn = Vec3(-23.5f, 0.05f, 2.0f)
            }

            DistrictBiomeType.COXS_BAZAR_COAST -> {
                skyRgb = Triple(0.18f, 0.32f, 0.48f)
                // Golden sandy beach + inland green
                c.baseMesh.addGroundRect(10f, 0f, 0f, 48f, 68f, 0.78f, 0.70f, 0.46f)
                c.baseMesh.addGroundRect(25f, 0.01f, 0f, 18f, 68f, 0.22f, 0.48f, 0.24f)
                // Marine Drive coastal highway
                c.baseMesh.addGroundRect(12f, 0.03f, 0f, 7.5f, 64f, 0.14f, 0.16f, 0.19f)
                // Bay of Bengal long western surf zone
                waterZone = Aabb(-31f, -0.5f, -30f, -10f, 0.45f, 30f)
                c.baseMesh.addGroundRect(-20.5f, 0.04f, 0f, 21f, 60f, 0.07f, 0.50f, 0.72f)
                // White surf foam line
                c.baseMesh.addGroundRect(-9.8f, 0.045f, 0f, 1.4f, 60f, 0.85f, 0.94f, 0.98f)
                // Coastal palms & beach settlements
                c.addCottage(22f, -14f, 5.5f, 3.4f, 5f, Triple(0.86f, 0.82f, 0.74f), Triple(0.75f, 0.32f, 0.20f))
                c.addCottage(22f, 14f, 6f, 3.6f, 5f, Triple(0.82f, 0.78f, 0.68f), Triple(0.12f, 0.62f, 0.48f))
                listOf(-18f to 20f, -6f to 21f, 6f to 20f, 18f to 21f).forEach { (z, x) ->
                    c.addPalmOrTree(x, z, 5.2f, Triple(0.12f, 0.58f, 0.26f))
                }
                boatSpawn = Vec3(-17.5f, 0.05f, 4.0f)
            }

            DistrictBiomeType.SYLHET_TEA_HIGHLANDS -> {
                skyRgb = Triple(0.12f, 0.24f, 0.26f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.14f, 0.42f, 0.18f)
                // Surma river & Keane Bridge
                waterZone = Aabb(-31f, -0.5f, 13f, 31f, 0.45f, 26f)
                c.baseMesh.addGroundRect(0f, 0.04f, 19.5f, 62f, 13f, 0.08f, 0.44f, 0.58f)
                c.addBridgeSpan(0f, 19.5f, 7.2f, 14.5f, Triple(0.32f, 0.34f, 0.36f), Triple(0.82f, 0.26f, 0.24f))
                // Stepped emerald tea garden terraces
                c.addSteppedHill(-19f, -16f, 16f, 13f, 7.2f, 0.10f, 0.58f, 0.22f)
                c.addSteppedHill(19f, -16f, 16f, 13f, 7.5f, 0.12f, 0.62f, 0.24f)
                c.addSteppedHill(-20f, 4f, 11f, 9f, 4.8f, 0.12f, 0.56f, 0.22f)
                // Tea rows (vaultable hedgerows)
                for (z in listOf(-12f, -8f, -4f)) {
                    c.addSolidBox(-7f, 0.5f, z, 6f, 0.9f, 1.1f, 0.08f, 0.64f, 0.24f, isVaultable = true)
                    c.addSolidBox(7f, 0.5f, z, 6f, 0.9f, 1.1f, 0.08f, 0.64f, 0.24f, isVaultable = true)
                }
                boatSpawn = Vec3(-12f, 0.05f, 19.5f)
            }

            DistrictBiomeType.KHULNA_SUNDARBANS -> {
                skyRgb = Triple(0.10f, 0.20f, 0.20f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.22f, 0.28f, 0.16f)
                // Tidal river channel
                waterZone = Aabb(-31f, -0.5f, -12f, -14f, 0.45f, 18f)
                c.baseMesh.addGroundRect(-22.5f, 0.04f, 3f, 17f, 30f, 0.09f, 0.36f, 0.38f)
                c.addWoodenBoatProp(-20f, -6f, Triple(0.45f, 0.28f, 0.14f))
                c.addWoodenBoatProp(-24f, 11f, Triple(0.50f, 0.32f, 0.16f))
                // Sundari mangrove forest clusters
                listOf(-10f to -14f, -8f to -4f, 18f to -14f, 20f to -4f, 21f to 16f, -9f to 20f).forEach { (x, z) ->
                    c.addPalmOrTree(x, z, 5.4f, Triple(0.08f, 0.46f, 0.18f))
                }
                // Forest ranger timber outpost
                c.addCottage(16f, 12f, 6f, 3.6f, 5.5f, Triple(0.48f, 0.34f, 0.20f), Triple(0.14f, 0.48f, 0.24f))
                boatSpawn = Vec3(-21f, 0.05f, 3f)
            }

            DistrictBiomeType.RAJSHAHI_PADMA_PLAINS -> {
                skyRgb = Triple(0.16f, 0.24f, 0.32f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.30f, 0.42f, 0.20f)
                // Padma River embankment
                waterZone = Aabb(-31f, -0.5f, -24f, 31f, 0.45f, -14f)
                c.baseMesh.addGroundRect(0f, 0.04f, -19f, 62f, 10f, 0.12f, 0.44f, 0.56f)
                c.addSolidBox(0f, 0.45f, -13.4f, 58f, 0.9f, 1.2f, 0.58f, 0.50f, 0.40f, isVaultable = true)
                // Terracotta heritage building & mango orchard
                c.addCottage(-16f, 16f, 9f, 5.5f, 7f, Triple(0.68f, 0.34f, 0.22f), Triple(0.52f, 0.24f, 0.16f))
                listOf(18f to -4f, 22f to 4f, 18f to 14f, 24f to 18f).forEach { (x, z) ->
                    c.addPalmOrTree(x, z, 4.4f, Triple(0.14f, 0.54f, 0.18f))
                }
                boatSpawn = Vec3(-10f, 0.05f, -19f)
            }

            DistrictBiomeType.RANGPUR_NORTHERN_AGRO -> {
                skyRgb = Triple(0.15f, 0.23f, 0.30f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.26f, 0.48f, 0.20f)
                // Crop paddy patchwork
                c.baseMesh.addGroundRect(-18f, 0.015f, -14f, 18f, 16f, 0.56f, 0.64f, 0.22f)
                c.baseMesh.addGroundRect(18f, 0.015f, 14f, 18f, 16f, 0.18f, 0.54f, 0.20f)
                // Brick rural road & Teesta water canal
                c.baseMesh.addGroundRect(0f, 0.02f, 0f, 7f, 64f, 0.48f, 0.28f, 0.20f)
                waterZone = Aabb(-31f, -0.5f, 6f, -20f, 0.45f, 24f)
                c.baseMesh.addGroundRect(-25.5f, 0.04f, 15f, 11f, 18f, 0.09f, 0.45f, 0.62f)
                c.addCottage(-16f, 16f, 6.5f, 3.6f, 5f, Triple(0.76f, 0.42f, 0.28f), Triple(0.32f, 0.35f, 0.38f))
                c.addCottage(17f, -15f, 7.5f, 4.8f, 5.5f, Triple(0.84f, 0.78f, 0.64f), Triple(0.65f, 0.24f, 0.18f))
                boatSpawn = Vec3(-24.5f, 0.05f, 15f)
            }

            DistrictBiomeType.BARISHAL_RIVER_DELTA -> {
                skyRgb = Triple(0.12f, 0.24f, 0.34f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.18f, 0.36f, 0.22f)
                // Wide Kirtonkhola river + side market canals
                waterZone = Aabb(-31f, -0.5f, -12f, -13f, 0.45f, 22f)
                c.baseMesh.addGroundRect(-22f, 0.04f, 5f, 18f, 34f, 0.06f, 0.42f, 0.62f)
                c.addBridgeSpan(-13f, 2f, 5f, 10f, Triple(0.48f, 0.34f, 0.20f), Triple(0.62f, 0.44f, 0.25f))
                c.addWoodenBoatProp(-18f, -5f, Triple(0.55f, 0.36f, 0.18f))
                c.addWoodenBoatProp(-20f, 12f, Triple(0.18f, 0.48f, 0.62f))
                c.addCottage(18f, -15f, 7f, 3.8f, 5.5f, Triple(0.74f, 0.72f, 0.66f), Triple(0.10f, 0.56f, 0.68f))
                c.addCottage(18f, 15f, 7f, 3.8f, 5.5f, Triple(0.76f, 0.68f, 0.54f), Triple(0.82f, 0.36f, 0.18f))
                boatSpawn = Vec3(-19.5f, 0.05f, 3.5f)
            }

            DistrictBiomeType.INDUSTRIAL_CORRIDOR,
            DistrictBiomeType.BRAHMAPUTRA_RIVERFRONT,
            DistrictBiomeType.DHAKA_METROPOLIS -> {
                skyRgb = Triple(0.12f, 0.19f, 0.27f)
                c.baseMesh.addGroundRect(0f, 0f, 0f, 68f, 68f, 0.20f, 0.30f, 0.22f)
                c.baseMesh.addGroundRect(0f, 0.02f, 0f, 8.5f, 64f, 0.13f, 0.15f, 0.17f)
                waterZone = Aabb(-31f, -0.5f, -6f, -19f, 0.45f, 14f)
                c.baseMesh.addGroundRect(-25f, 0.04f, 4f, 12f, 20f, 0.07f, 0.40f, 0.58f)
                c.addCottage(-19f, -17f, 9f, 6f, 7f, Triple(0.45f, 0.50f, 0.56f), Triple(0.14f, 0.52f, 0.68f))
                c.addCottage(20f, 17f, 9f, 6.5f, 7f, Triple(0.66f, 0.38f, 0.28f), Triple(0.32f, 0.35f, 0.40f))
                c.addPalmOrTree(19f, -17f, 5.0f, Triple(0.12f, 0.50f, 0.20f))
                c.addPalmOrTree(24f, -13f, 4.6f, Triple(0.14f, 0.46f, 0.18f))
                boatSpawn = Vec3(-24.5f, 0.05f, 4f)
            }
        }

        // Common vaultable tactical cover + street lights + beacons + travel portal
        c.addSolidBox(-6.0f, 0.58f, -6.0f, 2.5f, 1.15f, 1.0f, 0.42f, 0.46f, 0.48f, isVaultable = true)
        c.addSolidBox(6.0f, 0.58f, 6.0f, 2.5f, 1.15f, 1.0f, 0.42f, 0.46f, 0.48f, isVaultable = true)
        c.addSolidBox(-6.5f, 0.58f, 6.5f, 1.1f, 1.15f, 2.4f, 0.44f, 0.42f, 0.36f, isVaultable = true)
        c.addSolidBox(6.5f, 0.50f, -6.5f, 2.2f, 1.0f, 2.2f, 0.34f, 0.44f, 0.38f, isVaultable = true)

        val lampCoords = listOf(Vec3(-5f, 0f, -14f), Vec3(5f, 0f, -14f), Vec3(-5f, 0f, 14f), Vec3(5f, 0f, 14f))
        for (lp in lampCoords) {
            c.addSolidBox(lp.x, 2.2f, lp.z, 0.24f, 4.4f, 0.24f, 0.26f, 0.28f, 0.32f)
            c.baseMesh.addBox(lp.x, 4.5f, lp.z, 0.58f, 0.22f, 0.58f, 1.0f, 0.86f, 0.44f)
            lightPositions.add(Vec3(lp.x, 4.5f, lp.z))
        }

        val challengePad = Vec3(0f, 0f, -11.5f)
        val missionPad = Vec3(-10.5f, 0f, 13.5f)
        val portalPad = Vec3(0f, 0f, 27.5f)
        c.baseMesh.addBox(challengePad.x, 0.08f, challengePad.z, 3.0f, 0.16f, 3.0f, 0.05f, 0.72f, 0.45f)
        c.baseMesh.addBox(missionPad.x, 0.09f, missionPad.z, 2.6f, 0.18f, 2.6f, 0.10f, 0.82f, 0.92f)
        c.baseMesh.addBox(portalPad.x, 0.09f, portalPad.z, 5.4f, 0.18f, 3.0f, 0.94f, 0.66f, 0.12f)

        val baseBuf = c.baseMesh.toFloatBuffer()
        val detailBuf = c.detailMesh.toFloatBuffer()
        val chunks = buildList {
            add(
                WorldRenderChunk(
                    id = "${entry.id}_base",
                    bounds = Aabb(-34f, 0f, -34f, 34f, 16f, 34f),
                    buffer = baseBuf,
                    vertexCount = c.baseMesh.vertexCount,
                    isDetailChunk = false
                )
            )
            if (c.detailMesh.vertexCount > 0) {
                add(
                    WorldRenderChunk(
                        id = "${entry.id}_detail",
                        bounds = Aabb(-30f, 0f, -30f, 30f, 14f, 30f),
                        buffer = detailBuf,
                        vertexCount = c.detailMesh.vertexCount,
                        isDetailChunk = true
                    )
                )
            }
        }

        val vehicles = listOf(
            VehicleEntity("${entry.id}_car", VehicleType.CAR, Vec3(10.5f, 0f, 11.5f), yawRad = 0f),
            VehicleEntity("${entry.id}_moto", VehicleType.MOTORCYCLE, Vec3(14.5f, 0f, 10.5f), yawRad = -0.35f),
            VehicleEntity("${entry.id}_truck", VehicleType.TRUCK, Vec3(11.5f, 0f, 18.0f), yawRad = 0.25f),
            VehicleEntity("${entry.id}_boat", VehicleType.BOAT, boatSpawn, yawRad = 0f)
        )

        val targets = listOf(
            RangeTarget("target_1", Vec3(10.0f, 1.3f, -16.0f), isMoving = false),
            RangeTarget("target_2", Vec3(13.0f, 1.3f, -16.0f), isMoving = false),
            RangeTarget("target_moving_1", Vec3(11.5f, 1.9f, -13.5f), isMoving = true)
        )

        val loot = listOf(
            GroundLootItem("${entry.id}_akm", GroundLootKind.WEAPON_CRATE, 7.2f, 0.35f, -7.8f, weaponId = "ar_bengal_akm"),
            GroundLootItem("${entry.id}_scar", GroundLootKind.WEAPON_CRATE, -5.0f, 0.35f, 5.0f, weaponId = "ar_scar"),
            GroundLootItem("${entry.id}_ammo", GroundLootKind.AMMO_BOX, 13.5f, 0.35f, -6.5f, ammoAmount = 60),
            GroundLootItem("${entry.id}_med", GroundLootKind.MEDKIT_DROP, -6.0f, 0.35f, -4.5f, consumable = ConsumableType.FIRST_AID_KIT)
        )

        val estMem = ((c.baseMesh.vertexCount + c.detailMesh.vertexCount) * 36f) / (1024f * 1024f) + 12.5f
        return DhakaWorldGeometry(
            staticBuffer = baseBuf,
            staticVertexCount = c.baseMesh.vertexCount,
            colliders = c.colliders,
            vaultables = c.vaultables,
            waterZone = waterZone,
            initialRangeTargets = targets,
            initialVehicles = vehicles,
            initialLoot = loot,
            npcCommanderPos = Vec3(7.0f, 0f, -10.5f),
            weaponBenchPos = Vec3(8.5f, 0f, -7.8f),
            ammoStationPos = Vec3(14.5f, 0f, -7.8f),
            aiPracticePos = Vec3(17.5f, 0f, -9.5f),
            missionBeaconPos = challengePad,
            streetLightPositions = lightPositions,
            districtId = entry.id,
            biome = entry.biome,
            skyRgb = skyRgb,
            missionObjectivePos = missionPad,
            travelPortalPos = portalPad,
            spatialChunks = chunks,
            estimatedMemoryMb = estMem
        )
    }
}
