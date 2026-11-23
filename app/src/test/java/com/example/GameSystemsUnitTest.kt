package com.example

import com.example.game.ai.AiDifficulty
import com.example.game.ai.DhakaNavGrid
import com.example.game.ai.EnemyAiAgent
import com.example.game.ai.EnemyAiState
import com.example.game.challenges.ChallengeOutcome
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.dialogue.DialogueCatalog
import com.example.game.districts.DistrictBiomeType
import com.example.game.districts.DistrictBuildStatus
import com.example.game.districts.DistrictCatalog
import com.example.game.districts.Division
import com.example.game.districts.SequentialUnlockConfig
import com.example.game.engine.EnemyAiStepper
import com.example.game.engine.GameSimulation
import com.example.game.engine.MovementStance
import com.example.game.engine.NearbyInteraction
import com.example.game.engine.Projectile
import com.example.game.engine.Vec3
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.FireMode
import com.example.game.inventory.InventoryCatalog
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponCategory
import com.example.game.inventory.WeaponLoadoutAttachments
import com.example.game.inventory.WeaponSlotType
import com.example.game.localization.AppLanguage
import com.example.game.modes.GameModeId
import com.example.game.multiplayer.AuthoritativeMatchServer
import com.example.game.multiplayer.ChatChannel
import com.example.game.multiplayer.ConnectionState
import com.example.game.multiplayer.MatchPhase
import com.example.game.multiplayer.RoomPrivacy
import com.example.game.multiplayer.ServerAuthorityValidator
import com.example.game.multiplayer.SocketMultiplayerClient
import com.example.game.multiplayer.TeamSide
import com.example.game.save.GraphicsQuality
import com.example.game.vehicles.VehicleType
import com.example.game.world.DistrictTravelGraph
import com.example.game.world.DistrictWorldBuilder
import com.example.game.world.LodLevel
import com.example.game.world.PlatformTarget
import com.example.game.world.WorldStreamingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSystemsUnitTest {

    private fun startTestSimulation(
        mode: GameModeId = GameModeId.SOLO,
        primaryId: String = "ar_dhaka_m4",
        secondaryId: String = "pistol_p92",
        meleeId: String = "melee_knife",
        attachments: WeaponLoadoutAttachments = WeaponLoadoutAttachments(),
        difficulty: AiDifficulty = AiDifficulty.NORMAL
    ): GameSimulation {
        val sim = GameSimulation()
        sim.startSession(
            mode = mode,
            character = CharacterCatalog.slots.first(),
            outfit = OutfitStyle.DHAKA_EMERALD,
            headwear = HeadwearOption.TACTICAL_CAP,
            accessory = AccessoryOption.TACTICAL_BACKPACK,
            colorTint = CharacterColorTint.DEFAULT,
            primary = InventoryCatalog.getWeapon(primaryId),
            secondary = InventoryCatalog.getWeapon(secondaryId),
            melee = InventoryCatalog.getWeapon(meleeId),
            loadoutAttachments = attachments,
            difficulty = difficulty,
            sensitivity = 1.0f,
            invertY = false,
            audioEnabled = false
        )
        return sim
    }

    @Test
    fun `district catalog covers all 8 divisions and 64 districts with 10 playable streamed biomes and 54 staged`() {
        assertEquals(8, Division.entries.size)
        assertEquals(64, DistrictCatalog.allDistricts.size)
        assertEquals(64, DistrictCatalog.allDistricts.map { it.id }.toSet().size)

        val expectedDivisionCounts = mapOf(
            Division.DHAKA to 13,
            Division.CHATTOGRAM to 11,
            Division.RAJSHAHI to 8,
            Division.KHULNA to 10,
            Division.BARISHAL to 6,
            Division.SYLHET to 4,
            Division.RANGPUR to 8,
            Division.MYMENSINGH to 4
        )
        expectedDivisionCounts.forEach { (division, expected) ->
            assertEquals(expected, DistrictCatalog.allDistricts.count { it.division == division })
        }

        val dhaka = DistrictCatalog.getById("dhaka")
        assertTrue(dhaka.isEnvironmentReady)
        assertEquals(DistrictBuildStatus.PLAYABLE_STREAMED, dhaka.buildStatus)
        assertEquals(10, DistrictCatalog.allDistricts.count { it.isEnvironmentReady })
        assertEquals(54, DistrictCatalog.allDistricts.count { it.buildStatus == DistrictBuildStatus.STAGED_FUTURE_BATCH })
    }

    @Test
    fun `character catalog has 4 fictional slots and game modes expose 2 playable modes`() {
        assertEquals(4, CharacterCatalog.slots.size)
        assertEquals(
            listOf("Hero Alam", "Khairul Dewan", "Gaming With Talha", "IShowSpeed"),
            CharacterCatalog.slots.map { it.displayName }
        )
        assertTrue(CharacterCatalog.slots.all { it.isFictionalPlaceholder })

        val playableModes = GameModeId.entries.filter { it.isPlayableInMilestone1 }
        assertEquals(setOf(GameModeId.SOLO, GameModeId.TRAINING_GROUND), playableModes.toSet())
    }

    @Test
    fun `bilingual NPC dialogue matches exact Bengali and English prompt text`() {
        val welcome = DialogueCatalog.trainingOfficerLines.first()
        assertEquals("ঢাকার ট্রেনিং গ্রাউন্ডে স্বাগতম। তোমার অভিযান শুরু করো।", welcome.text(AppLanguage.BENGALI))
        assertEquals("Welcome to the Dhaka Training Ground. Begin your mission.", welcome.text(AppLanguage.ENGLISH))
    }

    @Test
    fun `modular weapon catalog provides all 8 categories and 26 distinct prototype weapons`() {
        assertEquals(8, WeaponCategory.entries.size)
        assertEquals(26, InventoryCatalog.weapons.size)
        assertEquals(26, InventoryCatalog.weapons.map { it.id }.toSet().size)

        assertEquals(5, InventoryCatalog.byCategory(WeaponCategory.ASSAULT_RIFLE).size)
        assertEquals(4, InventoryCatalog.byCategory(WeaponCategory.SMG).size)
        assertEquals(3, InventoryCatalog.byCategory(WeaponCategory.SNIPER).size)
        assertEquals(3, InventoryCatalog.byCategory(WeaponCategory.DMR).size)
        assertEquals(3, InventoryCatalog.byCategory(WeaponCategory.SHOTGUN).size)
        assertEquals(2, InventoryCatalog.byCategory(WeaponCategory.LMG).size)
        assertEquals(3, InventoryCatalog.byCategory(WeaponCategory.PISTOL).size)
        assertEquals(3, InventoryCatalog.byCategory(WeaponCategory.MELEE).size)

        // Legacy Milestone 1 IDs resolve gracefully
        assertEquals("ar_dhaka_m4", InventoryCatalog.getWeapon("bd74_carbine").id)
        assertEquals("pistol_p92", InventoryCatalog.getWeapon("dhaka9_pistol").id)
        assertEquals("dmr_mini14", InventoryCatalog.getWeapon("range_marksman").id)
    }

    @Test
    fun `weapon attachments scale magazine size, recoil, spread, and ADS FOV`() {
        val baseAttachments = WeaponLoadoutAttachments(
            optic = OpticAttachment.NONE,
            muzzle = MuzzleAttachment.NONE,
            magazine = MagazineAttachment.STANDARD,
            underbarrel = UnderbarrelAttachment.NONE
        )
        val upgraded = WeaponLoadoutAttachments(
            optic = OpticAttachment.SCOPE_4X,
            muzzle = MuzzleAttachment.COMPENSATOR,
            magazine = MagazineAttachment.EXTENDED,
            underbarrel = UnderbarrelAttachment.FOREGRIP
        )

        assertEquals(30, baseAttachments.effectiveMagSize(30))
        assertEquals(42, upgraded.effectiveMagSize(30))
        assertTrue(upgraded.effectiveRecoil(0.20f) < baseAttachments.effectiveRecoil(0.20f))
        assertTrue(upgraded.effectiveSpread(2.0f, isAiming = true) < baseAttachments.effectiveSpread(2.0f, isAiming = true))
        assertEquals(28f, upgraded.optic.adsFov, 0.01f)
    }

    @Test
    fun `simulation handles walk, sprint, swim, vault, crouch, 1P-3P camera, and challenge victory`() {
        val sim = startTestSimulation(mode = GameModeId.SOLO)

        val startPos = sim.playerPos
        sim.setMoveInput(0f, 1f)
        repeat(15) { sim.step(0.016f) }
        assertTrue(sim.playerPos.distanceTo(startPos) > 0.2f)

        sim.setSprint(true)
        assertTrue(sim.snapshot().isSprinting)

        sim.toggleCameraMode()
        assertTrue(sim.snapshot().isFirstPerson)

        sim.toggleCrouch()
        assertTrue(sim.snapshot().isCrouching)

        // Test Vault over tactical cover at (6.0f, 0.50f, -6.0f)
        sim.toggleCrouch()
        sim.isGrounded = true
        sim.playerPos = Vec3(6.0f, 0f, -3.9f)
        sim.playerYawRad = 0f
        sim.jumpOrVault()
        assertTrue(sim.snapshot().isVaulting)
        assertEquals(MovementStance.VAULTING, sim.snapshot().stance)
        repeat(30) { sim.step(0.02f) }
        assertFalse(sim.snapshot().isVaulting)

        // Test Swimming inside Buriganga Canal waterZone
        sim.playerPos = Vec3(-24.0f, 0f, 3.0f)
        sim.step(0.02f)
        assertTrue(sim.snapshot().isSwimming)
        assertEquals(MovementStance.SWIMMING, sim.snapshot().stance)

        // Eliminate 5 of 6 AI raiders, then shoot the last AI raider to verify Challenge Victory
        sim.playerPos = Vec3(0f, 0f, 11f)
        sim.step(0.02f)
        for (i in 0 until sim.aiAgents.size - 1) {
            sim.aiAgents[i].hp = 0
            sim.aiAgents[i].aiState = EnemyAiState.DEAD
        }
        val lastAgent = sim.aiAgents.last()
        lastAgent.hp = 10
        sim.isFirstPerson = true
        sim.playerPos = Vec3(lastAgent.pos.x, 0f, lastAgent.pos.z + 3f)
        sim.playerYawRad = 0f
        sim.playerPitchRad = -0.05f
        sim.fireCooldownSec = 0f
        sim.step(0.016f)
        sim.fireWeapon()

        val snap = sim.snapshot()
        assertNotNull(snap)
        assertEquals(6, snap.enemiesEliminated)
        assertEquals(ChallengeOutcome.VICTORY, snap.outcome)
    }

    @Test
    fun `combat mechanics support 3-slot switching, fire modes, reload, melee, consumables, and ground loot`() {
        val sim = startTestSimulation(
            mode = GameModeId.TRAINING_GROUND,
            attachments = WeaponLoadoutAttachments(magazine = MagazineAttachment.EXTENDED)
        )
        assertEquals(42, sim.snapshot().magSize)
        assertEquals(FireMode.AUTO, sim.snapshot().activeFireMode)

        // Cycle fire mode AUTO -> BURST -> SEMI
        sim.cycleFireMode()
        assertEquals(FireMode.BURST, sim.snapshot().activeFireMode)
        sim.cycleFireMode()
        assertEquals(FireMode.SEMI, sim.snapshot().activeFireMode)

        // Aiming + firing + reloading decrements reserve ammo and restores mag
        sim.toggleAim()
        assertTrue(sim.snapshot().isAiming)
        sim.fireWeapon()
        assertEquals(41, sim.snapshot().ammoInMag)
        val reserveBefore = sim.snapshot().reserveAmmo
        sim.reloadWeapon()
        assertTrue(sim.snapshot().isReloading)
        repeat(90) { sim.step(0.02f) }
        assertFalse(sim.snapshot().isReloading)
        assertEquals(42, sim.snapshot().ammoInMag)
        assertEquals(reserveBefore - 1, sim.snapshot().reserveAmmo)

        // Switch to secondary & melee
        sim.switchWeaponSlot(WeaponSlotType.SECONDARY)
        assertEquals(WeaponSlotType.SECONDARY, sim.snapshot().activeSlot)
        sim.switchWeaponSlot(WeaponSlotType.MELEE)
        assertEquals(WeaponSlotType.MELEE, sim.snapshot().activeSlot)

        // Spawn Training Ground sparring bot and strike with melee
        sim.spawnPracticeBot()
        assertEquals(1, sim.aiAgents.count { it.isAlive })
        val bot = sim.aiAgents.first()
        val initialBotHp = bot.hp
        sim.playerPos = Vec3(bot.pos.x, 0f, bot.pos.z + 1.5f)
        sim.playerYawRad = 0f
        sim.fireWeapon()
        assertTrue(bot.hp < initialBotHp)

        // Consumables restore health & stamina
        sim.health = 50
        sim.stamina = 40f
        assertTrue(sim.useConsumable(ConsumableType.BANDAGE))
        assertEquals(75, sim.snapshot().health)
        assertTrue(sim.useConsumable(ConsumableType.ENERGY_DRINK))
        assertEquals(85, sim.snapshot().health)
        assertEquals(85, sim.snapshot().stamina)

        // Ground loot pickup & dropping active weapon
        sim.switchWeaponSlot(WeaponSlotType.PRIMARY)
        val akmLoot = sim.lootItems.first { it.id == "loot_akm" }
        sim.playerPos = Vec3(akmLoot.x, 0f, akmLoot.z)
        assertEquals(NearbyInteraction.LOOT_NEARBY, sim.snapshot().nearbyInteraction)
        assertTrue(sim.pickupNearbyLoot())
        assertEquals("ar_bengal_akm", sim.snapshot().primaryWeapon.id)

        val countBeforeDrop = sim.lootItems.count { !it.collected }
        assertTrue(sim.dropActiveItem())
        assertEquals(countBeforeDrop + 1, sim.lootItems.count { !it.collected })
    }

    @Test
    fun `all 5 vehicle types drive, steer, consume fuel, reverse, and exit cleanly`() {
        val sim = startTestSimulation(mode = GameModeId.TRAINING_GROUND)
        assertEquals(VehicleType.entries.toSet(), sim.vehicles.map { it.type }.toSet())

        for (veh in sim.vehicles) {
            sim.playerPos = Vec3(veh.pos.x + 0.5f, 0f, veh.pos.z + 0.5f)
            assertTrue("Should enter ${veh.type}", sim.enterOrExitVehicle())
            assertEquals(veh.id, sim.snapshot().activeVehicle?.id)
            assertEquals(MovementStance.DRIVING, sim.snapshot().stance)

            val startPos = veh.pos
            val startFuel = veh.fuel
            val startYaw = veh.yawRad
            sim.setVehicleInputs(throttle = 1f, brakeReverse = 0f, steer = 0.7f)
            repeat(30) { sim.step(0.02f) }
            assertTrue("${veh.type} should move", veh.pos.distanceTo(startPos) > 0.2f)
            assertTrue("${veh.type} should turn", veh.yawRad != startYaw)
            assertTrue("${veh.type} should consume fuel", veh.fuel < startFuel)

            // Brake and reverse
            sim.setVehicleInputs(throttle = 0f, brakeReverse = 1f, steer = 0f)
            repeat(40) { sim.step(0.02f) }
            assertTrue("${veh.type} should reverse", veh.speed < 0f)

            assertTrue("Should exit ${veh.type}", sim.enterOrExitVehicle())
            assertEquals(null, sim.snapshot().activeVehicle)
        }
    }

    @Test
    fun `enemy AI transitions through Idle, Patrol, Detect, Attack, Search, Retreat, and Dead states`() {
        val sim = startTestSimulation(mode = GameModeId.SOLO, difficulty = AiDifficulty.HARD)
        val navGrid = DhakaNavGrid(sim.world.colliders)
        val agent = EnemyAiAgent(
            id = 99,
            callsign = "TestRaider",
            patrolPoints = listOf(Vec3(4f, 0f, 0f), Vec3(8f, 0f, 0f)),
            pos = Vec3(0f, 0f, 0f),
            hp = 100,
            maxHp = 100,
            aiState = EnemyAiState.IDLE
        )

        // IDLE -> PATROL after idle timer
        repeat(100) {
            EnemyAiStepper.stepAgents(
                agents = listOf(agent),
                playerPos = Vec3(25f, 0f, 25f),
                playerNoiseRadius = 2f,
                difficulty = AiDifficulty.NORMAL,
                navGrid = navGrid,
                world = sim.world,
                dt = 0.02f,
                onFireProjectile = { _, _, _ -> }
            )
        }
        assertEquals(EnemyAiState.PATROL, agent.aiState)

        // PATROL -> DETECT -> ATTACK when player enters open line of sight
        var firedShots = 0
        repeat(35) {
            EnemyAiStepper.stepAgents(
                agents = listOf(agent),
                playerPos = Vec3(0f, 0f, 8f),
                playerNoiseRadius = 5f,
                difficulty = AiDifficulty.NORMAL,
                navGrid = navGrid,
                world = sim.world,
                dt = 0.02f,
                onFireProjectile = { _, _, _ -> firedShots++ }
            )
        }
        assertTrue(agent.aiState == EnemyAiState.ATTACK || agent.aiState == EnemyAiState.DETECT)

        // Step past fireCooldown in ATTACK to verify enemy fires
        repeat(80) {
            EnemyAiStepper.stepAgents(
                agents = listOf(agent),
                playerPos = Vec3(0f, 0f, 8f),
                playerNoiseRadius = 5f,
                difficulty = AiDifficulty.NORMAL,
                navGrid = navGrid,
                world = sim.world,
                dt = 0.02f,
                onFireProjectile = { _, _, _ -> firedShots++ }
            )
        }
        assertEquals(EnemyAiState.ATTACK, agent.aiState)
        assertTrue(firedShots > 0)

        // Low HP triggers RETREAT
        agent.hp = 20
        EnemyAiStepper.stepAgents(
            agents = listOf(agent),
            playerPos = Vec3(0f, 0f, 8f),
            playerNoiseRadius = 5f,
            difficulty = AiDifficulty.NORMAL,
            navGrid = navGrid,
            world = sim.world,
            dt = 0.02f,
            onFireProjectile = { _, _, _ -> }
        )
        assertEquals(EnemyAiState.RETREAT, agent.aiState)

        // Losing sight transitions ATTACK -> SEARCH
        agent.hp = 90
        agent.aiState = EnemyAiState.ATTACK
        EnemyAiStepper.stepAgents(
            agents = listOf(agent),
            playerPos = Vec3(-18f, 0f, -18f), // Behind Dhaka residential building
            playerNoiseRadius = 1f,
            difficulty = AiDifficulty.NORMAL,
            navGrid = navGrid,
            world = sim.world,
            dt = 0.02f,
            onFireProjectile = { _, _, _ -> }
        )
        assertEquals(EnemyAiState.SEARCH, agent.aiState)

        // Zero HP transitions -> DEAD
        agent.hp = 0
        EnemyAiStepper.stepAgents(
            agents = listOf(agent),
            playerPos = Vec3(0f, 0f, 8f),
            playerNoiseRadius = 5f,
            difficulty = AiDifficulty.NORMAL,
            navGrid = navGrid,
            world = sim.world,
            dt = 0.02f,
            onFireProjectile = { _, _, _ -> }
        )
        assertEquals(EnemyAiState.DEAD, agent.aiState)
    }

    @Test
    fun `player defeat enters spectator mode`() {
        val sim = startTestSimulation(mode = GameModeId.SOLO)
        sim.health = 5
        sim.projectiles.add(
            Projectile(
                pos = Vec3(sim.playerPos.x, 1.0f, sim.playerPos.z - 0.1f),
                vel = Vec3(0f, 0f, 10f),
                fromPlayer = false,
                lifeSec = 1f,
                damage = 25,
                rgb = Triple(1f, 0f, 0f)
            )
        )
        sim.step(0.02f)
        assertEquals(0, sim.health)
        assertEquals(ChallengeOutcome.SPECTATOR_DEFEAT, sim.snapshot().outcome)
        assertFalse(sim.snapshot().outcome == ChallengeOutcome.IN_PROGRESS)
    }

    @Test
    fun `four-player socket test verifies room join, separate characters, teams, ready, movement sync, chat, anti-cheat, and reconnect`() = runBlocking {
        val server = AuthoritativeMatchServer()
        val port = server.start()
        val clients = List(4) { SocketMultiplayerClient() }
        try {
            val charIds = listOf("hero_alam", "khairul_dewan", "gaming_with_talha", "ishowspeed")
            clients.forEachIndexed { idx, c ->
                val res = c.connectToServer(
                    host = "127.0.0.1",
                    port = port,
                    playerId = "sq_${idx + 1}",
                    username = "Warrior${idx + 1}",
                    characterId = charIds[idx],
                    weaponId = "ar_dhaka_m4"
                )
                assertTrue("Client ${idx + 1} should connect", res.isSuccess)
            }
            delay(100L)

            // Host creates a 4-player Squad room
            clients[0].createRoom(RoomPrivacy.PUBLIC, GameModeId.FOUR_PLAYER_SQUAD, "dhaka", 4)
            var roomCode = ""
            repeat(25) {
                delay(40L)
                val code = clients[0].activeRoomFlow.value?.roomCode
                if (!code.isNullOrBlank()) {
                    roomCode = code
                    return@repeat
                }
            }
            assertTrue("Room code should be assigned", roomCode.startsWith("BD"))

            // Other 3 players join by code and pick distinct characters & teams
            for (i in 1..3) {
                clients[i].joinRoomByCode(roomCode)
                delay(60L)
                clients[i].setCharacterAndWeapon(charIds[i], "ar_dhaka_m4")
                clients[i].setTeam(if (i == 1) TeamSide.ALPHA else TeamSide.BRAVO)
                clients[i].setLocalReady(true)
            }
            clients[0].setCharacterAndWeapon(charIds[0], "ar_dhaka_m4")
            clients[0].setTeam(TeamSide.ALPHA)
            clients[0].setLocalReady(true)

            repeat(25) {
                delay(40L)
                val slots = clients[0].activeRoomFlow.value?.players.orEmpty()
                if (slots.size == 4 && slots.all { it.isReady }) return@repeat
            }

            val lobbyRoom = clients[0].activeRoomFlow.value
            assertNotNull(lobbyRoom)
            assertEquals(4, lobbyRoom!!.players.size)
            assertEquals(charIds.toSet(), lobbyRoom.players.map { it.characterId }.toSet())
            assertTrue(lobbyRoom.players.all { it.isReady })
            assertEquals(2, lobbyRoom.players.count { it.team == TeamSide.ALPHA })
            assertEquals(2, lobbyRoom.players.count { it.team == TeamSide.BRAVO })

            // Host starts match
            clients[0].startMatch()
            repeat(25) {
                delay(40L)
                if (clients[3].worldStateFlow.value?.phase == MatchPhase.IN_MATCH) return@repeat
            }
            assertEquals(MatchPhase.IN_MATCH, clients[3].worldStateFlow.value?.phase)

            // Movement synchronization across sockets (incremental steps respecting server speed authority)
            val sq2Start = clients[0].worldStateFlow.value!!.players.first { it.playerId == "sq_2" }
            for (step in 1..5) {
                clients[1].sendMove(sq2Start.x + step * 0.45f, 0f, sq2Start.z - step * 0.25f, 0.5f, "ar_dhaka_m4")
                delay(65L)
            }
            repeat(20) {
                delay(50L)
                val sq2 = clients[0].worldStateFlow.value?.players?.firstOrNull { it.playerId == "sq_2" }
                if (sq2 != null && sq2.x > sq2Start.x + 1.2f) return@repeat
            }
            val syncedSq2 = clients[0].worldStateFlow.value?.players?.first { it.playerId == "sq_2" }!!
            assertTrue("Movement should synchronize via server tick", syncedSq2.x > sq2Start.x + 1.2f)

            // Team chat isolation & private chat routing
            clients[0].sendChat(ChatChannel.TEAM, "", "Alpha push plaza!")
            clients[1].sendChat(ChatChannel.PRIVATE, "sq_4", "Watch flank!")
            delay(140L)
            assertTrue(clients[1].chatMessagesFlow.value.any { it.text == "Alpha push plaza!" })
            assertFalse(clients[2].chatMessagesFlow.value.any { it.text == "Alpha push plaza!" })
            assertTrue(clients[3].chatMessagesFlow.value.any { it.text == "Watch flank!" })

            // Server-authoritative weapon hit & anti-cheat tamper rejection
            val p3Before = clients[0].worldStateFlow.value!!.players.first { it.playerId == "sq_3" }
            val p1Now = clients[0].worldStateFlow.value!!.players.first { it.playerId == "sq_1" }
            clients[0].sendFire(p3Before.x - p1Now.x, 0f, p3Before.z - p1Now.z)
            clients[1].sendInvalidRewardTamperForTest()
            delay(160L)
            val p3After = clients[0].worldStateFlow.value!!.players.first { it.playerId == "sq_3" }
            assertTrue("Server-calculated raycast should damage enemy team target", p3After.health < p3Before.health)
            assertTrue("Server audit log should capture tampered reward claim", server.auditLogs.any { it.category == "ANTICHEAT_TAMPER" })

            // Speedhack clamp verification
            val (clampedX, _) = ServerAuthorityValidator.validateStepMovement(0f, 0f, 500f, 0f, 0.1f)
            assertTrue("Speed validator must clamp teleport delta", clampedX < 2.0f)

            // Disconnect + Session Reconnect Recovery
            clients[3].simulateConnectionDrop()
            assertEquals(ConnectionState.DISCONNECTED, clients[3].connectionFlow.value)
            val recRes = clients[3].reconnectSession()
            assertTrue(recRes.isSuccess)
            delay(120L)
            assertEquals(ConnectionState.IN_ROOM, clients[3].connectionFlow.value)
            assertNotNull(clients[3].activeRoomFlow.value)
        } finally {
            clients.forEach { it.shutdown() }
            server.stop()
        }
    }

    @Test
    fun `regional biome builder creates distinct 3D district worlds with spatial chunks and sky tint`() {
        val dhakaWorld = DistrictWorldBuilder.buildForDistrict("dhaka", LodLevel.LOD_HIGH)
        val coxsWorld = DistrictWorldBuilder.buildForDistrict("coxs_bazar", LodLevel.LOD_HIGH)
        val sylhetWorld = DistrictWorldBuilder.buildForDistrict("sylhet", LodLevel.LOD_HIGH)
        val khulnaWorld = DistrictWorldBuilder.buildForDistrict("khulna", LodLevel.LOD_HIGH)
        val chattogramWorld = DistrictWorldBuilder.buildForDistrict("chattogram", LodLevel.LOD_HIGH)
        val barishalWorld = DistrictWorldBuilder.buildForDistrict("barishal", LodLevel.LOD_HIGH)

        assertEquals(DistrictBiomeType.DHAKA_METROPOLIS, dhakaWorld.biome)
        assertEquals(DistrictBiomeType.COXS_BAZAR_COAST, coxsWorld.biome)
        assertEquals(DistrictBiomeType.SYLHET_TEA_HIGHLANDS, sylhetWorld.biome)
        assertEquals(DistrictBiomeType.KHULNA_SUNDARBANS, khulnaWorld.biome)
        assertEquals(DistrictBiomeType.CHATTOGRAM_PORT_HILLS, chattogramWorld.biome)
        assertEquals(DistrictBiomeType.BARISHAL_RIVER_DELTA, barishalWorld.biome)

        assertNotEquals(dhakaWorld.skyRgb, coxsWorld.skyRgb)
        assertNotEquals(sylhetWorld.skyRgb, khulnaWorld.skyRgb)
        assertTrue(coxsWorld.spatialChunks.isNotEmpty())
        assertTrue(sylhetWorld.colliders.isNotEmpty())
        assertTrue(barishalWorld.waterZone.maxX > barishalWorld.waterZone.minX)
        assertTrue(khulnaWorld.initialVehicles.any { it.type == VehicleType.BOAT })
    }

    @Test
    fun `world streaming manager loads asynchronously, enforces LRU eviction under memory budget, and culls LOD chunks`() = runBlocking {
        val manager = WorldStreamingManager()
        manager.configureQuality(GraphicsQuality.MEDIUM, PlatformTarget.ANDROID)

        val sequence = listOf("dhaka", "gazipur", "chattogram", "coxs_bazar", "sylhet", "khulna")
        for (id in sequence) {
            val world = manager.streamToDistrict(id, GraphicsQuality.MEDIUM)
            assertEquals(id, world.districtId)
            assertTrue("Cache size should never exceed 2 active districts", manager.loadedCount <= 2)
        }

        val telemetry = manager.telemetry.value
        assertEquals("khulna", telemetry.activeDistrictId)
        assertTrue("Streaming should evict older districts", telemetry.totalUnloads >= 4)
        assertTrue(telemetry.estimatedMemoryMb <= telemetry.budgetMb.toFloat())

        // Verify LOD_LOW disables detail chunks and frustum culling hides far chunks behind camera
        manager.configureQuality(GraphicsQuality.LOW, PlatformTarget.ANDROID)
        val lowLodWorld = manager.streamToDistrict("chattogram", GraphicsQuality.LOW)
        val visibility = manager.evaluateChunksVisibility(
            world = lowLodWorld,
            cameraPos = Vec3(0f, 3f, 20f),
            cameraForward = Vec3(0f, 0f, 1f)
        )
        assertEquals(lowLodWorld.spatialChunks.size, visibility.size)
        lowLodWorld.spatialChunks.forEachIndexed { idx, chunk ->
            if (chunk.isDetailChunk) {
                assertFalse("LOD_LOW must cull detail chunk ${chunk.id}", visibility[idx])
            }
        }
    }

    @Test
    fun `travel graph connects all 64 districts and sequential unlock requires both mission and challenge`() {
        // BFS reachability from Dhaka to every single one of the 64 districts
        DistrictCatalog.allDistricts.forEach { target ->
            val route = DistrictTravelGraph.findShortestRoute("dhaka", target.id)
            assertTrue("Dhaka must connect to ${target.id}", route.isNotEmpty())
            assertEquals("dhaka", route.first())
            assertEquals(target.id, route.last())
        }
        assertTrue(DistrictTravelGraph.canFastTravel("dhaka", setOf("dhaka", "gazipur")))
        assertFalse(DistrictTravelGraph.canFastTravel("coxs_bazar", setOf("dhaka", "gazipur")))

        // Completion math: 0 -> 50 -> 100
        assertEquals(0, SequentialUnlockConfig.completionPercent("dhaka", emptySet(), emptySet()))
        assertEquals(50, SequentialUnlockConfig.completionPercent("dhaka", setOf("dhaka"), emptySet()))
        assertEquals(50, SequentialUnlockConfig.completionPercent("dhaka", emptySet(), setOf("dhaka")))
        assertEquals(100, SequentialUnlockConfig.completionPercent("dhaka", setOf("dhaka"), setOf("dhaka")))

        // Configurable unlock order
        val customOrder = listOf("dhaka", "sylhet") +
            SequentialUnlockConfig.defaultOrder.filter { it != "dhaka" && it != "sylhet" }
        assertEquals("sylhet", DistrictCatalog.nextDistrictAfter("dhaka", customOrder)?.id)
    }

    @Test
    fun `object pools recycle projectiles and impacts and simulation detects mission and travel portal`() {
        val sim = startTestSimulation(mode = GameModeId.SOLO)
        sim.loadStreamedWorld(DistrictWorldBuilder.buildForDistrict("coxs_bazar", LodLevel.LOD_MEDIUM))
        assertEquals("coxs_bazar", sim.snapshot().activeDistrictId)

        // Fire shots and step until projectiles/impacts expire and return to pool
        repeat(4) {
            sim.fireCooldownSec = 0f
            sim.fireWeapon()
        }
        repeat(150) { sim.step(0.02f) }
        repeat(4) {
            sim.fireCooldownSec = 0f
            sim.fireWeapon()
        }
        assertTrue("Projectile/impact pools should recycle instances", sim.snapshot().poolRecycledCount > 0)

        // Proximity detection for District Mission Objective & Border Travel Portal
        sim.playerPos = Vec3(sim.world.missionObjectivePos.x, 0f, sim.world.missionObjectivePos.z)
        assertEquals(NearbyInteraction.MISSION_OBJECTIVE, sim.snapshot().nearbyInteraction)
        assertTrue(sim.completeDistrictMissionObjective())
        assertTrue(sim.snapshot().districtMissionCompleted)

        sim.playerPos = Vec3(sim.world.travelPortalPos.x, 0f, sim.world.travelPortalPos.z)
        assertEquals(NearbyInteraction.TRAVEL_PORTAL, sim.snapshot().nearbyInteraction)
    }
}

