package com.example.game.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.ai.EnemyAiAgent
import com.example.game.ai.EnemyAiState
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.CharacterSlot
import com.example.game.characters.EmoteAction
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.inventory.GroundLootItem
import com.example.game.inventory.GroundLootKind
import com.example.game.inventory.WeaponSpec
import com.example.game.multiplayer.RemotePlayerSnapshot
import com.example.game.multiplayer.TeamSide
import com.example.game.save.GraphicsQuality
import com.example.game.vehicles.VehicleEntity
import com.example.game.vehicles.VehicleType
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

enum class GlRenderMode {
    LOBBY_SHOWCASE,
    PLAYABLE_WORLD
}

class WorldGlRenderer(
    private val simulation: GameSimulation,
    private var renderMode: GlRenderMode,
    private val onSimulationTick: ((Int) -> Unit)? = null
) : GLSurfaceView.Renderer {

    @Volatile
    var graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM

    private var programId = 0
    private var aPosition = 0
    private var aNormal = 0
    private var aColor = 0
    private var uMvpMatrix = 0
    private var uModelMatrix = 0
    private var uOverrideColor = 0
    private var uUseOverrideColor = 0
    private var uFogDensity = 0

    private lateinit var cubeBuffer: FloatBuffer
    private var cubeVertexCount = 0

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var surfaceWidth = 1280
    private var surfaceHeight = 720
    private var lastAppliedFov = 62f
    private var lastFrameNs = System.nanoTime()
    private var showcaseTime = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        programId = buildShaderProgram()
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
        uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uOverrideColor = GLES20.glGetUniformLocation(programId, "uOverrideColor")
        uUseOverrideColor = GLES20.glGetUniformLocation(programId, "uUseOverrideColor")
        uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")

        val (buf, count) = MeshBuilder.createUnitCube()
        cubeBuffer = buf
        cubeVertexCount = count
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val fov = if (renderMode == GlRenderMode.LOBBY_SHOWCASE) 42f else simulation.currentFov
        lastAppliedFov = fov
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.15f, 150f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now
        showcaseTime += dt

        if (renderMode == GlRenderMode.PLAYABLE_WORLD) {
            simulation.step(dt)
            if (kotlin.math.abs(simulation.currentFov - lastAppliedFov) > 0.25f) {
                lastAppliedFov = simulation.currentFov
                Matrix.perspectiveM(projMatrix, 0, lastAppliedFov, surfaceWidth.toFloat() / surfaceHeight, 0.15f, 150f)
            }
            onSimulationTick?.invoke(0)
            drawPlayableWorld()
        } else {
            drawLobbyShowcase()
        }
    }

    private fun drawLobbyShowcase() {
        GLES20.glClearColor(0.05f, 0.08f, 0.12f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(programId)
        GLES20.glUniform1f(uFogDensity, 0.015f)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 1.85f, 4.1f, 0f, 1.05f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val charSlot: CharacterSlot
        val outfit: OutfitStyle
        val headwear: HeadwearOption
        val accessory: AccessoryOption
        val colorTint: CharacterColorTint
        val emote: EmoteAction
        val yaw: Float

        synchronized(simulation.lock) {
            charSlot = simulation.activeCharacter
            outfit = simulation.activeOutfit
            headwear = simulation.activeHeadwear
            accessory = simulation.activeAccessory
            colorTint = simulation.activeColorTint
            emote = simulation.activeEmote
            yaw = simulation.showcaseYawRad + sin(showcaseTime * 0.45f) * 0.15f
        }

        drawCubePart(0f, -0.14f, 0f, 2.2f, 0.24f, 2.2f, 0f, 0.11f, 0.15f, 0.21f)
        drawCubePart(0f, 0.0f, 0f, 1.8f, 0.06f, 1.8f, yaw, 0.05f, 0.72f, 0.46f)

        drawCharacterModel(
            pos = Vec3(0f, 0.04f, 0f),
            yawRad = yaw,
            character = charSlot,
            outfit = outfit,
            headwear = headwear,
            accessory = accessory,
            colorTint = colorTint,
            walkPhase = 0f,
            isMoving = false,
            isCrouching = false,
            isSwimming = false,
            emote = emote,
            time = showcaseTime
        )
    }

    private fun drawPlayableWorld() {
        val sky = simulation.world.skyRgb
        GLES20.glClearColor(sky.first, sky.second, sky.third, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(programId)
        val fog = when (graphicsQuality) {
            GraphicsQuality.LOW -> 0.018f
            GraphicsQuality.MEDIUM -> 0.012f
            GraphicsQuality.HIGH -> 0.008f
        }
        GLES20.glUniform1f(uFogDensity, fog)

        val camPos: Vec3
        val camTarget: Vec3
        val pPos: Vec3
        val pYaw: Float
        val pPitch: Float
        val blend: Float
        val pWalk: Float
        val pMoving: Boolean
        val pCrouch: Boolean
        val pSwim: Boolean
        val driving: Boolean
        val charSlot: CharacterSlot
        val outfitStyle: OutfitStyle
        val headwear: HeadwearOption
        val accessory: AccessoryOption
        val colorTint: CharacterColorTint
        val activeWep: WeaponSpec
        val muzzleActive: Boolean
        val meleeSwing: Float
        val targetsCopy: List<RangeTarget>
        val vehiclesCopy: List<VehicleEntity>
        val lootCopy: List<GroundLootItem>
        val agentsCopy: List<EnemyAiAgent>
        val remotePlayersCopy: List<RemotePlayerSnapshot>
        val projectilesCopy: List<Projectile>
        val impactsCopy: List<ImpactEffect>
        val timeSec: Float

        synchronized(simulation.lock) {
            camPos = simulation.cameraPos
            camTarget = simulation.cameraLookAt
            pPos = simulation.playerPos
            pYaw = simulation.playerYawRad
            pPitch = simulation.playerPitchRad
            blend = simulation.cameraBlend
            pWalk = simulation.walkAnimPhase
            pMoving = simulation.isMoving
            pCrouch = simulation.isCrouching
            pSwim = simulation.isSwimming
            driving = simulation.activeVehicle != null
            charSlot = simulation.activeCharacter
            outfitStyle = simulation.activeOutfit
            headwear = simulation.activeHeadwear
            accessory = simulation.activeAccessory
            colorTint = simulation.activeColorTint
            activeWep = simulation.activeWeapon()
            muzzleActive = simulation.muzzleFlashSec > 0f
            meleeSwing = simulation.meleeSwingSec
            targetsCopy = simulation.rangeTargets.map { it.copy() }
            vehiclesCopy = simulation.vehicles.map { it.copy() }
            lootCopy = simulation.lootItems.filter { !it.collected }.map { it.copy() }
            agentsCopy = simulation.aiAgents.map { it.copy() }
            remotePlayersCopy = simulation.remotePlayers.toList()
            projectilesCopy = simulation.projectiles.map { it.copy() }
            impactsCopy = simulation.impacts.map { it.copy() }
            timeSec = simulation.elapsedTimeSec
        }

        Matrix.setLookAtM(
            viewMatrix, 0,
            camPos.x, camPos.y, camPos.z,
            camTarget.x, camTarget.y, camTarget.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Streamed District Static Chunks with LOD & Frustum Culling
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniform1i(uUseOverrideColor, 0)
        val currentWorld = simulation.world
        val chunks = currentWorld.spatialChunks
        if (chunks.isNotEmpty()) {
            val camForward = (camTarget - camPos).normalized()
            val visibility = simulation.streamingManager.evaluateChunksVisibility(currentWorld, camPos, camForward)
            chunks.forEachIndexed { idx, chunk ->
                if (visibility.getOrElse(idx) { true }) {
                    drawBuffer(chunk.buffer, chunk.vertexCount)
                }
            }
        } else {
            drawBuffer(currentWorld.staticBuffer, currentWorld.staticVertexCount)
        }

        // 2. Pulsing Beacons (Challenge Pad, Mission Objective, Travel Portal, Weapon Bench, Ammo Station)
        val pulse = 0.55f + 0.35f * sin(timeSec * 3.5f)
        val mb = currentWorld.missionBeaconPos
        drawCubePart(mb.x, 1.3f, mb.z, 0.25f, 2.6f, 0.25f, timeSec, 0.08f, pulse, 0.55f)
        val obj = currentWorld.missionObjectivePos
        drawCubePart(obj.x, 1.45f, obj.z, 0.32f, 2.5f, 0.32f, -timeSec * 1.2f, 0.08f, 0.88f, pulse)
        val portal = currentWorld.travelPortalPos
        drawCubePart(portal.x - 1.8f, 1.7f, portal.z, 0.38f, 3.4f, 0.38f, 0f, 0.96f, 0.65f, 0.14f)
        drawCubePart(portal.x + 1.8f, 1.7f, portal.z, 0.38f, 3.4f, 0.38f, 0f, 0.96f, 0.65f, 0.14f)
        drawCubePart(portal.x, 3.45f, portal.z, 4.1f, 0.42f, 0.52f, 0f, 0.96f, 0.65f, 0.14f)
        val wb = currentWorld.weaponBenchPos
        drawCubePart(wb.x, 1.15f, wb.z, 0.22f, 0.35f, 0.22f, timeSec * 1.4f, 0.06f, 0.85f, 0.92f)
        val ab = currentWorld.ammoStationPos
        drawCubePart(ab.x, 1.15f, ab.z, 0.22f, 0.35f, 0.22f, -timeSec * 1.4f, 0.96f, 0.72f, 0.14f)

        // 3. Commander Karim NPC
        val npcPos = simulation.world.npcCommanderPos
        drawCharacterModel(
            pos = npcPos,
            yawRad = 3.6f,
            character = com.example.game.characters.CharacterCatalog.slots[1],
            outfit = OutfitStyle.DHAKA_EMERALD,
            headwear = HeadwearOption.TACTICAL_CAP,
            accessory = AccessoryOption.UTILITY_BELT,
            colorTint = CharacterColorTint.DEFAULT,
            walkPhase = 0f,
            isMoving = false,
            isCrouching = false,
            isSwimming = false,
            emote = EmoteAction.SALUTE,
            time = timeSec
        )
        drawCubePart(npcPos.x, 2.35f + sin(timeSec * 3f) * 0.1f, npcPos.z, 0.26f, 0.26f, 0.26f, timeSec * 1.5f, 0.98f, 0.72f, 0.12f)

        // 4. Training Range Targets
        for (rt in targetsCopy) {
            val hitTint = rt.hitCooldownSec > 0f
            drawCubePart(rt.currentPos.x, rt.currentPos.y, rt.currentPos.z, 0.95f, 1.25f, 0.22f, 0f, if (hitTint) 0.12f else 0.92f, if (hitTint) 0.95f else 0.24f, if (hitTint) 0.55f else 0.24f)
            drawCubePart(rt.currentPos.x, rt.currentPos.y, rt.currentPos.z + 0.13f, 0.42f, 0.42f, 0.05f, 0f, 0.98f, 0.98f, 0.98f)
        }

        // 5. Ground Loot Pickups
        for (loot in lootCopy) {
            val bob = sin(timeSec * 3.2f + loot.x) * 0.08f
            val (lr, lg, lb) = when (loot.kind) {
                GroundLootKind.WEAPON_CRATE -> Triple(0.12f, 0.88f, 0.96f)
                GroundLootKind.AMMO_BOX -> Triple(0.96f, 0.72f, 0.14f)
                GroundLootKind.MEDKIT_DROP -> Triple(0.12f, 0.92f, 0.45f)
                GroundLootKind.ARMOR_DROP -> Triple(0.55f, 0.65f, 0.98f)
            }
            drawCubePart(loot.x, loot.y + bob, loot.z, 0.55f, 0.40f, 0.55f, timeSec * 1.4f, lr, lg, lb)
        }

        // 6. Drivable Vehicles (Car, Motorcycle, Bus, Truck, Boat)
        for (veh in vehiclesCopy) {
            drawVehicleModel(veh)
        }

        // 7. Intelligent Enemy AI Agents
        for (agent in agentsCopy) {
            if (!agent.isAlive) continue
            drawCharacterModel(
                pos = agent.pos,
                yawRad = -agent.yawRad,
                character = com.example.game.characters.CharacterCatalog.slots[2],
                outfit = OutfitStyle.CRIMSON_OPS,
                headwear = HeadwearOption.COMBAT_HELMET,
                accessory = AccessoryOption.SHOULDER_ARMOR,
                colorTint = CharacterColorTint.DEFAULT,
                walkPhase = agent.walkPhase,
                isMoving = agent.aiState == EnemyAiState.PATROL || agent.aiState == EnemyAiState.CHASE || agent.aiState == EnemyAiState.RETREAT,
                isCrouching = false,
                isSwimming = false,
                emote = EmoteAction.READY_STANCE,
                time = timeSec
            )
            // AI State Beacon above enemy
            val (sr, sg, sb) = when (agent.aiState) {
                EnemyAiState.ATTACK -> Triple(0.98f, 0.16f, 0.18f)
                EnemyAiState.CHASE, EnemyAiState.DETECT -> Triple(0.98f, 0.55f, 0.12f)
                EnemyAiState.RETREAT -> Triple(0.16f, 0.82f, 0.98f)
                else -> Triple(0.95f, 0.82f, 0.25f)
            }
            drawCubePart(agent.pos.x, 2.25f, agent.pos.z, 0.24f, 0.24f, 0.24f, timeSec * 2f, sr, sg, sb)
        }

        // 7b. Synchronized Multiplayer Remote Players
        for (rp in remotePlayersCopy) {
            if (!rp.isAlive) continue
            val rpChar = CharacterCatalog.getById(rp.characterId)
            val rpOutfit = when (rp.team) {
                TeamSide.ALPHA -> OutfitStyle.DHAKA_EMERALD
                TeamSide.BRAVO -> OutfitStyle.CRIMSON_OPS
                TeamSide.SOLO -> OutfitStyle.PADMA_CAMO
            }
            drawCharacterModel(
                pos = Vec3(rp.x, rp.y, rp.z),
                yawRad = -rp.yawRad,
                character = rpChar,
                outfit = rpOutfit,
                headwear = HeadwearOption.TACTICAL_CAP,
                accessory = AccessoryOption.TACTICAL_BACKPACK,
                colorTint = CharacterColorTint.DEFAULT,
                walkPhase = timeSec * 5f,
                isMoving = true,
                isCrouching = false,
                isSwimming = false,
                emote = EmoteAction.READY_STANCE,
                time = timeSec
            )
            val (tr, tg, tb) = when {
                !rp.isConnected -> Triple(0.45f, 0.45f, 0.48f)
                rp.team == TeamSide.ALPHA -> Triple(0.08f, 0.94f, 0.56f)
                rp.team == TeamSide.BRAVO -> Triple(0.96f, 0.22f, 0.28f)
                else -> Triple(0.98f, 0.75f, 0.14f)
            }
            drawCubePart(rp.x, rp.y + 2.28f, rp.z, 0.24f, 0.24f, 0.24f, timeSec * 2.2f, tr, tg, tb)
        }

        // 8. Projectiles & Impact Sparks
        for (proj in projectilesCopy) {
            drawCubePart(proj.pos.x, proj.pos.y, proj.pos.z, 0.16f, 0.16f, 0.44f, pYaw, proj.rgb.first, proj.rgb.second, proj.rgb.third)
        }
        for (imp in impactsCopy) {
            val s = (imp.lifeSec * 1.8f).coerceIn(0.1f, 0.42f)
            drawCubePart(imp.pos.x, imp.pos.y, imp.pos.z, s, s, s, timeSec * 4f, imp.rgb.first, imp.rgb.second, imp.rgb.third)
        }

        // 9. Player Character or 1P Weapon Viewmodel
        if (!driving) {
            if (blend > 0.25f) {
                drawCharacterModel(
                    pos = pPos,
                    yawRad = -pYaw,
                    character = charSlot,
                    outfit = outfitStyle,
                    headwear = headwear,
                    accessory = accessory,
                    colorTint = colorTint,
                    walkPhase = pWalk,
                    isMoving = pMoving,
                    isCrouching = pCrouch,
                    isSwimming = pSwim,
                    emote = EmoteAction.READY_STANCE,
                    time = timeSec
                )
            } else {
                val forward = GlTrig.forwardFromYawPitch(pYaw, pPitch)
                val right = GlTrig.rightFromYaw(pYaw)
                val swingOff = if (meleeSwing > 0f) sin(meleeSwing * 18f) * 0.22f else 0f
                val barrelPos = camPos + forward * 0.56f + right * (0.22f - swingOff) + Vec3(0f, -0.20f, 0f)
                drawCubePart(
                    barrelPos.x, barrelPos.y, barrelPos.z,
                    activeWep.modelWidth, activeWep.modelWidth * 1.2f, activeWep.modelLength,
                    -pYaw + swingOff,
                    0.16f, 0.19f, 0.24f
                )
                drawCubePart(
                    barrelPos.x, barrelPos.y + 0.05f, barrelPos.z,
                    activeWep.modelWidth * 0.5f, 0.03f, activeWep.modelLength * 0.65f,
                    -pYaw + swingOff,
                    activeWep.tracerRgb.first, activeWep.tracerRgb.second, activeWep.tracerRgb.third
                )
                if (muzzleActive) {
                    val flashPos = barrelPos + forward * (activeWep.modelLength * 0.55f)
                    drawCubePart(flashPos.x, flashPos.y, flashPos.z, 0.16f, 0.16f, 0.16f, timeSec * 12f, 1f, 0.88f, 0.35f)
                }
            }
        }
    }

    private fun drawVehicleModel(veh: VehicleEntity) {
        val t = veh.type
        val bodyR = if (veh.isOperational) t.bodyRgb.first else 0.18f
        val bodyG = if (veh.isOperational) t.bodyRgb.second else 0.18f
        val bodyB = if (veh.isOperational) t.bodyRgb.third else 0.19f
        val yaw = -veh.yawRad

        // Main chassis
        drawCubePart(veh.pos.x, veh.pos.y + t.height * 0.42f, veh.pos.z, t.width, t.height * 0.62f, t.length, yaw, bodyR, bodyG, bodyB)
        // Cabin / top accent
        val cabinLen = if (t == VehicleType.TRUCK) t.length * 0.36f else t.length * 0.56f
        val cabinY = veh.pos.y + t.height * 0.82f
        drawCubePart(veh.pos.x, cabinY, veh.pos.z, t.width * 0.86f, t.height * 0.42f, cabinLen, yaw, t.accentRgb.first, t.accentRgb.second, t.accentRgb.third)

        // Wheels for land vehicles
        if (!t.isWatercraft) {
            val s = sin(-yaw)
            val c = cos(-yaw)
            val wx = t.width * 0.48f
            val wz = t.length * 0.33f
            val offsets = listOf(Pair(-wx, -wz), Pair(wx, -wz), Pair(-wx, wz), Pair(wx, wz))
            for ((ox, oz) in offsets) {
                val px = veh.pos.x + ox * c - oz * s
                val pz = veh.pos.z + ox * s + oz * c
                drawCubePart(px, 0.32f, pz, 0.28f, 0.62f, 0.62f, yaw, 0.08f, 0.08f, 0.10f)
            }
        }
    }

    private fun drawCharacterModel(
        pos: Vec3,
        yawRad: Float,
        character: CharacterSlot,
        outfit: OutfitStyle,
        headwear: HeadwearOption,
        accessory: AccessoryOption,
        colorTint: CharacterColorTint,
        walkPhase: Float,
        isMoving: Boolean,
        isCrouching: Boolean,
        isSwimming: Boolean,
        emote: EmoteAction,
        time: Float
    ) {
        val crouchScale = if (isCrouching) 0.72f else if (isSwimming) 0.78f else 1.0f
        val swing = if (isMoving) sin(walkPhase) * 0.24f else 0f
        val breathe = sin(time * 2.4f) * 0.02f

        val primary = colorTint.rgb ?: outfit.primaryRgb
        val accent = outfit.accentRgb
        val skin = character.skinToneRgb
        val hair = character.hairRgb

        fun localOffset(lx: Float, ly: Float, lz: Float): Vec3 {
            val s = sin(-yawRad)
            val c = cos(-yawRad)
            return Vec3(pos.x + lx * c - lz * s, pos.y + ly * crouchScale, pos.z + lx * s + lz * c)
        }

        // Legs
        val leftLeg = localOffset(-0.14f, 0.42f, swing)
        val rightLeg = localOffset(0.14f, 0.42f, -swing)
        drawCubePart(leftLeg.x, leftLeg.y, leftLeg.z, 0.20f, 0.80f * crouchScale, 0.22f, yawRad, 0.13f, 0.16f, 0.21f)
        drawCubePart(rightLeg.x, rightLeg.y, rightLeg.z, 0.20f, 0.80f * crouchScale, 0.22f, yawRad, 0.13f, 0.16f, 0.21f)

        // Torso & Chest Rig
        val torso = localOffset(0f, 1.08f + breathe, 0f)
        drawCubePart(torso.x, torso.y, torso.z, 0.52f, 0.62f * crouchScale, 0.28f, yawRad, primary.first, primary.second, primary.third)
        val chestRig = localOffset(0f, 1.12f + breathe, 0f)
        drawCubePart(chestRig.x, chestRig.y, chestRig.z, 0.55f, 0.24f * crouchScale, 0.31f, yawRad, accent.first, accent.second, accent.third)

        // Accessories
        when (accessory) {
            AccessoryOption.TACTICAL_BACKPACK -> {
                val pack = localOffset(0f, 1.10f + breathe, 0.24f)
                drawCubePart(pack.x, pack.y, pack.z, 0.40f, 0.50f * crouchScale, 0.20f, yawRad, 0.20f, 0.24f, 0.28f)
            }
            AccessoryOption.UTILITY_BELT -> {
                val belt = localOffset(0f, 0.82f + breathe, 0f)
                drawCubePart(belt.x, belt.y, belt.z, 0.56f, 0.12f * crouchScale, 0.32f, yawRad, 0.88f, 0.64f, 0.15f)
            }
            AccessoryOption.SHOULDER_ARMOR -> {
                val lp = localOffset(-0.36f, 1.32f + breathe, 0f)
                val rp = localOffset(0.36f, 1.32f + breathe, 0f)
                drawCubePart(lp.x, lp.y, lp.z, 0.22f, 0.16f, 0.26f, yawRad, accent.first, accent.second, accent.third)
                drawCubePart(rp.x, rp.y, rp.z, 0.22f, 0.16f, 0.26f, yawRad, accent.first, accent.second, accent.third)
            }
            AccessoryOption.NONE -> Unit
        }

        // Arms (swim strokes or emotes)
        val swimRaise = if (isSwimming) 0.25f + sin(walkPhase * 1.5f) * 0.20f else 0f
        val rightArmRaise = when (emote) {
            EmoteAction.SALUTE -> 0.32f
            EmoteAction.VICTORY_FIST -> 0.45f + sin(time * 4f) * 0.06f
            EmoteAction.COMBAT_WARMUP -> 0.18f + sin(time * 6f) * 0.08f
            EmoteAction.READY_STANCE -> 0f
        }
        val leftArm = localOffset(-0.36f, 1.06f + breathe + swimRaise, -swing * 0.8f)
        val rightArm = localOffset(0.36f, 1.06f + breathe + rightArmRaise + swimRaise, swing * 0.8f)
        drawCubePart(leftArm.x, leftArm.y, leftArm.z, 0.17f, 0.58f * crouchScale, 0.18f, yawRad, primary.first * 0.9f, primary.second * 0.9f, primary.third * 0.9f)
        drawCubePart(rightArm.x, rightArm.y, rightArm.z, 0.17f, 0.58f * crouchScale, 0.18f, yawRad, primary.first * 0.9f, primary.second * 0.9f, primary.third * 0.9f)

        // Head & Headwear
        val head = localOffset(0f, 1.58f + breathe, 0f)
        drawCubePart(head.x, head.y, head.z, 0.34f, 0.34f, 0.34f, yawRad, skin.first, skin.second, skin.third)
        when (headwear) {
            HeadwearOption.NONE -> {
                val hairCap = localOffset(0f, 1.75f + breathe, 0f)
                drawCubePart(hairCap.x, hairCap.y, hairCap.z, 0.36f, 0.11f, 0.36f, yawRad, hair.first, hair.second, hair.third)
            }
            HeadwearOption.TACTICAL_CAP -> {
                val cap = localOffset(0f, 1.76f + breathe, 0f)
                drawCubePart(cap.x, cap.y, cap.z, 0.37f, 0.12f, 0.38f, yawRad, headwear.rgb.first, headwear.rgb.second, headwear.rgb.third)
                val brim = localOffset(0f, 1.71f + breathe, -0.22f)
                drawCubePart(brim.x, brim.y, brim.z, 0.34f, 0.04f, 0.16f, yawRad, headwear.rgb.first, headwear.rgb.second, headwear.rgb.third)
            }
            HeadwearOption.COMBAT_HELMET -> {
                val helm = localOffset(0f, 1.74f + breathe, 0f)
                drawCubePart(helm.x, helm.y, helm.z, 0.42f, 0.22f, 0.42f, yawRad, headwear.rgb.first, headwear.rgb.second, headwear.rgb.third)
            }
            HeadwearOption.VISOR_SHADES -> {
                val hairCap = localOffset(0f, 1.75f + breathe, 0f)
                drawCubePart(hairCap.x, hairCap.y, hairCap.z, 0.36f, 0.11f, 0.36f, yawRad, hair.first, hair.second, hair.third)
                val visor = localOffset(0f, 1.62f + breathe, -0.18f)
                drawCubePart(visor.x, visor.y, visor.z, 0.32f, 0.09f, 0.06f, yawRad, headwear.rgb.first, headwear.rgb.second, headwear.rgb.third)
            }
            HeadwearOption.HEADBAND -> {
                val band = localOffset(0f, 1.69f + breathe, 0f)
                drawCubePart(band.x, band.y, band.z, 0.37f, 0.08f, 0.37f, yawRad, headwear.rgb.first, headwear.rgb.second, headwear.rgb.third)
            }
        }
    }

    private fun drawCubePart(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        yawRad: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniform1i(uUseOverrideColor, 1)
        GLES20.glUniform3f(uOverrideColor, r, g, b)

        drawBuffer(cubeBuffer, cubeVertexCount)
    }

    private fun drawBuffer(buffer: FloatBuffer, vertexCount: Int) {
        val stride = 9 * 4
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(6)
        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, stride, buffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    private fun buildShaderProgram(): Int {
        val vsCode = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;
            varying vec3 vColor;
            varying vec3 vNormal;
            varying float vViewDist;
            void main() {
                vec4 clipPos = uMvpMatrix * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vColor = aColor;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vViewDist = length(clipPos.xyz);
            }
        """.trimIndent()

        val fsCode = """
            precision mediump float;
            uniform vec3 uOverrideColor;
            uniform int uUseOverrideColor;
            uniform float uFogDensity;
            varying vec3 vColor;
            varying vec3 vNormal;
            varying float vViewDist;
            void main() {
                vec3 baseColor = (uUseOverrideColor == 1) ? uOverrideColor : vColor;
                vec3 sunDir = normalize(vec3(0.48, 0.78, 0.38));
                float ndotl = max(dot(normalize(vNormal), sunDir), 0.0);
                float hemi = 0.42 + 0.14 * normalize(vNormal).y;
                vec3 lit = baseColor * (hemi + ndotl * 0.72);
                float fogFactor = 1.0 - exp(-vViewDist * uFogDensity);
                vec3 fogColor = vec3(0.11, 0.16, 0.24);
                vec3 finalColor = mix(lit, fogColor, clamp(fogFactor, 0.0, 0.85));
                gl_FragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsCode)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsCode)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
