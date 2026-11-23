package com.example.game.engine

import com.example.game.ai.AiDifficulty
import com.example.game.ai.DhakaNavGrid
import com.example.game.ai.EnemyAiAgent
import com.example.game.ai.EnemyAiState
import com.example.game.audio.SoundEffectsEngine
import com.example.game.challenges.ChallengeOutcome
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.CharacterSlot
import com.example.game.characters.EmoteAction
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.EquipmentTier
import com.example.game.inventory.FireMode
import com.example.game.inventory.GroundLootItem
import com.example.game.inventory.GroundLootKind
import com.example.game.inventory.InventoryCatalog
import com.example.game.inventory.WeaponLoadoutAttachments
import com.example.game.inventory.WeaponSlotType
import com.example.game.inventory.WeaponSpec
import com.example.game.modes.GameModeId
import com.example.game.multiplayer.RemotePlayerSnapshot
import com.example.game.vehicles.VehicleEntity
import com.example.game.vehicles.VehicleType
import com.example.game.world.ObjectPool
import com.example.game.world.WorldStreamingManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class MovementStance {
    IDLE,
    WALK,
    RUN,
    SPRINT,
    CROUCH,
    SWIMMING,
    VAULTING,
    DRIVING
}

enum class NearbyInteraction {
    NONE,
    NPC_COMMANDER,
    WEAPON_BENCH,
    AMMO_STATION,
    AI_PRACTICE_PAD,
    MISSION_BEACON,
    MISSION_OBJECTIVE,
    TRAVEL_PORTAL,
    VEHICLE_NEARBY,
    LOOT_NEARBY
}

data class Projectile(
    var pos: Vec3,
    var vel: Vec3,
    var fromPlayer: Boolean,
    var lifeSec: Float,
    var damage: Int,
    var rgb: Triple<Float, Float, Float>
)

data class HudSnapshot(
    val mode: GameModeId = GameModeId.TRAINING_GROUND,
    val activeDistrictId: String = "dhaka",
    val districtMissionCompleted: Boolean = false,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val stamina: Int = 100,
    val armorTier: EquipmentTier = EquipmentTier.VEST_MK1,
    val activeSlot: WeaponSlotType = WeaponSlotType.PRIMARY,
    val activeWeapon: WeaponSpec = InventoryCatalog.weapons.first(),
    val primaryWeapon: WeaponSpec = InventoryCatalog.weapons.first(),
    val secondaryWeapon: WeaponSpec = InventoryCatalog.getWeapon("pistol_p92"),
    val meleeWeapon: WeaponSpec = InventoryCatalog.getWeapon("melee_knife"),
    val activeFireMode: FireMode = FireMode.AUTO,
    val ammoInMag: Int = 30,
    val magSize: Int = 30,
    val reserveAmmo: Int = 120,
    val isReloading: Boolean = false,
    val isAiming: Boolean = false,
    val isFirstPerson: Boolean = false,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val isSwimming: Boolean = false,
    val isVaulting: Boolean = false,
    val stance: MovementStance = MovementStance.IDLE,
    val nearbyInteraction: NearbyInteraction = NearbyInteraction.NONE,
    val nearbyVehicle: VehicleEntity? = null,
    val activeVehicle: VehicleEntity? = null,
    val nearbyLoot: GroundLootItem? = null,
    val bandagesCount: Int = 2,
    val firstAidCount: Int = 1,
    val energyCount: Int = 1,
    val damageFlashAlpha: Float = 0f,
    val hitMarkerAlpha: Float = 0f,
    val trainingHitsSession: Int = 0,
    val enemiesEliminated: Int = 0,
    val totalEnemies: Int = 6,
    val activeAiCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val outcome: ChallengeOutcome = ChallengeOutcome.IN_PROGRESS,
    val isMultiplayerSession: Boolean = false,
    val remotePlayersCount: Int = 0,
    val poolRecycledCount: Int = 0
)

class GameSimulation(
    initialWorld: DhakaWorldGeometry = DhakaWorldData.build(),
    val soundEngine: SoundEffectsEngine = SoundEffectsEngine(),
    val streamingManager: WorldStreamingManager = WorldStreamingManager()
) {
    val lock = Any()
    var world: DhakaWorldGeometry = initialWorld
    private var navGrid = DhakaNavGrid(world.colliders)
    private val rng = Random(42)

    // Session configuration
    var activeMode: GameModeId = GameModeId.TRAINING_GROUND
    var activeCharacter: CharacterSlot = CharacterCatalog.slots.first()
    var activeOutfit: OutfitStyle = OutfitStyle.DHAKA_EMERALD
    var activeHeadwear: HeadwearOption = HeadwearOption.NONE
    var activeAccessory: AccessoryOption = AccessoryOption.NONE
    var activeColorTint: CharacterColorTint = CharacterColorTint.DEFAULT
    var activeEmote: EmoteAction = EmoteAction.READY_STANCE
    var attachments: WeaponLoadoutAttachments = WeaponLoadoutAttachments()
    var aiDifficulty: AiDifficulty = AiDifficulty.NORMAL
    var cameraSensitivity: Float = 1.0f
    var invertYAxis: Boolean = false

    // 3-Slot Weapon Loadout & Ammo
    var activeSlot: WeaponSlotType = WeaponSlotType.PRIMARY
    var primaryWeapon: WeaponSpec = InventoryCatalog.getWeapon("ar_dhaka_m4")
    var secondaryWeapon: WeaponSpec = InventoryCatalog.getWeapon("pistol_p92")
    var meleeWeapon: WeaponSpec = InventoryCatalog.getWeapon("melee_knife")
    private var primaryMagAmmo = 30
    private var primaryReserveAmmo = 120
    private var secondaryMagAmmo = 15
    private var secondaryReserveAmmo = 75
    var activeFireMode: FireMode = FireMode.AUTO

    // Consumables & Equipment
    var equippedArmor: EquipmentTier = EquipmentTier.VEST_MK1
    var bandagesCount = 2
    var firstAidCount = 1
    var energyCount = 1

    // Player transform & physics
    var playerPos = Vec3(0f, 0f, 8f)
    var playerVelY = 0f
    var playerYawRad = 0f
    var playerPitchRad = -0.06f
    var isGrounded = true
    var isCrouching = false
    var isSprinting = false
    var isSwimming = false
    var isVaulting = false
    private var vaultTimerSec = 0f
    private var vaultStartPos = Vec3()
    private var vaultEndPos = Vec3()
    var health = 100
    var stamina = 100f
    var walkAnimPhase = 0f
    var isMoving = false
    var damageFlashSec = 0f
    var hitMarkerSec = 0f
    var muzzleFlashSec = 0f
    var meleeSwingSec = 0f

    // Input state
    var moveInputX = 0f
    var moveInputY = 0f
    var vehicleThrottle = 0f
    var vehicleBrakeReverse = 0f
    var vehicleSteer = 0f
    private var engineAudioCooldownSec = 0f

    // Camera state
    var isFirstPerson = false
    var isAiming = false
    var currentFov = 62f
    var cameraBlend = 1f
    var cameraPos = Vec3(0f, 2f, 12f)
    var cameraLookAt = Vec3(0f, 1.4f, 0f)
    var spectatorAngle = 0f
    var showcaseYawRad = 0.35f

    // Combat & World Entities
    var isReloading = false
    var reloadTimerSec = 0f
    var fireCooldownSec = 0f
    var recoilKick = 0f
    var trainingHitsSession = 0
    var totalChallengeEnemiesDefeated = 0
    var elapsedTimeSec = 0f
    var outcome = ChallengeOutcome.IN_PROGRESS
    var districtMissionCompleted = false

    val rangeTargets = world.initialRangeTargets.map { it.copy() }.toMutableList()
    val vehicles = world.initialVehicles.map { it.copy() }.toMutableList()
    var activeVehicle: VehicleEntity? = null
    val lootItems = world.initialLoot.map { it.copy() }.toMutableList()
    val aiAgents = ArrayList<EnemyAiAgent>()
    val projectiles = ArrayList<Projectile>()
    val impacts = ArrayList<ImpactEffect>()
    val projectilePool = ObjectPool<Projectile>(
        maxCapacity = 96,
        factory = { Projectile(Vec3(), Vec3(), true, 0f, 0, Triple(1f, 1f, 1f)) }
    )
    val impactPool = ObjectPool<ImpactEffect>(
        maxCapacity = 64,
        factory = { ImpactEffect(Vec3(), 0f, Triple(1f, 1f, 1f)) }
    )
    var isMultiplayerSession: Boolean = false
    var remotePlayers: List<RemotePlayerSnapshot> = emptyList()

    private fun obtainProjectile(
        pos: Vec3,
        vel: Vec3,
        fromPlayer: Boolean,
        lifeSec: Float,
        damage: Int,
        rgb: Triple<Float, Float, Float>
    ): Projectile {
        val p = projectilePool.obtain()
        p.pos = pos
        p.vel = vel
        p.fromPlayer = fromPlayer
        p.lifeSec = lifeSec
        p.damage = damage
        p.rgb = rgb
        return p
    }

    private fun obtainImpact(pos: Vec3, lifeSec: Float, rgb: Triple<Float, Float, Float>): ImpactEffect {
        val imp = impactPool.obtain()
        imp.pos = pos
        imp.lifeSec = lifeSec
        imp.rgb = rgb
        return imp
    }

    fun loadStreamedWorld(newWorld: DhakaWorldGeometry, resetSpawn: Boolean = true) {
        synchronized(lock) {
            world = newWorld
            navGrid = DhakaNavGrid(newWorld.colliders)
            rangeTargets.clear()
            rangeTargets.addAll(newWorld.initialRangeTargets.map { it.copy() })
            vehicles.clear()
            vehicles.addAll(newWorld.initialVehicles.map { it.copy() })
            activeVehicle = null
            lootItems.clear()
            lootItems.addAll(newWorld.initialLoot.map { it.copy() })
            projectiles.forEach { projectilePool.recycle(it) }
            projectiles.clear()
            impacts.forEach { impactPool.recycle(it) }
            impacts.clear()
            districtMissionCompleted = false
            if (resetSpawn) {
                playerPos = Vec3(0f, 0f, 11f)
                playerVelY = 0f
                playerYawRad = 0f
            }
            updateCamera(0.016f)
        }
    }

    fun completeDistrictMissionObjective(): Boolean {
        synchronized(lock) {
            if (districtMissionCompleted) return false
            districtMissionCompleted = true
            return true
        }
    }

    fun activeWeapon(): WeaponSpec = when (activeSlot) {
        WeaponSlotType.PRIMARY -> primaryWeapon
        WeaponSlotType.SECONDARY -> secondaryWeapon
        WeaponSlotType.MELEE -> meleeWeapon
    }

    private fun currentMagSize(): Int {
        val w = activeWeapon()
        if (w.isMelee) return 0
        return if (w.supportsAttachments) attachments.effectiveMagSize(w.magSize) else w.magSize
    }

    private fun currentMagAmmo(): Int = when (activeSlot) {
        WeaponSlotType.PRIMARY -> primaryMagAmmo
        WeaponSlotType.SECONDARY -> secondaryMagAmmo
        WeaponSlotType.MELEE -> 0
    }

    private fun setCurrentMagAmmo(value: Int) {
        when (activeSlot) {
            WeaponSlotType.PRIMARY -> primaryMagAmmo = value
            WeaponSlotType.SECONDARY -> secondaryMagAmmo = value
            WeaponSlotType.MELEE -> Unit
        }
    }

    private fun currentReserveAmmo(): Int = when (activeSlot) {
        WeaponSlotType.PRIMARY -> primaryReserveAmmo
        WeaponSlotType.SECONDARY -> secondaryReserveAmmo
        WeaponSlotType.MELEE -> 0
    }

    private fun setCurrentReserveAmmo(value: Int) {
        when (activeSlot) {
            WeaponSlotType.PRIMARY -> primaryReserveAmmo = value.coerceAtLeast(0)
            WeaponSlotType.SECONDARY -> secondaryReserveAmmo = value.coerceAtLeast(0)
            WeaponSlotType.MELEE -> Unit
        }
    }

    fun startSession(
        mode: GameModeId,
        character: CharacterSlot,
        outfit: OutfitStyle,
        headwear: HeadwearOption,
        accessory: AccessoryOption,
        colorTint: CharacterColorTint,
        primary: WeaponSpec,
        secondary: WeaponSpec,
        melee: WeaponSpec,
        loadoutAttachments: WeaponLoadoutAttachments,
        difficulty: AiDifficulty,
        sensitivity: Float,
        invertY: Boolean,
        audioEnabled: Boolean,
        multiplayerSession: Boolean = mode.requiresMultiplayer
    ) {
        synchronized(lock) {
            activeMode = mode
            isMultiplayerSession = multiplayerSession
            remotePlayers = emptyList()
            activeCharacter = character
            activeOutfit = outfit
            activeHeadwear = headwear
            activeAccessory = accessory
            activeColorTint = colorTint
            attachments = loadoutAttachments
            aiDifficulty = difficulty
            cameraSensitivity = sensitivity
            invertYAxis = invertY
            soundEngine.enabled = audioEnabled

            primaryWeapon = primary
            secondaryWeapon = secondary
            meleeWeapon = melee
            activeSlot = WeaponSlotType.PRIMARY
            activeFireMode = primary.supportedFireModes.first()
            primaryMagAmmo = if (primary.supportsAttachments) attachments.effectiveMagSize(primary.magSize) else primary.magSize
            primaryReserveAmmo = primary.defaultReserveAmmo
            secondaryMagAmmo = secondary.magSize
            secondaryReserveAmmo = secondary.defaultReserveAmmo

            equippedArmor = EquipmentTier.VEST_MK1
            bandagesCount = 2
            firstAidCount = 1
            energyCount = 1

            playerPos = if (mode == GameModeId.TRAINING_GROUND) Vec3(5.5f, 0f, -5.5f) else Vec3(0f, 0f, 11f)
            playerVelY = 0f
            playerYawRad = if (mode == GameModeId.TRAINING_GROUND) 0.45f else 0f
            playerPitchRad = -0.06f
            isGrounded = true
            isCrouching = false
            isSprinting = false
            isSwimming = false
            isVaulting = false
            isAiming = false
            health = 100
            stamina = 100f
            moveInputX = 0f
            moveInputY = 0f
            vehicleThrottle = 0f
            vehicleBrakeReverse = 0f
            vehicleSteer = 0f
            activeVehicle = null

            vehicles.clear()
            vehicles.addAll(world.initialVehicles.map { it.copy() })
            lootItems.clear()
            lootItems.addAll(world.initialLoot.map { it.copy() })
            projectiles.forEach { projectilePool.recycle(it) }
            projectiles.clear()
            impacts.forEach { impactPool.recycle(it) }
            impacts.clear()
            rangeTargets.forEachIndexed { idx, rt ->
                rt.currentPos = rt.basePos
                rt.phase = idx * 1.1f
                rt.hitCooldownSec = 0f
            }
            districtMissionCompleted = false

            isReloading = false
            reloadTimerSec = 0f
            fireCooldownSec = 0f
            recoilKick = 0f
            damageFlashSec = 0f
            hitMarkerSec = 0f
            muzzleFlashSec = 0f
            meleeSwingSec = 0f
            trainingHitsSession = 0
            totalChallengeEnemiesDefeated = 0
            elapsedTimeSec = 0f
            outcome = ChallengeOutcome.IN_PROGRESS

            aiAgents.clear()
            if (mode == GameModeId.SOLO && !multiplayerSession) {
                spawnChallengeSquad(difficulty)
            } else if (mode == GameModeId.DISTRICT_CHALLENGE && multiplayerSession) {
                spawnChallengeSquad(difficulty)
            }
            updateCamera(0.016f)
        }
    }

    fun updateRemotePlayers(
        players: List<RemotePlayerSnapshot>,
        localPlayerId: String,
        authoritativeLocalHealth: Int? = null
    ) {
        synchronized(lock) {
            remotePlayers = players.filter { it.playerId != localPlayerId }
            if (isMultiplayerSession && authoritativeLocalHealth != null) {
                health = authoritativeLocalHealth.coerceIn(0, 100)
                if (health <= 0 && outcome == ChallengeOutcome.IN_PROGRESS) {
                    outcome = ChallengeOutcome.SPECTATOR_DEFEAT
                }
            }
        }
    }

    private fun spawnChallengeSquad(difficulty: AiDifficulty) {
        val baseHp = (100f * difficulty.healthMultiplier).toInt().coerceAtLeast(60)
        val routes = listOf(
            listOf(Vec3(-6f, 0f, -2f), Vec3(-10f, 0f, -8f)),
            listOf(Vec3(6f, 0f, -2f), Vec3(10f, 0f, -7f)),
            listOf(Vec3(-9f, 0f, -11f), Vec3(-4f, 0f, -15f)),
            listOf(Vec3(8f, 0f, -12f), Vec3(2f, 0f, -16f)),
            listOf(Vec3(-12f, 0f, 8f), Vec3(-8f, 0f, 13f)),
            listOf(Vec3(4f, 0f, 6f), Vec3(-3f, 0f, 4f))
        )
        routes.forEachIndexed { i, pts ->
            aiAgents.add(
                EnemyAiAgent(
                    id = i + 1,
                    callsign = "Raider-${i + 1}",
                    patrolPoints = pts,
                    pos = pts.first(),
                    hp = baseHp,
                    maxHp = baseHp,
                    aiState = EnemyAiState.PATROL,
                    fireCooldownSec = 1.2f + i * 0.3f
                )
            )
        }
    }

    fun spawnPracticeBot() {
        synchronized(lock) {
            if (activeMode != GameModeId.TRAINING_GROUND) return
            val existingAlive = aiAgents.count { it.isAlive }
            if (existingAlive >= 2) return
            val baseHp = (90f * aiDifficulty.healthMultiplier).toInt()
            val spawn = Vec3(16.5f + existingAlive * 2.5f, 0f, -12.5f)
            aiAgents.add(
                EnemyAiAgent(
                    id = aiAgents.size + 1,
                    callsign = "SparBot-${aiAgents.size + 1}",
                    patrolPoints = listOf(spawn, Vec3(12f, 0f, -12.5f)),
                    pos = spawn,
                    hp = baseHp,
                    maxHp = baseHp,
                    aiState = EnemyAiState.PATROL
                )
            )
        }
    }

    fun refillAmmoAndHealth() {
        synchronized(lock) {
            health = 100
            stamina = 100f
            primaryReserveAmmo = primaryWeapon.defaultReserveAmmo
            secondaryReserveAmmo = secondaryWeapon.defaultReserveAmmo
            primaryMagAmmo = if (primaryWeapon.supportsAttachments) attachments.effectiveMagSize(primaryWeapon.magSize) else primaryWeapon.magSize
            secondaryMagAmmo = secondaryWeapon.magSize
            isReloading = false
        }
    }

    fun switchWeaponSlot(slot: WeaponSlotType) {
        synchronized(lock) {
            if (activeVehicle != null) return
            activeSlot = slot
            isReloading = false
            isAiming = false
            val w = activeWeapon()
            activeFireMode = w.supportedFireModes.first()
        }
    }

    fun cycleFireMode() {
        synchronized(lock) {
            val modes = activeWeapon().supportedFireModes
            if (modes.size <= 1) return
            val idx = modes.indexOf(activeFireMode)
            activeFireMode = modes[(idx + 1) % modes.size]
        }
    }

    fun toggleAim() {
        synchronized(lock) {
            if (activeVehicle != null || activeWeapon().isMelee) {
                isAiming = false
                return
            }
            isAiming = !isAiming
            if (isAiming) isSprinting = false
        }
    }

    fun switchTestingWeapon(weapon: WeaponSpec) {
        synchronized(lock) {
            when (weapon.category) {
                com.example.game.inventory.WeaponCategory.PISTOL -> {
                    secondaryWeapon = weapon
                    activeSlot = WeaponSlotType.SECONDARY
                    secondaryMagAmmo = weapon.magSize
                    secondaryReserveAmmo = weapon.defaultReserveAmmo
                }
                com.example.game.inventory.WeaponCategory.MELEE -> {
                    meleeWeapon = weapon
                    activeSlot = WeaponSlotType.MELEE
                }
                else -> {
                    primaryWeapon = weapon
                    activeSlot = WeaponSlotType.PRIMARY
                    primaryMagAmmo = if (weapon.supportsAttachments) attachments.effectiveMagSize(weapon.magSize) else weapon.magSize
                    primaryReserveAmmo = weapon.defaultReserveAmmo
                }
            }
            activeFireMode = weapon.supportedFireModes.first()
            isReloading = false
        }
    }

    fun setMoveInput(x: Float, y: Float) {
        synchronized(lock) {
            moveInputX = x.coerceIn(-1f, 1f)
            moveInputY = y.coerceIn(-1f, 1f)
        }
    }

    fun setVehicleInputs(throttle: Float, brakeReverse: Float, steer: Float) {
        synchronized(lock) {
            vehicleThrottle = throttle.coerceIn(0f, 1f)
            vehicleBrakeReverse = brakeReverse.coerceIn(0f, 1f)
            vehicleSteer = steer.coerceIn(-1f, 1f)
        }
    }

    fun applyLookDelta(dxPixels: Float, dyPixels: Float) {
        synchronized(lock) {
            if (outcome == ChallengeOutcome.SPECTATOR_DEFEAT) {
                spectatorAngle += dxPixels * 0.005f
                return
            }
            val sensScale = if (isAiming) 0.55f else 1.0f
            val sens = 0.0038f * cameraSensitivity * sensScale
            playerYawRad += dxPixels * sens
            val ySign = if (invertYAxis) 1f else -1f
            playerPitchRad = (playerPitchRad + dyPixels * sens * ySign).coerceIn(-1.05f, 0.95f)
        }
    }

    fun rotateShowcase(dxPixels: Float) {
        synchronized(lock) { showcaseYawRad += dxPixels * 0.008f }
    }

    fun toggleCameraMode() {
        synchronized(lock) { isFirstPerson = !isFirstPerson }
    }

    fun toggleCrouch() {
        synchronized(lock) {
            if (isSwimming || activeVehicle != null) return
            isCrouching = !isCrouching
            if (isCrouching) isSprinting = false
        }
    }

    fun setSprint(enabled: Boolean) {
        synchronized(lock) {
            isSprinting = enabled && !isCrouching && !isSwimming && !isAiming && stamina > 5f
        }
    }

    fun jumpOrVault() {
        synchronized(lock) {
            if (outcome != ChallengeOutcome.IN_PROGRESS || activeVehicle != null) return
            if (isSwimming) {
                playerVelY = 3.2f
                return
            }
            // Check vaultable low cover ahead
            val forward = Vec3(sin(playerYawRad), 0f, -cos(playerYawRad)).normalized()
            val probePos = playerPos + forward * 1.1f
            val vaultBox = world.vaultables.firstOrNull { box ->
                probePos.x in (box.minX - 0.5f)..(box.maxX + 0.5f) &&
                    probePos.z in (box.minZ - 0.5f)..(box.maxZ + 0.5f) &&
                    box.maxY <= 1.35f
            }
            if (vaultBox != null && !isVaulting && isGrounded) {
                isCrouching = false
                isVaulting = true
                vaultTimerSec = 0f
                vaultStartPos = playerPos
                val hopOver = playerPos + forward * 2.1f
                val limit = DhakaWorldData.ARENA_HALF_SIZE - 0.8f
                vaultEndPos = Vec3(hopOver.x.coerceIn(-limit, limit), 0f, hopOver.z.coerceIn(-limit, limit))
                return
            }
            if (isGrounded) {
                isCrouching = false
                playerVelY = 6.4f
                isGrounded = false
            }
        }
    }

    fun enterOrExitVehicle(): Boolean {
        synchronized(lock) {
            val currentVeh = activeVehicle
            if (currentVeh != null) {
                // Exit to the side of the vehicle
                val sideOffset = GlTrig.rightFromYaw(currentVeh.yawRad) * (currentVeh.type.width * 0.8f + 0.7f)
                val exitPos = currentVeh.pos + sideOffset
                playerPos = Vec3(exitPos.x, 0f, exitPos.z)
                activeVehicle = null
                vehicleThrottle = 0f
                vehicleBrakeReverse = 0f
                vehicleSteer = 0f
                return true
            }
            val nearest = findNearbyVehicle() ?: return false
            if (!nearest.isOperational) return false
            activeVehicle = nearest
            isCrouching = false
            isSprinting = false
            isAiming = false
            playerYawRad = nearest.yawRad
            return true
        }
    }

    fun useConsumable(type: ConsumableType): Boolean {
        synchronized(lock) {
            if (outcome != ChallengeOutcome.IN_PROGRESS) return false
            when (type) {
                ConsumableType.BANDAGE -> {
                    if (bandagesCount <= 0 || health >= 100) return false
                    bandagesCount -= 1
                    health = (health + type.healAmount).coerceAtMost(100)
                }
                ConsumableType.FIRST_AID_KIT -> {
                    if (firstAidCount <= 0 || health >= 100) return false
                    firstAidCount -= 1
                    health = (health + type.healAmount).coerceAtMost(100)
                }
                ConsumableType.ENERGY_DRINK -> {
                    if (energyCount <= 0) return false
                    energyCount -= 1
                    health = (health + type.healAmount).coerceAtMost(100)
                    stamina = (stamina + type.staminaAmount).coerceAtMost(100f)
                }
            }
            return true
        }
    }

    fun pickupNearbyLoot(): Boolean {
        synchronized(lock) {
            val loot = findNearbyLoot() ?: return false
            loot.collected = true
            when (loot.kind) {
                GroundLootKind.WEAPON_CRATE -> {
                    loot.weaponId?.let { switchTestingWeapon(InventoryCatalog.getWeapon(it)) }
                }
                GroundLootKind.AMMO_BOX -> {
                    primaryReserveAmmo += loot.ammoAmount
                    secondaryReserveAmmo += loot.ammoAmount / 2
                }
                GroundLootKind.MEDKIT_DROP -> {
                    when (loot.consumable) {
                        ConsumableType.FIRST_AID_KIT -> firstAidCount += 1
                        ConsumableType.ENERGY_DRINK -> energyCount += 1
                        else -> bandagesCount += 1
                    }
                }
                GroundLootKind.ARMOR_DROP -> {
                    equippedArmor = EquipmentTier.VEST_MK2
                }
            }
            return true
        }
    }

    fun dropActiveItem(): Boolean {
        synchronized(lock) {
            if (activeSlot == WeaponSlotType.MELEE) return false
            val w = activeWeapon()
            val forward = Vec3(sin(playerYawRad), 0f, -cos(playerYawRad)).normalized()
            val dropPos = playerPos + forward * 1.4f
            lootItems.add(
                GroundLootItem(
                    id = "drop_${System.nanoTime()}",
                    kind = GroundLootKind.WEAPON_CRATE,
                    x = dropPos.x,
                    y = 0.35f,
                    z = dropPos.z,
                    weaponId = w.id
                )
            )
            // Fallback to default pistol or melee after dropping
            if (activeSlot == WeaponSlotType.PRIMARY) {
                activeSlot = WeaponSlotType.SECONDARY
            } else {
                activeSlot = WeaponSlotType.MELEE
            }
            activeFireMode = activeWeapon().supportedFireModes.first()
            return true
        }
    }

    fun reloadWeapon() {
        synchronized(lock) {
            val w = activeWeapon()
            if (w.isMelee || isReloading) return
            val maxMag = currentMagSize()
            if (currentMagAmmo() >= maxMag || currentReserveAmmo() <= 0) return
            isReloading = true
            reloadTimerSec = w.reloadDurationMs / 1000f
        }
    }

    fun fireWeapon(): Int {
        synchronized(lock) {
            if (outcome != ChallengeOutcome.IN_PROGRESS || activeVehicle != null) return 0
            val w = activeWeapon()
            if (isReloading || fireCooldownSec > 0f) return 0

            if (w.isMelee) {
                fireCooldownSec = w.fireCooldownMs / 1000f
                meleeSwingSec = 0.28f
                soundEngine.playWeaponShot(w.category, suppressed = false)
                return performMeleeAttack(w)
            }

            if (currentMagAmmo() <= 0) {
                reloadWeapon()
                return 0
            }

            val roundsToFire = if (activeFireMode == FireMode.BURST) min(3, currentMagAmmo()) else 1
            setCurrentMagAmmo(currentMagAmmo() - roundsToFire)
            fireCooldownSec = (w.fireCooldownMs / 1000f) * (if (activeFireMode == FireMode.BURST) 1.5f else 1f)
            muzzleFlashSec = 0.06f

            val effectiveRecoil = if (w.supportsAttachments) attachments.effectiveRecoil(w.baseRecoil) else w.baseRecoil
            recoilKick = effectiveRecoil
            playerPitchRad = (playerPitchRad + effectiveRecoil * 0.14f).coerceAtMost(0.95f)

            val suppressed = w.supportsAttachments && attachments.muzzle.isSuppressed
            soundEngine.playWeaponShot(w.category, suppressed)

            val spreadDeg = if (w.supportsAttachments) {
                attachments.effectiveSpread(w.baseSpreadDeg, isAiming)
            } else {
                w.baseSpreadDeg * (if (isAiming) 0.7f else 1.0f)
            }

            val baseDir = GlTrig.forwardFromYawPitch(playerYawRad, playerPitchRad)
            val right = GlTrig.rightFromYaw(playerYawRad)
            val up = Vec3(0f, 1f, 0f)
            val muzzleOrigin = if (isFirstPerson) {
                cameraPos + baseDir * 0.5f
            } else {
                val eyeHeight = if (isCrouching) 1.05f else 1.52f
                Vec3(playerPos.x, playerPos.y + eyeHeight, playerPos.z) + baseDir * 0.7f
            }

            var newTrainingHits = 0
            var landedAnyHit = false
            val totalRays = roundsToFire * w.pelletsPerShot

            for (i in 0 until totalRays) {
                val spreadRad = Math.toRadians(spreadDeg.toDouble()).toFloat()
                val jx = (rng.nextFloat() * 2f - 1f) * spreadRad
                val jy = (rng.nextFloat() * 2f - 1f) * spreadRad
                val rayDir = (baseDir + right * jx + up * jy).normalized()

                if (i < 4) {
                    projectiles.add(
                        obtainProjectile(
                            pos = muzzleOrigin,
                            vel = rayDir * w.projectileSpeed,
                            fromPlayer = true,
                            lifeSec = 1.4f,
                            damage = w.damage,
                            rgb = w.tracerRgb
                        )
                    )
                }

                var wallDist = w.rangeMeters
                for (c in world.colliders) {
                    val hitT = c.intersectRay(cameraPos, rayDir, wallDist)
                    if (hitT != null && hitT < wallDist) {
                        wallDist = hitT
                    }
                }

                // Check range targets
                var hitEntity = false
                for (rt in rangeTargets) {
                    if (rt.hitCooldownSec > 0f) continue
                    val box = Aabb(
                        rt.currentPos.x - 0.55f, rt.currentPos.y - 0.7f, rt.currentPos.z - 0.55f,
                        rt.currentPos.x + 0.55f, rt.currentPos.y + 0.7f, rt.currentPos.z + 0.55f
                    )
                    val hitT = box.intersectRay(cameraPos, rayDir, wallDist)
                    if (hitT != null) {
                        rt.hitCooldownSec = 0.65f
                        trainingHitsSession += 1
                        newTrainingHits += 1
                        hitEntity = true
                        landedAnyHit = true
                        impacts.add(obtainImpact(cameraPos + rayDir * hitT, 0.25f, w.tracerRgb))
                        break
                    }
                }

                // Check Enemy AI Agents
                if (!hitEntity) {
                    var bestAgent: EnemyAiAgent? = null
                    var bestDist = wallDist
                    for (agent in aiAgents) {
                        if (!agent.isAlive) continue
                        val box = Aabb(
                            agent.pos.x - 0.50f, agent.pos.y, agent.pos.z - 0.50f,
                            agent.pos.x + 0.50f, agent.pos.y + 1.85f, agent.pos.z + 0.50f
                        )
                        val hitT = box.intersectRay(cameraPos, rayDir, bestDist)
                        if (hitT != null && hitT < bestDist) {
                            bestDist = hitT
                            bestAgent = agent
                        }
                    }
                    if (bestAgent != null) {
                        landedAnyHit = true
                        val impactPos = cameraPos + rayDir * bestDist
                        val headshot = impactPos.y >= bestAgent.pos.y + 1.42f
                        val dmg = if (headshot) (w.damage * 1.45f).toInt() else w.damage
                        applyDamageToAgent(bestAgent, dmg)
                        impacts.add(obtainImpact(impactPos, 0.25f, Triple(0.98f, 0.22f, 0.22f)))
                    } else if (wallDist < w.rangeMeters) {
                        impacts.add(obtainImpact(cameraPos + rayDir * wallDist, 0.22f, Triple(0.95f, 0.85f, 0.45f)))
                    }
                }
            }

            if (landedAnyHit) {
                hitMarkerSec = 0.16f
                soundEngine.playHitMarker()
            }

            if (currentMagAmmo() == 0 && currentReserveAmmo() > 0) {
                reloadWeapon()
            }
            return newTrainingHits
        }
    }

    private fun performMeleeAttack(w: WeaponSpec): Int {
        val forward = Vec3(sin(playerYawRad), 0f, -cos(playerYawRad)).normalized()
        var hits = 0
        for (agent in aiAgents) {
            if (!agent.isAlive) continue
            val delta = agent.pos - playerPos
            val dist = delta.length()
            if (dist <= w.rangeMeters) {
                val dot = (delta.normalized().x * forward.x + delta.normalized().z * forward.z)
                if (dot > 0.25f || dist < 1.3f) {
                    applyDamageToAgent(agent, w.damage)
                    hitMarkerSec = 0.18f
                    soundEngine.playHitMarker()
                    impacts.add(ImpactEffect(agent.pos + Vec3(0f, 1.1f, 0f), 0.25f, Triple(0.98f, 0.25f, 0.25f)))
                }
            }
        }
        for (rt in rangeTargets) {
            if (rt.hitCooldownSec <= 0f && rt.currentPos.distanceTo(playerPos) <= w.rangeMeters + 0.5f) {
                rt.hitCooldownSec = 0.65f
                trainingHitsSession += 1
                hits += 1
            }
        }
        return hits
    }

    private fun applyDamageToAgent(agent: EnemyAiAgent, dmg: Int) {
        val wasAlive = agent.isAlive
        agent.hp = (agent.hp - dmg).coerceAtLeast(0)
        agent.lastKnownPlayerPos = playerPos
        if (agent.hp <= 0) {
            agent.aiState = EnemyAiState.DEAD
            if (wasAlive) {
                totalChallengeEnemiesDefeated += 1
                if (activeMode == GameModeId.SOLO && aiAgents.all { !it.isAlive }) {
                    outcome = ChallengeOutcome.VICTORY
                }
            }
        } else if (agent.hp <= agent.maxHp * 0.28f) {
            agent.aiState = EnemyAiState.RETREAT
        } else if (agent.aiState == EnemyAiState.IDLE || agent.aiState == EnemyAiState.PATROL) {
            agent.aiState = EnemyAiState.CHASE
        }
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        synchronized(lock) {
            elapsedTimeSec += dt
            fireCooldownSec = max(0f, fireCooldownSec - dt)
            recoilKick = max(0f, recoilKick - dt * 1.4f)
            damageFlashSec = max(0f, damageFlashSec - dt)
            hitMarkerSec = max(0f, hitMarkerSec - dt)
            muzzleFlashSec = max(0f, muzzleFlashSec - dt)
            meleeSwingSec = max(0f, meleeSwingSec - dt)
            engineAudioCooldownSec = max(0f, engineAudioCooldownSec - dt)

            // Smooth FOV zoom on ADS
            val targetFov = if (isAiming && activeVehicle == null) attachments.optic.adsFov else 62f
            currentFov += (targetFov - currentFov) * min(1f, dt * 12f)

            if (isReloading) {
                reloadTimerSec -= dt
                if (reloadTimerSec <= 0f) {
                    isReloading = false
                    val needed = currentMagSize() - currentMagAmmo()
                    val loaded = min(needed, currentReserveAmmo())
                    setCurrentMagAmmo(currentMagAmmo() + loaded)
                    setCurrentReserveAmmo(currentReserveAmmo() - loaded)
                }
            }

            // Update range targets
            for (rt in rangeTargets) {
                rt.hitCooldownSec = max(0f, rt.hitCooldownSec - dt)
                if (rt.isMoving) {
                    rt.phase += dt * 1.7f
                    rt.currentPos = Vec3(rt.basePos.x + sin(rt.phase) * 3.8f, rt.basePos.y, rt.basePos.z)
                }
            }

            // Update impact particles (with pool recycling)
            val impIt = impacts.iterator()
            while (impIt.hasNext()) {
                val imp = impIt.next()
                imp.lifeSec -= dt
                if (imp.lifeSec <= 0f) {
                    impIt.remove()
                    impactPool.recycle(imp)
                }
            }

            if (outcome == ChallengeOutcome.IN_PROGRESS) {
                val veh = activeVehicle
                if (veh != null) {
                    VehiclePhysicsStepper.stepVehicle(veh, vehicleThrottle, vehicleBrakeReverse, vehicleSteer, dt, world)
                    playerPos = Vec3(veh.pos.x, veh.pos.y + 0.35f, veh.pos.z)
                    isMoving = abs(veh.speed) > 0.3f
                    if (isMoving && engineAudioCooldownSec <= 0f) {
                        engineAudioCooldownSec = 0.24f
                        soundEngine.playVehiclePulse(abs(veh.speed) / veh.type.maxSpeed)
                    }
                } else {
                    stepPlayerPhysics(dt)
                }

                val noiseRadius = when {
                    muzzleFlashSec > 0f -> 26f * attachments.muzzle.noiseRadiusMultiplier
                    activeVehicle != null && isMoving -> 18f
                    isSprinting -> 12f
                    else -> 4f
                }

                EnemyAiStepper.stepAgents(
                    agents = aiAgents,
                    playerPos = playerPos,
                    playerNoiseRadius = noiseRadius,
                    difficulty = aiDifficulty,
                    navGrid = navGrid,
                    world = world,
                    dt = dt
                ) { origin, vel, dmg ->
                    projectiles.add(
                        obtainProjectile(
                            pos = origin,
                            vel = vel,
                            fromPlayer = false,
                            lifeSec = 2.4f,
                            damage = dmg,
                            rgb = Triple(0.95f, 0.18f, 0.24f)
                        )
                    )
                }

                stepProjectiles(dt)
            } else if (outcome == ChallengeOutcome.SPECTATOR_DEFEAT) {
                spectatorAngle += dt * 0.42f
            }

            updateCamera(dt)
        }
    }

    private fun stepPlayerPhysics(dt: Float) {
        if (isVaulting) {
            vaultTimerSec += dt
            val t = (vaultTimerSec / 0.38f).coerceIn(0f, 1f)
            val arcY = sin(t * PI.toFloat()) * 1.35f
            playerPos = Vec3(
                x = vaultStartPos.x + (vaultEndPos.x - vaultStartPos.x) * t,
                y = arcY,
                z = vaultStartPos.z + (vaultEndPos.z - vaultStartPos.z) * t
            )
            if (t >= 1f) {
                isVaulting = false
                playerPos = vaultEndPos
                isGrounded = true
            }
            return
        }

        val wz = world.waterZone
        val inWater = playerPos.x in wz.minX..wz.maxX && playerPos.z in wz.minZ..wz.maxZ
        isSwimming = inWater
        if (isSwimming) {
            isCrouching = false
            isSprinting = false
        }

        val forward = Vec3(sin(playerYawRad), 0f, -cos(playerYawRad)).normalized()
        val right = GlTrig.rightFromYaw(playerYawRad)
        val moveVec = right * moveInputX + forward * moveInputY
        val mag = sqrt(moveVec.x * moveVec.x + moveVec.z * moveVec.z)
        isMoving = mag > 0.08f

        val wantsSprint = isSprinting && isMoving && !isCrouching && !isSwimming && !isAiming && stamina > 1f
        val baseSpeed = when {
            isSwimming -> 3.4f
            isCrouching -> 2.7f
            wantsSprint -> 7.6f
            isAiming -> 2.8f
            mag > 0.65f -> 5.2f
            else -> 3.0f
        } * activeCharacter.speedMultiplier

        if (wantsSprint) {
            stamina = max(0f, stamina - dt * 24f)
            if (stamina <= 0.5f) isSprinting = false
        } else {
            stamina = min(100f, stamina + dt * 16f)
        }

        val dir = if (mag > 1e-4f) Vec3(moveVec.x / mag, 0f, moveVec.z / mag) else Vec3(0f, 0f, 0f)
        val speedScale = min(1f, mag)
        var nextX = playerPos.x + dir.x * baseSpeed * speedScale * dt
        var nextZ = playerPos.z + dir.z * baseSpeed * speedScale * dt

        if (isMoving) {
            walkAnimPhase += dt * baseSpeed * 2.1f * activeCharacter.animationProfile.speedFactor
        }

        val floorY = if (isSwimming) -0.35f else 0f
        playerVelY -= (if (isSwimming) 6.5f else 18.5f) * dt
        var nextY = playerPos.y + playerVelY * dt
        if (nextY <= floorY) {
            nextY = floorY
            playerVelY = 0f
            isGrounded = true
        }

        val playerRadius = 0.42f
        val playerHeight = if (isCrouching) 1.15f else 1.8f
        for (c in world.colliders) {
            val resolved = c.resolveCircleXz(nextX, nextY, nextZ, playerRadius, playerHeight)
            if (resolved != null) {
                nextX = resolved.first
                nextZ = resolved.second
            }
        }

        val limit = DhakaWorldData.ARENA_HALF_SIZE - 0.6f
        playerPos = Vec3(nextX.coerceIn(-limit, limit), nextY, nextZ.coerceIn(-limit, limit))
    }

    private fun stepProjectiles(dt: Float) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.lifeSec -= dt
            if (p.lifeSec <= 0f) {
                iterator.remove()
                projectilePool.recycle(p)
                continue
            }
            val nextPos = p.pos + p.vel * dt
            val hitWall = world.colliders.any { c ->
                nextPos.x in c.minX..c.maxX && nextPos.y in c.minY..c.maxY && nextPos.z in c.minZ..c.maxZ
            }
            if (hitWall || nextPos.y < 0f) {
                iterator.remove()
                projectilePool.recycle(p)
                continue
            }

            if (!p.fromPlayer) {
                val veh = activeVehicle
                if (veh != null && nextPos.distanceTo(veh.pos + Vec3(0f, 0.8f, 0f)) <= max(veh.type.width, veh.type.length) * 0.55f) {
                    veh.health = (veh.health - p.damage).coerceAtLeast(0)
                    damageFlashSec = 0.18f
                    iterator.remove()
                    projectilePool.recycle(p)
                    if (veh.health <= 0) {
                        enterOrExitVehicle()
                    }
                    continue
                }
                val playerHeight = if (isCrouching) 1.15f else 1.8f
                val dx = nextPos.x - playerPos.x
                val dz = nextPos.z - playerPos.z
                if (dx * dx + dz * dz <= 0.52f * 0.52f && nextPos.y in playerPos.y..(playerPos.y + playerHeight)) {
                    val mitigated = (p.damage * (1f - equippedArmor.damageReduction)).toInt().coerceAtLeast(2)
                    health = (health - mitigated).coerceAtLeast(0)
                    damageFlashSec = 0.28f
                    iterator.remove()
                    projectilePool.recycle(p)
                    if (health <= 0) {
                        outcome = ChallengeOutcome.SPECTATOR_DEFEAT
                    }
                    continue
                }
            }
            p.pos = nextPos
        }
    }

    private fun updateCamera(dt: Float) {
        if (outcome == ChallengeOutcome.SPECTATOR_DEFEAT) {
            val radius = 16f
            cameraPos = Vec3(sin(spectatorAngle) * radius, 9.0f, cos(spectatorAngle) * radius)
            cameraLookAt = Vec3(0f, 1.5f, -2f)
            return
        }

        val veh = activeVehicle
        if (veh != null) {
            val backDist = veh.type.length * 1.45f + 2.8f
            val vehForward = Vec3(sin(veh.yawRad), 0f, -cos(veh.yawRad))
            val desiredPos = veh.pos - vehForward * backDist + Vec3(0f, veh.type.height + 2.1f, 0f)
            val pivot = veh.pos + Vec3(0f, veh.type.height * 0.8f, 0f)
            val dir = (desiredPos - pivot).normalized()
            val maxD = pivot.distanceTo(desiredPos)
            var clampedD = maxD
            for (c in world.colliders) {
                val hitT = c.intersectRay(pivot, dir, maxD)
                if (hitT != null && hitT < clampedD) clampedD = max(1.2f, hitT - 0.3f)
            }
            cameraPos = pivot + dir * clampedD
            cameraLookAt = veh.pos + vehForward * 5.0f + Vec3(0f, 1.0f, 0f)
            return
        }

        val targetBlend = if (isFirstPerson) 0f else 1f
        cameraBlend += (targetBlend - cameraBlend) * min(1f, dt * 11f)

        val eyeHeight = when {
            isSwimming -> 1.05f
            isCrouching -> 1.05f
            else -> 1.58f
        }
        val shoulderPivot = Vec3(playerPos.x, playerPos.y + eyeHeight, playerPos.z)
        val forward = GlTrig.forwardFromYawPitch(playerYawRad, playerPitchRad + recoilKick * 0.25f)
        val right = GlTrig.rightFromYaw(playerYawRad)

        val zoomPullIn = if (isAiming) 0.55f else 1.0f
        val desiredOffset = (forward * (-4.2f * zoomPullIn)) + (right * 0.65f) + Vec3(0f, 0.52f, 0f)
        val offsetDist = desiredOffset.length()
        val offsetDir = desiredOffset.normalized()

        var clampedDist = offsetDist
        for (c in world.colliders) {
            val hitT = c.intersectRay(shoulderPivot, offsetDir, offsetDist)
            if (hitT != null && hitT < clampedDist) clampedDist = max(0.35f, hitT - 0.28f)
        }
        val safeThird = shoulderPivot + offsetDir * clampedDist
        val finalThird = Vec3(safeThird.x, max(0.35f, safeThird.y), safeThird.z)

        cameraPos = Vec3(
            x = shoulderPivot.x + (finalThird.x - shoulderPivot.x) * cameraBlend,
            y = shoulderPivot.y + (finalThird.y - shoulderPivot.y) * cameraBlend,
            z = shoulderPivot.z + (finalThird.z - shoulderPivot.z) * cameraBlend
        )
        cameraLookAt = cameraPos + forward * 12f
    }

    fun findNearbyVehicle(): VehicleEntity? {
        if (activeVehicle != null) return activeVehicle
        return vehicles
            .filter { it.isOperational && it.pos.distanceTo(playerPos) < (it.type.length * 0.65f + 2.4f) }
            .minByOrNull { it.pos.distanceTo(playerPos) }
    }

    fun findNearbyLoot(): GroundLootItem? =
        lootItems
            .filter { !it.collected && Vec3(it.x, it.y, it.z).distanceTo(playerPos) < 2.6f }
            .minByOrNull { Vec3(it.x, it.y, it.z).distanceTo(playerPos) }

    fun detectNearbyInteraction(): NearbyInteraction {
        if (activeVehicle != null) return NearbyInteraction.NONE
        if (findNearbyLoot() != null) return NearbyInteraction.LOOT_NEARBY
        if (findNearbyVehicle() != null) return NearbyInteraction.VEHICLE_NEARBY
        if (playerPos.distanceTo(world.npcCommanderPos) < 3.2f) return NearbyInteraction.NPC_COMMANDER
        if (!districtMissionCompleted && playerPos.distanceTo(world.missionObjectivePos) < 3.4f) {
            return NearbyInteraction.MISSION_OBJECTIVE
        }
        if (playerPos.distanceTo(world.travelPortalPos) < 3.8f) return NearbyInteraction.TRAVEL_PORTAL
        if (activeMode == GameModeId.TRAINING_GROUND) {
            if (playerPos.distanceTo(world.weaponBenchPos) < 3.0f) return NearbyInteraction.WEAPON_BENCH
            if (playerPos.distanceTo(world.ammoStationPos) < 3.0f) return NearbyInteraction.AMMO_STATION
            if (playerPos.distanceTo(world.aiPracticePos) < 3.2f) return NearbyInteraction.AI_PRACTICE_PAD
            if (playerPos.distanceTo(world.missionBeaconPos) < 3.2f) return NearbyInteraction.MISSION_BEACON
        }
        return NearbyInteraction.NONE
    }

    fun snapshot(): HudSnapshot = synchronized(lock) {
        val stance = when {
            activeVehicle != null -> MovementStance.DRIVING
            isVaulting -> MovementStance.VAULTING
            isSwimming -> MovementStance.SWIMMING
            isCrouching -> MovementStance.CROUCH
            !isMoving -> MovementStance.IDLE
            isSprinting -> MovementStance.SPRINT
            kotlin.math.hypot(moveInputX, moveInputY) > 0.65f -> MovementStance.RUN
            else -> MovementStance.WALK
        }
        val w = activeWeapon()
        HudSnapshot(
            mode = activeMode,
            activeDistrictId = world.districtId,
            districtMissionCompleted = districtMissionCompleted,
            health = health,
            maxHealth = 100,
            stamina = stamina.toInt(),
            armorTier = equippedArmor,
            activeSlot = activeSlot,
            activeWeapon = w,
            primaryWeapon = primaryWeapon,
            secondaryWeapon = secondaryWeapon,
            meleeWeapon = meleeWeapon,
            activeFireMode = activeFireMode,
            ammoInMag = currentMagAmmo(),
            magSize = currentMagSize(),
            reserveAmmo = currentReserveAmmo(),
            isReloading = isReloading,
            isAiming = isAiming,
            isFirstPerson = isFirstPerson,
            isCrouching = isCrouching,
            isSprinting = isSprinting,
            isSwimming = isSwimming,
            isVaulting = isVaulting,
            stance = stance,
            nearbyInteraction = detectNearbyInteraction(),
            nearbyVehicle = findNearbyVehicle(),
            activeVehicle = activeVehicle,
            nearbyLoot = findNearbyLoot(),
            bandagesCount = bandagesCount,
            firstAidCount = firstAidCount,
            energyCount = energyCount,
            damageFlashAlpha = (damageFlashSec / 0.28f).coerceIn(0f, 0.55f),
            hitMarkerAlpha = (hitMarkerSec / 0.16f).coerceIn(0f, 1f),
            trainingHitsSession = trainingHitsSession,
            enemiesEliminated = aiAgents.count { !it.isAlive },
            totalEnemies = if (activeMode == GameModeId.SOLO) aiAgents.size else 0,
            activeAiCount = aiAgents.count { it.isAlive },
            elapsedSeconds = elapsedTimeSec.toInt(),
            outcome = outcome,
            isMultiplayerSession = isMultiplayerSession,
            remotePlayersCount = remotePlayers.size,
            poolRecycledCount = projectilePool.totalRecycledHits + impactPool.totalRecycledHits
        )
    }
}
