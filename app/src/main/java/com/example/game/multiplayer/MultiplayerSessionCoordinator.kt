package com.example.game.multiplayer

import com.example.game.characters.CharacterCatalog
import com.example.game.modes.GameModeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Coordinates the Local Authoritative TCP Test Server (127.0.0.1), the primary player's
 * SocketMultiplayerClient, up to 3 companion test socket clients (for 4-player verification),
 * and the LocalTestVoiceProvider.
 */
class MultiplayerSessionCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val server = AuthoritativeMatchServer()
    val primaryClient = SocketMultiplayerClient()
    val voiceProvider = LocalTestVoiceProvider()

    private val companionClients = ArrayList<SocketMultiplayerClient>()
    private var companionSimJob: Job? = null
    private var monitorJob: Job? = null

    private val _serverPort = MutableStateFlow(-1)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<ServerAuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<ServerAuditLogEntry>> = _auditLogs.asStateFlow()

    private val _isFourPlayerTestActive = MutableStateFlow(false)
    val isFourPlayerTestActive: StateFlow<Boolean> = _isFourPlayerTestActive.asStateFlow()

    val connectionFlow: StateFlow<ConnectionState> = primaryClient.connectionFlow
    val activeRoomFlow: StateFlow<RoomSessionInfo?> = primaryClient.activeRoomFlow
    val publicRoomsFlow: StateFlow<List<PublicRoomSummary>> = primaryClient.publicRoomsFlow
    val worldStateFlow: StateFlow<WorldStateSnapshot?> = primaryClient.worldStateFlow
    val chatMessagesFlow: StateFlow<List<ChatMessage>> = primaryClient.chatMessagesFlow
    val errorNoticeFlow: StateFlow<String?> = primaryClient.errorNoticeFlow
    val verifiedRewardFlow: StateFlow<VerifiedMatchReward?> = primaryClient.verifiedRewardFlow

    suspend fun ensureLocalServerAndConnect(
        playerId: String,
        username: String,
        characterId: String,
        weaponId: String
    ): Result<Int> {
        val port = if (server.isRunning) server.port else server.start()
        _serverPort.value = port
        if (primaryClient.connectionState != ConnectionState.IN_ROOM) {
            val res = primaryClient.connectToServer(
                host = "127.0.0.1",
                port = port,
                playerId = playerId,
                username = username,
                characterId = characterId,
                weaponId = weaponId
            )
            if (res.isFailure) return Result.failure(res.exceptionOrNull() ?: IllegalStateException("Connect error"))
            delay(80L)
        } else {
            primaryClient.requestRoomList()
        }
        startMonitorLoop()
        return Result.success(port)
    }

    private fun startMonitorLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                val room = primaryClient.activeRoomFlow.value
                if (room != null) {
                    voiceProvider.syncRoomParticipants(room.players)
                    if (room.phase == MatchPhase.IN_MATCH && companionClients.isNotEmpty() && companionSimJob?.isActive != true) {
                        startCompanionMovementLoop()
                    } else if (room.phase != MatchPhase.IN_MATCH) {
                        companionSimJob?.cancel()
                    }
                }
                _auditLogs.value = server.auditLogs.toList().takeLast(20).reversed()
                delay(200L)
            }
        }
    }

    suspend fun createRoom(
        privacy: RoomPrivacy,
        mode: GameModeId,
        districtId: String,
        playerId: String,
        username: String,
        characterId: String,
        weaponId: String
    ) {
        disconnectCompanions()
        ensureLocalServerAndConnect(playerId, username, characterId, weaponId)
        primaryClient.createRoom(privacy, mode, districtId, mode.defaultMaxPlayers.coerceIn(1, 4))
        delay(80L)
        primaryClient.setCharacterAndWeapon(characterId, weaponId)
    }

    suspend fun quickMatch(
        mode: GameModeId,
        districtId: String,
        playerId: String,
        username: String,
        characterId: String,
        weaponId: String
    ) {
        disconnectCompanions()
        ensureLocalServerAndConnect(playerId, username, characterId, weaponId)
        primaryClient.startQuickMatch(mode, districtId, mode.defaultMaxPlayers.coerceIn(1, 4))
        delay(80L)
        primaryClient.setCharacterAndWeapon(characterId, weaponId)
    }

    suspend fun joinRoomByCode(
        code: String,
        playerId: String,
        username: String,
        characterId: String,
        weaponId: String
    ) {
        disconnectCompanions()
        ensureLocalServerAndConnect(playerId, username, characterId, weaponId)
        primaryClient.joinRoomByCode(code)
        delay(80L)
        primaryClient.setCharacterAndWeapon(characterId, weaponId)
    }

    /**
     * Spins up a complete 4-player authoritative test room using 4 real TCP socket connections
     * (primary local player + 3 companion SocketMultiplayerClient sockets), assigns distinct
     * characters and balanced teams, marks all ready, and leaves match launch ready to go.
     */
    suspend fun launchFourPlayerTestRoom(
        mode: GameModeId,
        districtId: String,
        localPlayerId: String,
        localUsername: String,
        localCharacterId: String,
        localWeaponId: String
    ): Result<RoomSessionInfo> {
        disconnectCompanions()
        val effectiveMode = if (mode.defaultMaxPlayers >= 4) mode else GameModeId.FOUR_PLAYER_SQUAD
        val portRes = ensureLocalServerAndConnect(localPlayerId, localUsername, localCharacterId, localWeaponId)
        if (portRes.isFailure) return Result.failure(portRes.exceptionOrNull() ?: IllegalStateException("Server failed"))
        val port = portRes.getOrThrow()

        primaryClient.createRoom(RoomPrivacy.PUBLIC, effectiveMode, districtId, 4)
        var room: RoomSessionInfo? = null
        repeat(20) {
            delay(40L)
            val candidate = primaryClient.activeRoomFlow.value
            if (candidate != null && candidate.mode == effectiveMode) {
                room = candidate
                return@repeat
            }
        }
        val created = room ?: return Result.failure(IllegalStateException("Timed out creating 4-player test room"))
        primaryClient.setCharacterAndWeapon(localCharacterId, localWeaponId)
        if (effectiveMode.supportsTeams) {
            primaryClient.setTeam(TeamSide.ALPHA)
        }
        primaryClient.setLocalReady(true)

        val remainingChars = CharacterCatalog.slots.filter { it.id != localCharacterId } + CharacterCatalog.slots
        val companionNames = listOf("RafiqBD", "NadiaOps", "TariqSniper")
        val companionWeapons = listOf("ar_bengal_akm", "smg_vector", "dmr_sks")

        for (i in 0 until 3) {
            val client = SocketMultiplayerClient()
            companionClients.add(client)
            val compId = "test_sq_${i + 2}"
            val compChar = remainingChars[i].id
            val compWep = companionWeapons[i]
            client.connectToServer("127.0.0.1", port, compId, companionNames[i], compChar, compWep)
            delay(45L)
            client.joinRoomByCode(created.roomCode)
            delay(45L)
            client.setCharacterAndWeapon(compChar, compWep)
            val team = when {
                !effectiveMode.supportsTeams -> TeamSide.SOLO
                i == 0 -> TeamSide.ALPHA // 2 Alpha (local + companion 0), 2 Bravo (companions 1 & 2)
                else -> TeamSide.BRAVO
            }
            client.setTeam(team)
            client.setLocalReady(true)
        }
        delay(80L)
        _isFourPlayerTestActive.value = true
        return Result.success(primaryClient.activeRoomFlow.value ?: created)
    }

    private fun startCompanionMovementLoop() {
        companionSimJob?.cancel()
        companionSimJob = scope.launch {
            var t = 0f
            while (isActive) {
                val room = primaryClient.activeRoomFlow.value ?: break
                if (room.phase != MatchPhase.IN_MATCH) break
                val worldPlayers = primaryClient.worldStateFlow.value?.players.orEmpty()
                companionClients.forEachIndexed { idx, c ->
                    val mySnap = worldPlayers.firstOrNull { it.playerId == c.localPlayerId }
                    if (mySnap != null && mySnap.isAlive) {
                        val baseAngle = t * (0.55f + idx * 0.12f) + idx * 2.1f
                        val radius = 6.5f + idx * 2.0f
                        val goalX = cos(baseAngle) * radius
                        val goalZ = sin(baseAngle) * radius
                        val dx = goalX - mySnap.x
                        val dz = goalZ - mySnap.z
                        val dist = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
                        val maxStep = 0.60f // ~5.0 m/s over 120ms tick
                        val tx = if (dist > maxStep) mySnap.x + (dx / dist) * maxStep else goalX
                        val tz = if (dist > maxStep) mySnap.z + (dz / dist) * maxStep else goalZ
                        val yaw = (baseAngle + 1.57f)
                        c.sendMove(tx, 0f, tz, yaw, if (idx == 0) "ar_bengal_akm" else "smg_vector")

                        // Fire periodically if enemy team is roughly ahead during Team Battle/Survival
                        if ((t * 10).toInt() % 18 == idx * 5 && room.mode.supportsTeams) {
                            val enemy = worldPlayers.firstOrNull { it.isAlive && it.team != mySnap.team }
                            if (enemy != null) {
                                c.sendFire(enemy.x - tx, 0f, enemy.z - tz)
                            }
                        }
                    }
                }
                t += 0.12f
                delay(120L)
            }
        }
    }

    suspend fun triggerDisconnectForTest() {
        primaryClient.simulateConnectionDrop()
    }

    suspend fun reconnectLocalClient(): Result<Unit> {
        return primaryClient.reconnectSession()
    }

    fun leaveCurrentRoom() {
        scope.launch {
            disconnectCompanions()
            primaryClient.leaveRoom()
        }
    }

    private suspend fun disconnectCompanions() {
        companionSimJob?.cancel()
        companionSimJob = null
        _isFourPlayerTestActive.value = false
        val copy = companionClients.toList()
        companionClients.clear()
        copy.forEach {
            try {
                it.disconnect()
                it.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    fun shutdown() {
        monitorJob?.cancel()
        companionSimJob?.cancel()
        companionClients.forEach { it.shutdown() }
        companionClients.clear()
        primaryClient.shutdown()
        server.stop()
        scope.cancel()
    }
}
