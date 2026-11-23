package com.example.game.engine

import com.example.game.ai.AiDifficulty
import com.example.game.ai.DhakaNavGrid
import com.example.game.ai.EnemyAiAgent
import com.example.game.ai.EnemyAiState
import com.example.game.vehicles.VehicleEntity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class ImpactEffect(
    var pos: Vec3,
    var lifeSec: Float,
    var rgb: Triple<Float, Float, Float>
)

object VehiclePhysicsStepper {
    fun stepVehicle(
        vehicle: VehicleEntity,
        throttleInput: Float,
        brakeReverseInput: Float,
        steerInput: Float,
        dt: Float,
        world: DhakaWorldGeometry
    ) {
        if (!vehicle.isOperational) {
            vehicle.speed = 0f
            return
        }

        val hasFuel = vehicle.fuel > 0.1f
        val accel = if (hasFuel) throttleInput * vehicle.type.acceleration else 0f
        val brake = brakeReverseInput * vehicle.type.acceleration * 1.15f

        vehicle.speed = when {
            throttleInput > 0.05f && hasFuel -> min(vehicle.type.maxSpeed, vehicle.speed + accel * dt)
            brakeReverseInput > 0.05f -> max(-vehicle.type.reverseSpeed, vehicle.speed - brake * dt)
            else -> {
                val drag = 6.5f * dt
                if (abs(vehicle.speed) <= drag) 0f else vehicle.speed - kotlin.math.sign(vehicle.speed) * drag
            }
        }

        if (abs(vehicle.speed) > 0.15f && hasFuel) {
            vehicle.fuel = max(0f, vehicle.fuel - vehicle.type.fuelBurnPerSec * (abs(vehicle.speed) / vehicle.type.maxSpeed) * dt)
        }

        if (abs(vehicle.speed) > 0.25f) {
            val dirSign = if (vehicle.speed >= 0f) 1f else -1f
            val turnStrength = (abs(vehicle.speed) / 4.5f).coerceIn(0.35f, 1.0f)
            vehicle.yawRad += steerInput * vehicle.type.turnRateRad * turnStrength * dirSign * dt
        }

        val forward = Vec3(sin(vehicle.yawRad), 0f, -cos(vehicle.yawRad))
        var nextX = vehicle.pos.x + forward.x * vehicle.speed * dt
        var nextZ = vehicle.pos.z + forward.z * vehicle.speed * dt

        // Keep watercraft inside water zone and land vehicles outside deep water
        if (vehicle.type.isWatercraft) {
            val wz = world.waterZone
            nextX = nextX.coerceIn(wz.minX + 1.4f, wz.maxX - 1.4f)
            nextZ = nextZ.coerceIn(wz.minZ + 1.8f, wz.maxZ - 1.8f)
        } else {
            val radius = max(vehicle.type.width, vehicle.type.length) * 0.42f
            for (c in world.colliders) {
                val resolved = c.resolveCircleXz(nextX, 0f, nextZ, radius, vehicle.type.height)
                if (resolved != null) {
                    if (abs(vehicle.speed) > 7.5f) {
                        vehicle.health = (vehicle.health - 8).coerceAtLeast(0)
                    }
                    nextX = resolved.first
                    nextZ = resolved.second
                    vehicle.speed *= 0.35f
                }
            }
            val limit = DhakaWorldData.ARENA_HALF_SIZE - 1.6f
            nextX = nextX.coerceIn(-limit, limit)
            nextZ = nextZ.coerceIn(-limit, limit)
        }

        vehicle.pos = Vec3(nextX, vehicle.pos.y, nextZ)
    }
}

object EnemyAiStepper {
    fun stepAgents(
        agents: List<EnemyAiAgent>,
        playerPos: Vec3,
        playerNoiseRadius: Float,
        difficulty: AiDifficulty,
        navGrid: DhakaNavGrid,
        world: DhakaWorldGeometry,
        dt: Float,
        onFireProjectile: (Vec3, Vec3, Int) -> Unit
    ) {
        val playerEye = Vec3(playerPos.x, playerPos.y + 1.2f, playerPos.z)

        for (agent in agents) {
            if (!agent.isAlive) {
                agent.aiState = EnemyAiState.DEAD
                continue
            }

            agent.stateTimerSec += dt
            agent.fireCooldownSec = max(0f, agent.fireCooldownSec - dt)

            val agentEye = Vec3(agent.pos.x, agent.pos.y + 1.35f, agent.pos.z)
            val distToPlayer = agent.pos.distanceTo(playerPos)
            val canSeePlayer = distToPlayer <= difficulty.detectionRange && navGrid.hasLineOfSight(agentEye, playerEye)
            val hearsPlayer = distToPlayer <= playerNoiseRadius

            // State Transitions
            val lowHp = agent.hp <= (agent.maxHp * 0.28f)
            when (agent.aiState) {
                EnemyAiState.IDLE -> {
                    if (canSeePlayer || hearsPlayer) {
                        agent.aiState = EnemyAiState.DETECT
                        agent.stateTimerSec = 0f
                        agent.lastKnownPlayerPos = playerPos
                    } else if (agent.stateTimerSec > 1.8f) {
                        agent.aiState = EnemyAiState.PATROL
                        agent.stateTimerSec = 0f
                    }
                }
                EnemyAiState.PATROL -> {
                    if (canSeePlayer || hearsPlayer) {
                        agent.aiState = EnemyAiState.DETECT
                        agent.stateTimerSec = 0f
                        agent.lastKnownPlayerPos = playerPos
                    }
                }
                EnemyAiState.DETECT -> {
                    agent.lastKnownPlayerPos = playerPos
                    if (agent.stateTimerSec >= 0.45f) {
                        agent.aiState = when {
                            lowHp -> EnemyAiState.RETREAT
                            canSeePlayer && distToPlayer <= 15f -> EnemyAiState.ATTACK
                            else -> EnemyAiState.CHASE
                        }
                        agent.stateTimerSec = 0f
                    }
                }
                EnemyAiState.CHASE -> {
                    if (canSeePlayer) {
                        agent.lastKnownPlayerPos = playerPos
                        if (lowHp) {
                            agent.aiState = EnemyAiState.RETREAT
                        } else if (distToPlayer <= 13.5f) {
                            agent.aiState = EnemyAiState.ATTACK
                        }
                    } else {
                        agent.aiState = EnemyAiState.SEARCH
                        agent.stateTimerSec = 0f
                    }
                }
                EnemyAiState.ATTACK -> {
                    if (lowHp) {
                        agent.aiState = EnemyAiState.RETREAT
                        agent.stateTimerSec = 0f
                    } else if (!canSeePlayer) {
                        agent.aiState = EnemyAiState.SEARCH
                        agent.stateTimerSec = 0f
                    } else {
                        agent.lastKnownPlayerPos = playerPos
                        if (distToPlayer > 17.5f) {
                            agent.aiState = EnemyAiState.CHASE
                        }
                    }
                }
                EnemyAiState.RETREAT -> {
                    if (!lowHp && canSeePlayer && distToPlayer > 10f) {
                        agent.aiState = EnemyAiState.ATTACK
                    } else if (distToPlayer > 22f && agent.stateTimerSec > 3.5f) {
                        agent.aiState = EnemyAiState.SEARCH
                        agent.stateTimerSec = 0f
                    }
                }
                EnemyAiState.SEARCH -> {
                    if (canSeePlayer) {
                        agent.aiState = EnemyAiState.ATTACK
                        agent.stateTimerSec = 0f
                        agent.lastKnownPlayerPos = playerPos
                    } else if (agent.stateTimerSec > 5.0f) {
                        agent.aiState = EnemyAiState.PATROL
                        agent.stateTimerSec = 0f
                    }
                }
                EnemyAiState.DEAD -> Unit
            }

            // State Actions & NavGrid Steering
            var moveDir = Vec3(0f, 0f, 0f)
            var moveSpeed = 0f

            when (agent.aiState) {
                EnemyAiState.IDLE -> Unit
                EnemyAiState.PATROL -> {
                    val wp = agent.patrolPoints[agent.patrolIndex % agent.patrolPoints.size]
                    if (agent.pos.distanceTo(wp) < 1.2f) {
                        agent.patrolIndex = (agent.patrolIndex + 1) % agent.patrolPoints.size
                        agent.aiState = EnemyAiState.IDLE
                        agent.stateTimerSec = 0f
                    } else {
                        moveDir = navGrid.computeSteeringDirection(agent.pos, wp)
                        moveSpeed = 2.4f
                    }
                }
                EnemyAiState.DETECT -> {
                    val toP = (playerPos - agent.pos)
                    agent.yawRad = atan2(toP.x, -toP.z)
                }
                EnemyAiState.CHASE -> {
                    val target = agent.lastKnownPlayerPos ?: playerPos
                    moveDir = navGrid.computeSteeringDirection(agent.pos, target)
                    moveSpeed = 4.3f
                }
                EnemyAiState.ATTACK -> {
                    val toP = (playerPos - agent.pos)
                    agent.yawRad = atan2(toP.x, -toP.z)
                    // Strafe gently while attacking
                    val strafeSign = if ((agent.id + (agent.stateTimerSec.toInt() / 2)) % 2 == 0) 1f else -1f
                    moveDir = GlTrig.rightFromYaw(agent.yawRad) * strafeSign
                    moveSpeed = 1.6f

                    if (agent.fireCooldownSec <= 0f) {
                        agent.fireCooldownSec = 1.65f * difficulty.fireCooldownScale
                        val dir = (playerEye - agentEye).normalized()
                        val dmg = (13f * difficulty.damageMultiplier).toInt().coerceAtLeast(6)
                        onFireProjectile(agentEye + dir * 0.6f, dir * 18f, dmg)
                    }
                }
                EnemyAiState.RETREAT -> {
                    val away = (agent.pos - playerPos).normalized()
                    val retreatTarget = agent.pos + away * 8f
                    moveDir = navGrid.computeSteeringDirection(agent.pos, retreatTarget)
                    moveSpeed = 4.6f
                }
                EnemyAiState.SEARCH -> {
                    val lastPos = agent.lastKnownPlayerPos
                    if (lastPos != null && agent.pos.distanceTo(lastPos) > 1.3f) {
                        moveDir = navGrid.computeSteeringDirection(agent.pos, lastPos)
                        moveSpeed = 3.2f
                    } else {
                        agent.yawRad += dt * 1.8f
                    }
                }
                EnemyAiState.DEAD -> Unit
            }

            if (moveSpeed > 0.05f && (abs(moveDir.x) > 0.01f || abs(moveDir.z) > 0.01f)) {
                var nx = agent.pos.x + moveDir.x * moveSpeed * dt
                var nz = agent.pos.z + moveDir.z * moveSpeed * dt
                for (c in world.colliders) {
                    val res = c.resolveCircleXz(nx, 0f, nz, 0.42f, 1.8f)
                    if (res != null) {
                        nx = res.first
                        nz = res.second
                    }
                }
                val limit = DhakaWorldData.ARENA_HALF_SIZE - 0.8f
                agent.pos = Vec3(nx.coerceIn(-limit, limit), 0f, nz.coerceIn(-limit, limit))
                if (agent.aiState != EnemyAiState.ATTACK) {
                    agent.yawRad = atan2(moveDir.x, -moveDir.z)
                }
                agent.walkPhase += dt * moveSpeed * 2.2f
            }
        }
    }
}
