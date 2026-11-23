package com.example.game.multiplayer

import com.example.game.modes.GameModeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Locale

data class VerifiedMatchReward(
    val xp: Int,
    val taka: Int,
    val unlockedDistrictId: String
)

/**
 * Real TCP Socket client for connecting to AuthoritativeMatchServer (local loopback test mode
 * or external dedicated relay server), supporting reconnect token session recovery.
 */
class SocketMultiplayerClient : INetworkTransport, IMatchmakingService, IRoomSessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var readJob: Job? = null

    private var lastHost: String = "127.0.0.1"
    private var lastPort: Int = -1
    var localPlayerId: String = ""
        private set
    var localUsername: String = "Warrior"
        private set
    var sessionToken: String = ""
        private set

    private val _connectionFlow = MutableStateFlow(ConnectionState.OFFLINE_LOCAL_PROTOTYPE)
    val connectionFlow: StateFlow<ConnectionState> = _connectionFlow.asStateFlow()

    override val connectionState: ConnectionState
        get() = _connectionFlow.value

    private val _activeRoomFlow = MutableStateFlow<RoomSessionInfo?>(null)
    val activeRoomFlow: StateFlow<RoomSessionInfo?> = _activeRoomFlow.asStateFlow()

    override val activeRoom: RoomSessionInfo?
        get() = _activeRoomFlow.value

    private val _publicRoomsFlow = MutableStateFlow<List<PublicRoomSummary>>(emptyList())
    val publicRoomsFlow: StateFlow<List<PublicRoomSummary>> = _publicRoomsFlow.asStateFlow()

    private val _worldStateFlow = MutableStateFlow<WorldStateSnapshot?>(null)
    val worldStateFlow: StateFlow<WorldStateSnapshot?> = _worldStateFlow.asStateFlow()

    private val _chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessagesFlow: StateFlow<List<ChatMessage>> = _chatMessagesFlow.asStateFlow()

    private val _errorNoticeFlow = MutableStateFlow<String?>(null)
    val errorNoticeFlow: StateFlow<String?> = _errorNoticeFlow.asStateFlow()

    private val _verifiedRewardFlow = MutableStateFlow<VerifiedMatchReward?>(null)
    val verifiedRewardFlow: StateFlow<VerifiedMatchReward?> = _verifiedRewardFlow.asStateFlow()

    suspend fun connectToServer(
        host: String,
        port: Int,
        playerId: String,
        username: String,
        characterId: String,
        weaponId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            closeSocketOnly()
            _connectionFlow.value = ConnectionState.CONNECTING
            _errorNoticeFlow.value = null
            lastHost = host
            lastPort = port
            localPlayerId = playerId
            localUsername = username

            val sock = Socket(host, port)
            socket = sock
            writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
            readJob = scope.launch { readLoop(sock) }
            sendLine("HELLO|$playerId|$username|$characterId|$weaponId|")
            _connectionFlow.value = ConnectionState.IN_ROOM
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionFlow.value = ConnectionState.DISCONNECTED
            val msg = "Unable to connect to local server: ${e.localizedMessage ?: "Connection refused"}"
            _errorNoticeFlow.value = msg
            Result.failure(e)
        }
    }

    override suspend fun connect(endpoint: String): Result<Unit> {
        val parts = endpoint.split(":")
        val host = parts.getOrNull(0) ?: "127.0.0.1"
        val port = parts.getOrNull(1)?.toIntOrNull() ?: return Result.failure(IllegalArgumentException("Invalid endpoint"))
        return connectToServer(host, port, localPlayerId.ifBlank { "local_player" }, localUsername, "hero_alam", "ar_dhaka_m4")
    }

    suspend fun simulateConnectionDrop() = withContext(Dispatchers.IO) {
        closeSocketOnly()
        _connectionFlow.value = ConnectionState.DISCONNECTED
        _errorNoticeFlow.value = "Connection lost. Tap Reconnect to recover session."
    }

    suspend fun reconnectSession(): Result<Unit> = withContext(Dispatchers.IO) {
        val roomId = _activeRoomFlow.value?.roomId
        if (lastPort <= 0 || roomId.isNullOrBlank() || sessionToken.isBlank()) {
            _errorNoticeFlow.value = "No active room session available for reconnect."
            return@withContext Result.failure(IllegalStateException("Missing reconnect state"))
        }
        try {
            _connectionFlow.value = ConnectionState.RECONNECTING
            closeSocketOnly()
            val sock = Socket(lastHost, lastPort)
            socket = sock
            writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
            readJob = scope.launch { readLoop(sock) }
            sendLine("RECONNECT|$localPlayerId|$roomId|$sessionToken")
            _connectionFlow.value = ConnectionState.IN_ROOM
            _errorNoticeFlow.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionFlow.value = ConnectionState.DISCONNECTED
            _errorNoticeFlow.value = "Reconnect failed: ${e.localizedMessage ?: "Server unreachable"}"
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        sendLine("LEAVE_ROOM")
        closeSocketOnly()
        _activeRoomFlow.value = null
        _worldStateFlow.value = null
        _connectionFlow.value = ConnectionState.OFFLINE_LOCAL_PROTOTYPE
    }

    fun shutdown() {
        closeSocketOnly()
        scope.cancel()
    }

    override fun sendSnapshot(payload: ByteArray) {
        sendLine(payload.toString(Charsets.UTF_8))
    }

    override suspend fun createRoom(privacy: RoomPrivacy, districtId: String): Result<RoomSessionInfo> {
        return createRoom(privacy, GameModeId.FOUR_PLAYER_SQUAD, districtId, 4)
    }

    fun createRoom(privacy: RoomPrivacy, mode: GameModeId, districtId: String, maxPlayers: Int): Result<RoomSessionInfo> {
        sendLine("CREATE_ROOM|${privacy.id}|${mode.id}|$districtId|$maxPlayers")
        return _activeRoomFlow.value?.let { Result.success(it) } ?: Result.success(
            RoomSessionInfo("pending", "PENDING", privacy, mode, districtId, maxPlayers)
        )
    }

    override suspend fun joinRoomByCode(roomCode: String): Result<RoomSessionInfo> {
        if (!InputSanitizer.isValidRoomCode(roomCode)) {
            _errorNoticeFlow.value = "Invalid room code format."
            return Result.failure(IllegalArgumentException("Invalid room code"))
        }
        sendLine("JOIN_ROOM|${roomCode.trim().uppercase()}")
        return _activeRoomFlow.value?.let { Result.success(it) } ?: Result.success(
            RoomSessionInfo("joining", roomCode.uppercase(), RoomPrivacy.PRIVATE_CODE)
        )
    }

    override suspend fun findPublicMatch(districtId: String, squadSize: Int): Result<RoomSessionInfo> {
        val mode = when (squadSize) {
            1 -> GameModeId.SOLO
            2 -> GameModeId.DUO
            else -> GameModeId.FOUR_PLAYER_SQUAD
        }
        sendLine("QUICK_MATCH|${mode.id}|$districtId|$squadSize")
        return _activeRoomFlow.value?.let { Result.success(it) } ?: Result.success(
            RoomSessionInfo("matching", "QUICK", RoomPrivacy.PUBLIC, mode, districtId, squadSize)
        )
    }

    fun requestRoomList() = sendLine("LIST_ROOMS")
    fun startQuickMatch(mode: GameModeId, districtId: String, maxPlayers: Int) =
        sendLine("QUICK_MATCH|${mode.id}|$districtId|$maxPlayers")

    override suspend fun setLocalReady(ready: Boolean) = sendLine("SET_READY|$ready")
    fun setTeam(team: TeamSide) = sendLine("SET_TEAM|${team.id}")
    fun setCharacterAndWeapon(characterId: String, weaponId: String) = sendLine("SET_CHAR|$characterId|$weaponId")
    fun startMatch() = sendLine("START_MATCH")
    fun sendMove(x: Float, y: Float, z: Float, yawRad: Float, weaponId: String) =
        sendLine(
            "MOVE|${String.format(Locale.US, "%.2f", x)}|${String.format(Locale.US, "%.2f", y)}|${String.format(Locale.US, "%.2f", z)}|${String.format(Locale.US, "%.2f", yawRad)}|$weaponId"
        )
    fun sendFire(dirX: Float, dirY: Float, dirZ: Float) =
        sendLine(
            "FIRE|${String.format(Locale.US, "%.3f", dirX)}|${String.format(Locale.US, "%.3f", dirY)}|${String.format(Locale.US, "%.3f", dirZ)}"
        )
    fun sendReload() = sendLine("RELOAD")
    fun sendChat(channel: ChatChannel, recipientId: String = "", text: String) {
        val safe = InputSanitizer.sanitizeChatMessage(text)
        if (safe.isNotBlank()) sendLine("CHAT|${channel.id}|$recipientId|$safe")
    }
    fun sendInvalidRewardTamperForTest() = sendLine("CLAIM_REWARD|999999|999999")
    fun clearError() { _errorNoticeFlow.value = null }
    fun clearReward() { _verifiedRewardFlow.value = null }

    override suspend fun leaveRoom() {
        sendLine("LEAVE_ROOM")
        _activeRoomFlow.value = null
        _worldStateFlow.value = null
    }

    @Synchronized
    private fun sendLine(line: String) {
        scope.launch {
            try {
                writer?.write(line)
                writer?.write("\n")
                writer?.flush()
            } catch (_: Exception) {
                if (_connectionFlow.value == ConnectionState.IN_ROOM) {
                    _connectionFlow.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    private suspend fun readLoop(sock: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
            while (true) {
                val line = reader.readLine() ?: break
                handleServerPacket(line)
            }
        } catch (_: Exception) {
        } finally {
            if (socket === sock && _connectionFlow.value == ConnectionState.IN_ROOM) {
                _connectionFlow.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun handleServerPacket(line: String) {
        val parts = line.split("|")
        when (parts.getOrNull(0)) {
            "WELCOME" -> {
                localPlayerId = parts.getOrNull(1).orEmpty()
                sessionToken = parts.getOrNull(2).orEmpty()
                _connectionFlow.value = ConnectionState.IN_ROOM
            }
            "ROOMS" -> {
                val body = parts.getOrNull(2).orEmpty()
                if (body.isBlank()) {
                    _publicRoomsFlow.value = emptyList()
                    return
                }
                val parsed = body.split(";").mapNotNull { entry ->
                    val cols = entry.split(",")
                    if (cols.size < 8) null
                    else PublicRoomSummary(
                        roomId = cols[0],
                        roomCode = cols[1],
                        hostName = cols[2],
                        mode = GameModeId.entries.firstOrNull { it.id == cols[3] } ?: GameModeId.FOUR_PLAYER_SQUAD,
                        districtId = cols[4],
                        playerCount = cols[5].toIntOrNull() ?: 1,
                        maxPlayers = cols[6].toIntOrNull() ?: 4,
                        pingMs = 4,
                        privacy = if (cols[7] == RoomPrivacy.PRIVATE_CODE.id) RoomPrivacy.PRIVATE_CODE else RoomPrivacy.PUBLIC
                    )
                }
                _publicRoomsFlow.value = parsed
            }
            "ROOM_STATE" -> {
                if (parts.size < 9) return
                val roomId = parts[1]
                val roomCode = parts[2]
                val privacy = if (parts[3] == RoomPrivacy.PRIVATE_CODE.id) RoomPrivacy.PRIVATE_CODE else RoomPrivacy.PUBLIC
                val mode = GameModeId.entries.firstOrNull { it.id == parts[4] } ?: GameModeId.FOUR_PLAYER_SQUAD
                val districtId = parts[5]
                val maxP = parts[6].toIntOrNull() ?: 4
                val phase = MatchPhase.fromId(parts[7])
                val slotsStr = parts.getOrNull(9).orEmpty()
                val slots = if (slotsStr.isBlank()) emptyList() else slotsStr.split(";").mapNotNull { row ->
                    val c = row.split(",")
                    if (c.size < 9) null
                    else NetworkPlayerSlot(
                        playerId = c[0],
                        callsign = c[1],
                        characterId = c[2],
                        team = TeamSide.fromId(c[3]),
                        isReady = c[4].toBooleanStrictOrNull() ?: false,
                        isHost = c[5].toBooleanStrictOrNull() ?: false,
                        isBot = c[6].toBooleanStrictOrNull() ?: false,
                        isConnected = c[7].toBooleanStrictOrNull() ?: true,
                        avatarColorHex = c[8].toLongOrNull(16) ?: 0xFF10B981L
                    )
                }
                _activeRoomFlow.value = RoomSessionInfo(
                    roomId = roomId,
                    roomCode = roomCode,
                    privacy = privacy,
                    mode = mode,
                    districtId = districtId,
                    maxPlayers = maxP,
                    phase = phase,
                    players = slots,
                    endpointLabel = "127.0.0.1:$lastPort (Local Authoritative Test Mode)"
                )
            }
            "WORLD" -> {
                if (parts.size < 7) return
                val roomId = parts[1]
                val tick = parts[2].toLongOrNull() ?: 0L
                val phase = MatchPhase.fromId(parts[3])
                val remSec = parts[4].toIntOrNull() ?: 0
                val winner = parts[5].ifBlank { null }
                val rowsStr = parts.getOrNull(7).orEmpty()
                val players = if (rowsStr.isBlank()) emptyList() else rowsStr.split(";").mapNotNull { row ->
                    val c = row.split(",")
                    if (c.size < 14) null
                    else RemotePlayerSnapshot(
                        playerId = c[0],
                        callsign = c[1],
                        characterId = c[2],
                        team = TeamSide.fromId(c[3]),
                        x = c[4].toFloatOrNull() ?: 0f,
                        y = c[5].toFloatOrNull() ?: 0f,
                        z = c[6].toFloatOrNull() ?: 0f,
                        yawRad = c[7].toFloatOrNull() ?: 0f,
                        health = c[8].toIntOrNull() ?: 100,
                        weaponId = c[9],
                        ammoInMag = c[10].toIntOrNull() ?: 30,
                        kills = c[11].toIntOrNull() ?: 0,
                        isConnected = c[12].toBooleanStrictOrNull() ?: true,
                        isAlive = c[13].toBooleanStrictOrNull() ?: true
                    )
                }
                _worldStateFlow.value = WorldStateSnapshot(roomId, tick, phase, remSec, winner, players)
            }
            "CHAT" -> {
                if (parts.size < 9) return
                val msg = ChatMessage(
                    id = parts[1],
                    channel = ChatChannel.fromId(parts[2]),
                    senderId = parts[3],
                    senderName = parts[4],
                    team = TeamSide.fromId(parts[5]),
                    recipientId = parts[6].ifBlank { null },
                    text = parts[7],
                    timestampMs = parts[8].toLongOrNull() ?: System.currentTimeMillis()
                )
                _chatMessagesFlow.value = (_chatMessagesFlow.value + msg).takeLast(60)
            }
            "REWARD" -> {
                val xp = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val taka = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val dist = parts.getOrNull(3).orEmpty()
                _verifiedRewardFlow.value = VerifiedMatchReward(xp, taka, dist)
            }
            "ERROR" -> {
                _errorNoticeFlow.value = parts.getOrNull(2) ?: "Server rejected action."
            }
        }
    }

    private fun closeSocketOnly() {
        readJob?.cancel()
        readJob = null
        try { writer?.close() } catch (_: Exception) {}
        writer = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}
