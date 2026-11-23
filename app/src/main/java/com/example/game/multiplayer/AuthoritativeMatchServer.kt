package com.example.game.multiplayer

import com.example.game.characters.CharacterCatalog
import com.example.game.inventory.InventoryCatalog
import com.example.game.modes.GameModeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real loopback TCP server-authoritative multiplayer engine.
 * Owns canonical room membership, movement bounds/speed validation, weapon damage, ammo,
 * match outcome, XP rewards, rate limiting, and security audit logs.
 */
class AuthoritativeMatchServer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var tickJob: Job? = null

    val port: Int
        get() = serverSocket?.localPort ?: -1

    @Volatile
    var isRunning: Boolean = false
        private set

    private data class ClientConnection(
        val socket: Socket,
        val writer: BufferedWriter,
        var playerId: String = "",
        var username: String = "",
        var sessionToken: String = "",
        var roomId: String? = null
    ) {
        @Synchronized
        fun sendLine(line: String) {
            try {
                writer.write(line)
                writer.write("\n")
                writer.flush()
            } catch (_: Exception) {
            }
        }
    }

    private data class ServerPlayerState(
        val playerId: String,
        var callsign: String,
        var characterId: String,
        var team: TeamSide,
        var isReady: Boolean,
        var isHost: Boolean,
        var isBot: Boolean,
        var isConnected: Boolean,
        var sessionToken: String,
        var x: Float,
        var y: Float,
        var z: Float,
        var yawRad: Float,
        var hp: Int = 100,
        var weaponId: String = "ar_dhaka_m4",
        var ammoInMag: Int = 30,
        var kills: Int = 0,
        var lastMoveAtMs: Long = System.currentTimeMillis(),
        var lastFireAtMs: Long = 0L,
        var lastReloadAtMs: Long = 0L
    ) {
        val isAlive: Boolean get() = hp > 0
    }

    private data class ServerRoom(
        val roomId: String,
        val roomCode: String,
        val privacy: RoomPrivacy,
        var mode: GameModeId,
        var districtId: String,
        var maxPlayers: Int,
        var phase: MatchPhase = MatchPhase.WAITING,
        var remainingSec: Int = 120,
        var tickCounter: Long = 0L,
        var winningTeam: String = "",
        var rewardsDispatched: Boolean = false,
        val players: MutableMap<String, ServerPlayerState> = ConcurrentHashMap()
    )

    private val connections = CopyOnWriteArrayList<ClientConnection>()
    private val rooms = ConcurrentHashMap<String, ServerRoom>()
    private val chatLimiter = FixedWindowRateLimiter(600L)
    private val fireLimiter = FixedWindowRateLimiter(55L)
    val auditLogs = CopyOnWriteArrayList<ServerAuditLogEntry>()

    fun start(): Int {
        if (isRunning) return port
        val ss = ServerSocket(0, 32, InetAddress.getByName("127.0.0.1"))
        serverSocket = ss
        isRunning = true

        // Seed two discoverable public rooms for matchmaking/browser testing
        seedPublicRoomIfNeeded()

        acceptJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    val clientSock = ss.accept()
                    val writer = BufferedWriter(OutputStreamWriter(clientSock.getOutputStream(), Charsets.UTF_8))
                    val conn = ClientConnection(clientSock, writer)
                    connections.add(conn)
                    launch { handleClientLoop(conn) }
                } catch (_: Exception) {
                    if (!isRunning) break
                }
            }
        }

        tickJob = scope.launch {
            while (isActive && isRunning) {
                stepServerTick(0.05f)
                delay(50L) // 20 Hz server tick
            }
        }

        return ss.localPort
    }

    fun stop() {
        isRunning = false
        acceptJob?.cancel()
        tickJob?.cancel()
        connections.forEach { conn ->
            try { conn.socket.close() } catch (_: Exception) {}
        }
        connections.clear()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        scope.cancel()
    }

    private fun seedPublicRoomIfNeeded() {
        if (rooms.isNotEmpty()) return
        val r1 = ServerRoom(
            roomId = "room_dhaka_squad_01",
            roomCode = "BD401",
            privacy = RoomPrivacy.PUBLIC,
            mode = GameModeId.FOUR_PLAYER_SQUAD,
            districtId = "dhaka",
            maxPlayers = 4
        )
        r1.players["bot_host_1"] = ServerPlayerState(
            playerId = "bot_host_1",
            callsign = "DhakaLeader",
            characterId = "hero_alam",
            team = TeamSide.ALPHA,
            isReady = true,
            isHost = true,
            isBot = true,
            isConnected = true,
            sessionToken = "bot",
            x = -4f, y = 0f, z = 10f, yawRad = 0f
        )
        val r2 = ServerRoom(
            roomId = "room_dhaka_duo_02",
            roomCode = "BD202",
            privacy = RoomPrivacy.PUBLIC,
            mode = GameModeId.DUO,
            districtId = "dhaka",
            maxPlayers = 2
        )
        r2.players["bot_host_2"] = ServerPlayerState(
            playerId = "bot_host_2",
            callsign = "ChawkRanger",
            characterId = "khairul_dewan",
            team = TeamSide.ALPHA,
            isReady = true,
            isHost = true,
            isBot = true,
            isConnected = true,
            sessionToken = "bot",
            x = 4f, y = 0f, z = 10f, yawRad = 0f
        )
        rooms[r1.roomId] = r1
        rooms[r2.roomId] = r2
    }

    private fun handleClientLoop(conn: ClientConnection) {
        try {
            val reader = BufferedReader(InputStreamReader(conn.socket.getInputStream(), Charsets.UTF_8))
            while (isRunning) {
                val line = reader.readLine() ?: break
                processCommand(conn, line)
            }
        } catch (_: Exception) {
        } finally {
            onClientDisconnected(conn)
        }
    }

    private fun onClientDisconnected(conn: ClientConnection) {
        connections.remove(conn)
        val rId = conn.roomId ?: return
        val room = rooms[rId] ?: return
        val pState = room.players[conn.playerId] ?: return
        pState.isConnected = false
        logAudit("DISCONNECT", conn.playerId, "Disconnected from room ${room.roomCode}; session held for reconnect.")
        broadcastRoomState(room)
        broadcastSystemChat(room, "${pState.callsign} disconnected (reconnect window open).")
        try { conn.socket.close() } catch (_: Exception) {}
    }

    private fun processCommand(conn: ClientConnection, rawLine: String) {
        if (rawLine.length > 512) {
            logAudit("SECURITY_OVERSIZE", conn.playerId, "Rejected oversized packet (${rawLine.length} bytes)")
            return
        }
        val parts = rawLine.split("|")
        if (parts.isEmpty()) return
        when (parts[0]) {
            "HELLO" -> {
                val pid = cleanField(parts.getOrNull(1).orEmpty()).ifBlank { "p_${UUID.randomUUID().toString().take(6)}" }
                val uname = cleanField(parts.getOrNull(2).orEmpty()).ifBlank { "Warrior" }.take(20)
                val token = UUID.randomUUID().toString().replace("-", "").take(12)
                conn.playerId = pid
                conn.username = uname
                conn.sessionToken = token
                conn.sendLine("WELCOME|$pid|$token")
                sendRoomList(conn)
            }
            "LIST_ROOMS" -> sendRoomList(conn)
            "CREATE_ROOM" -> {
                if (conn.playerId.isBlank()) {
                    conn.sendLine("ERROR|AUTH|Authenticate via HELLO before creating a room.")
                    return
                }
                val privacy = if (parts.getOrNull(1) == RoomPrivacy.PRIVATE_CODE.id) RoomPrivacy.PRIVATE_CODE else RoomPrivacy.PUBLIC
                val mode = GameModeId.entries.firstOrNull { it.id == parts.getOrNull(2) } ?: GameModeId.FOUR_PLAYER_SQUAD
                val districtId = cleanField(parts.getOrNull(3).orEmpty()).ifBlank { "dhaka" }
                val maxP = (parts.getOrNull(4)?.toIntOrNull() ?: mode.defaultMaxPlayers).coerceIn(1, 4)
                val code = "BD" + (1000 + (Math.abs(UUID.randomUUID().hashCode()) % 8999))
                val room = ServerRoom(
                    roomId = "room_${UUID.randomUUID().toString().take(8)}",
                    roomCode = code,
                    privacy = privacy,
                    mode = mode,
                    districtId = districtId,
                    maxPlayers = maxP
                )
                rooms[room.roomId] = room
                attachPlayerToRoom(conn, room, asHost = true, charId = null, weaponId = null)
            }
            "JOIN_ROOM" -> {
                val query = cleanField(parts.getOrNull(1).orEmpty()).uppercase()
                val room = rooms.values.firstOrNull { it.roomId.equals(query, true) || it.roomCode.equals(query, true) }
                if (room == null) {
                    conn.sendLine("ERROR|ROOM_NOT_FOUND|Room code $query was not found.")
                    return
                }
                if (room.players.size >= room.maxPlayers && !room.players.containsKey(conn.playerId)) {
                    conn.sendLine("ERROR|ROOM_FULL|Room ${room.roomCode} is full (${room.maxPlayers}/${room.maxPlayers}).")
                    return
                }
                attachPlayerToRoom(conn, room, asHost = false, charId = parts.getOrNull(2), weaponId = parts.getOrNull(3))
            }
            "QUICK_MATCH" -> {
                val mode = GameModeId.entries.firstOrNull { it.id == parts.getOrNull(1) } ?: GameModeId.FOUR_PLAYER_SQUAD
                val districtId = cleanField(parts.getOrNull(2).orEmpty()).ifBlank { "dhaka" }
                val maxP = (parts.getOrNull(3)?.toIntOrNull() ?: mode.defaultMaxPlayers).coerceIn(1, 4)
                val existing = rooms.values.firstOrNull {
                    it.privacy == RoomPrivacy.PUBLIC &&
                        it.phase == MatchPhase.WAITING &&
                        it.mode == mode &&
                        it.players.size < it.maxPlayers
                }
                if (existing != null) {
                    attachPlayerToRoom(conn, existing, asHost = false, charId = null, weaponId = null)
                } else {
                    val code = "BD" + (1000 + (Math.abs(UUID.randomUUID().hashCode()) % 8999))
                    val room = ServerRoom(
                        roomId = "room_${UUID.randomUUID().toString().take(8)}",
                        roomCode = code,
                        privacy = RoomPrivacy.PUBLIC,
                        mode = mode,
                        districtId = districtId,
                        maxPlayers = maxP
                    )
                    rooms[room.roomId] = room
                    attachPlayerToRoom(conn, room, asHost = true, charId = null, weaponId = null)
                }
            }
            "RECONNECT" -> {
                val pid = cleanField(parts.getOrNull(1).orEmpty())
                val roomIdOrCode = cleanField(parts.getOrNull(2).orEmpty())
                val token = cleanField(parts.getOrNull(3).orEmpty())
                val room = rooms.values.firstOrNull { it.roomId == roomIdOrCode || it.roomCode.equals(roomIdOrCode, true) }
                val existing = room?.players?.get(pid)
                if (room == null || existing == null || existing.sessionToken != token) {
                    conn.sendLine("ERROR|RECONNECT_FAILED|Session expired or invalid reconnect token.")
                    return
                }
                conn.playerId = pid
                conn.username = existing.callsign
                conn.sessionToken = token
                conn.roomId = room.roomId
                existing.isConnected = true
                conn.sendLine("WELCOME|$pid|$token")
                broadcastRoomState(room)
                broadcastWorldState(room)
                broadcastSystemChat(room, "${existing.callsign} reconnected cleanly.")
                logAudit("RECONNECT", pid, "Session recovered in ${room.roomCode}")
            }
            "SET_READY" -> {
                val room = currentRoom(conn) ?: return
                val p = room.players[conn.playerId] ?: return
                p.isReady = parts.getOrNull(1) == "true"
                broadcastRoomState(room)
            }
            "SET_TEAM" -> {
                val room = currentRoom(conn) ?: return
                val p = room.players[conn.playerId] ?: return
                p.team = TeamSide.fromId(parts.getOrNull(1).orEmpty())
                broadcastRoomState(room)
            }
            "SET_CHAR" -> {
                val room = currentRoom(conn) ?: return
                val p = room.players[conn.playerId] ?: return
                val charId = cleanField(parts.getOrNull(1).orEmpty())
                val wepId = cleanField(parts.getOrNull(2).orEmpty())
                p.characterId = CharacterCatalog.getById(charId).id
                if (wepId.isNotBlank()) {
                    val w = InventoryCatalog.getWeapon(wepId)
                    p.weaponId = w.id
                    p.ammoInMag = w.magSize
                }
                broadcastRoomState(room)
            }
            "START_MATCH" -> {
                val room = currentRoom(conn) ?: return
                val caller = room.players[conn.playerId]
                if (caller == null || !caller.isHost) {
                    conn.sendLine("ERROR|HOST_ONLY|Only room host can start the match.")
                    return
                }
                if (room.players.values.any { !it.isReady && !it.isBot }) {
                    conn.sendLine("ERROR|NOT_READY|All human players must be ready before starting.")
                    return
                }
                room.phase = MatchPhase.IN_MATCH
                room.remainingSec = 120
                room.tickCounter = 0L
                room.winningTeam = ""
                room.rewardsDispatched = false
                val spawns = listOf(
                    Vec3Pair(-3.5f, 10.5f),
                    Vec3Pair(3.5f, 10.5f),
                    Vec3Pair(-3.5f, -10.5f),
                    Vec3Pair(3.5f, -10.5f)
                )
                val nowMs = System.currentTimeMillis()
                room.players.values.sortedBy { it.playerId }.forEachIndexed { idx, sp ->
                    val spawn = spawns[idx % spawns.size]
                    sp.x = spawn.x
                    sp.z = spawn.z
                    sp.y = 0f
                    sp.hp = 100
                    sp.kills = 0
                    sp.lastMoveAtMs = nowMs
                    val w = InventoryCatalog.getWeapon(sp.weaponId)
                    sp.ammoInMag = w.magSize
                }
                broadcastRoomState(room)
                broadcastWorldState(room)
                broadcastSystemChat(room, "Match started on ${room.districtId.uppercase()} (${room.mode.titleEn}).")
            }
            "MOVE" -> {
                val room = currentRoom(conn) ?: return
                val p = room.players[conn.playerId] ?: return
                if (!p.isAlive || room.phase != MatchPhase.IN_MATCH) return
                val rx = parts.getOrNull(1)?.toFloatOrNull() ?: return
                val ry = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
                val rz = parts.getOrNull(3)?.toFloatOrNull() ?: return
                val yaw = parts.getOrNull(4)?.toFloatOrNull() ?: p.yawRad
                val now = System.currentTimeMillis()
                val dt = ((now - p.lastMoveAtMs) / 1000f).coerceIn(0.01f, 0.25f)
                p.lastMoveAtMs = now
                val (safeX, safeZ) = ServerAuthorityValidator.validateStepMovement(p.x, p.z, rx, rz, dt)
                if (kotlin.math.hypot((rx - safeX).toDouble(), (rz - safeZ).toDouble()) > 1.5) {
                    logAudit("SPEED_ANOMALY", p.playerId, "Clamped teleport/speed delta from ($rx,$rz) to ($safeX,$safeZ)")
                }
                p.x = safeX
                p.y = ry.coerceIn(-0.6f, 3.0f)
                p.z = safeZ
                p.yawRad = yaw
            }
            "FIRE" -> {
                val room = currentRoom(conn) ?: return
                val shooter = room.players[conn.playerId] ?: return
                if (!shooter.isAlive || room.phase != MatchPhase.IN_MATCH) return
                if (!fireLimiter.allow(shooter.playerId)) return
                val weapon = InventoryCatalog.getWeapon(shooter.weaponId)
                val now = System.currentTimeMillis()
                if (now - shooter.lastFireAtMs < (weapon.fireCooldownMs * 0.8)) {
                    logAudit("RATE_LIMIT", shooter.playerId, "Fire rate clamped for ${weapon.id}")
                    return
                }
                if (!weapon.isMelee && shooter.ammoInMag <= 0) return
                shooter.lastFireAtMs = now
                if (!weapon.isMelee) shooter.ammoInMag -= 1

                val dx = parts.getOrNull(1)?.toFloatOrNull() ?: sin(shooter.yawRad)
                val dz = parts.getOrNull(3)?.toFloatOrNull() ?: -cos(shooter.yawRad)
                val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
                val nx = dx / len
                val nz = dz / len

                // Server-authoritative raycast hit calculation (NEVER trusts client damage)
                var bestTarget: ServerPlayerState? = null
                var bestDist = weapon.rangeMeters
                for (target in room.players.values) {
                    if (target.playerId == shooter.playerId || !target.isAlive) continue
                    if (room.mode.supportsTeams && target.team == shooter.team) continue
                    val vx = target.x - shooter.x
                    val vz = target.z - shooter.z
                    val proj = vx * nx + vz * nz
                    if (proj <= 0f || proj > bestDist) continue
                    val perpSq = (vx * vx + vz * vz) - (proj * proj)
                    if (perpSq <= 0.75f * 0.75f) {
                        bestDist = proj
                        bestTarget = target
                    }
                }
                if (bestTarget != null) {
                    bestTarget.hp = (bestTarget.hp - weapon.damage).coerceAtLeast(0)
                    if (bestTarget.hp == 0) {
                        shooter.kills += 1
                        broadcastSystemChat(room, "${shooter.callsign} eliminated ${bestTarget.callsign} (${weapon.nameEn})!")
                        checkMatchVictory(room)
                    }
                }
            }
            "RELOAD" -> {
                val room = currentRoom(conn) ?: return
                val p = room.players[conn.playerId] ?: return
                val weapon = InventoryCatalog.getWeapon(p.weaponId)
                p.ammoInMag = weapon.magSize
                p.lastReloadAtMs = System.currentTimeMillis()
            }
            "CHAT" -> {
                val room = currentRoom(conn) ?: return
                val sender = room.players[conn.playerId] ?: return
                if (!chatLimiter.allow(sender.playerId)) {
                    conn.sendLine("ERROR|RATE_LIMIT|Slow down chat messages.")
                    return
                }
                val channel = ChatChannel.fromId(parts.getOrNull(1).orEmpty())
                val recipientId = cleanField(parts.getOrNull(2).orEmpty())
                val rawText = parts.getOrNull(3).orEmpty()
                if (InputSanitizer.containsBlockedContent(rawText)) {
                    logAudit("MODERATION", sender.playerId, "Filtered blocked chat term.")
                }
                val cleanText = InputSanitizer.sanitizeChatMessage(rawText)
                if (cleanText.isBlank()) return
                val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
                val ts = System.currentTimeMillis()
                val packet = "CHAT|$msgId|${channel.id}|${sender.playerId}|${sender.callsign}|${sender.team.id}|$recipientId|$cleanText|$ts"
                connections.filter { it.roomId == room.roomId }.forEach { targetConn ->
                    val recipientPlayer = room.players[targetConn.playerId]
                    val allowed = when (channel) {
                        ChatChannel.TEAM -> recipientPlayer?.team == sender.team
                        ChatChannel.PRIVATE -> targetConn.playerId == recipientId || targetConn.playerId == sender.playerId
                        else -> true
                    }
                    if (allowed) targetConn.sendLine(packet)
                }
            }
            "CLAIM_REWARD" -> {
                // Server rejects client-supplied XP/Taka claims and logs tamper attempts
                logAudit("ANTICHEAT_TAMPER", conn.playerId, "Blocked unauthorized client reward claim.")
                conn.sendLine("ERROR|ANTICHEAT|Rewards are strictly server-validated.")
            }
            "LEAVE_ROOM" -> {
                val room = currentRoom(conn) ?: return
                room.players.remove(conn.playerId)
                conn.roomId = null
                if (room.players.values.none { !it.isBot } && !room.roomId.startsWith("room_dhaka_")) {
                    rooms.remove(room.roomId)
                } else {
                    if (room.players.values.none { it.isHost } && room.players.isNotEmpty()) {
                        room.players.values.first().isHost = true
                    }
                    broadcastRoomState(room)
                }
            }
            "PING" -> conn.sendLine("PONG|${parts.getOrNull(1).orEmpty()}")
        }
    }

    private data class Vec3Pair(val x: Float, val z: Float)

    private fun attachPlayerToRoom(
        conn: ClientConnection,
        room: ServerRoom,
        asHost: Boolean,
        charId: String?,
        weaponId: String?
    ) {
        conn.roomId = room.roomId
        val slotIdx = room.players.size
        val defaultChar = CharacterCatalog.slots[slotIdx % CharacterCatalog.slots.size].id
        val chosenChar = if (!charId.isNullOrBlank()) CharacterCatalog.getById(charId).id else defaultChar
        val team = if (room.mode.supportsTeams && slotIdx % 2 == 1) TeamSide.BRAVO else TeamSide.ALPHA
        val state = room.players[conn.playerId] ?: ServerPlayerState(
            playerId = conn.playerId,
            callsign = conn.username.ifBlank { "Warrior-${slotIdx + 1}" },
            characterId = chosenChar,
            team = team,
            isReady = asHost,
            isHost = asHost || room.players.isEmpty(),
            isBot = false,
            isConnected = true,
            sessionToken = conn.sessionToken,
            x = -3f + slotIdx * 2f,
            y = 0f,
            z = 10f,
            yawRad = 0f,
            weaponId = weaponId?.takeIf { it.isNotBlank() } ?: "ar_dhaka_m4"
        )
        state.isConnected = true
        room.players[conn.playerId] = state
        broadcastRoomState(room)
        broadcastSystemChat(room, "${state.callsign} joined room ${room.roomCode}.")
    }

    private fun stepServerTick(dt: Float) {
        for (room in rooms.values) {
            if (room.phase != MatchPhase.IN_MATCH) continue
            room.tickCounter += 1L
            if (room.tickCounter % 20L == 0L && room.remainingSec > 0) {
                room.remainingSec -= 1
                if (room.remainingSec <= 0) {
                    finishRoomByTimer(room)
                }
            }
            // Gently animate server-hosted test bots if present in room
            for (p in room.players.values) {
                if (!p.isBot || !p.isAlive) continue
                val angle = room.tickCounter * 0.04f + p.playerId.hashCode()
                val nx = (p.x + cos(angle) * 1.4f * dt).coerceIn(-18f, 18f)
                val nz = (p.z + sin(angle) * 1.4f * dt).coerceIn(-18f, 18f)
                p.x = nx
                p.z = nz
                p.yawRad = (angle % (2f * PI.toFloat()))
            }
            if (room.tickCounter % 2L == 0L) {
                broadcastWorldState(room)
            }
        }
    }

    private fun checkMatchVictory(room: ServerRoom) {
        if (room.phase != MatchPhase.IN_MATCH || room.players.size < 2) return
        if (room.mode.supportsTeams) {
            val alphaAlive = room.players.values.any { it.team == TeamSide.ALPHA && it.isAlive }
            val bravoAlive = room.players.values.any { it.team == TeamSide.BRAVO && it.isAlive }
            if (!alphaAlive || !bravoAlive) {
                room.winningTeam = if (alphaAlive) TeamSide.ALPHA.id else TeamSide.BRAVO.id
                finishMatch(room)
            }
        } else if (room.mode == GameModeId.SURVIVAL_BATTLE) {
            val alive = room.players.values.filter { it.isAlive }
            if (alive.size <= 1) {
                room.winningTeam = alive.firstOrNull()?.callsign ?: TeamSide.ALPHA.id
                finishMatch(room)
            }
        }
    }

    private fun finishRoomByTimer(room: ServerRoom) {
        room.winningTeam = TeamSide.ALPHA.id
        finishMatch(room)
    }

    private fun finishMatch(room: ServerRoom) {
        room.phase = MatchPhase.FINISHED
        if (!room.rewardsDispatched) {
            room.rewardsDispatched = true
            connections.filter { it.roomId == room.roomId }.forEach { c ->
                val sp = room.players[c.playerId]
                val won = sp != null && (sp.team.id == room.winningTeam || sp.callsign == room.winningTeam)
                val (xp, taka) = ServerAuthorityValidator.clampVerifiedMatchReward(
                    xp = 240 + (sp?.kills ?: 0) * 40,
                    taka = 600 + (sp?.kills ?: 0) * 100,
                    won = won
                )
                c.sendLine("REWARD|$xp|$taka|gazipur")
            }
        }
        broadcastRoomState(room)
        broadcastWorldState(room)
    }

    private fun currentRoom(conn: ClientConnection): ServerRoom? = conn.roomId?.let { rooms[it] }

    private fun sendRoomList(conn: ClientConnection) {
        val pubs = rooms.values.filter { it.privacy == RoomPrivacy.PUBLIC }
        val body = pubs.joinToString(";") { r ->
            val host = r.players.values.firstOrNull { it.isHost }?.callsign ?: "Host"
            "${r.roomId},${r.roomCode},${cleanField(host)},${r.mode.id},${r.districtId},${r.players.size},${r.maxPlayers},${r.privacy.id}"
        }
        conn.sendLine("ROOMS|${pubs.size}|$body")
    }

    private fun broadcastRoomState(room: ServerRoom) {
        val slots = room.players.values.joinToString(";") { sp ->
            val avatarHex = when (sp.team) {
                TeamSide.ALPHA -> "FF10B981"
                TeamSide.BRAVO -> "FFEF4444"
                TeamSide.SOLO -> "FFF59E0B"
            }
            "${sp.playerId},${cleanField(sp.callsign)},${sp.characterId},${sp.team.id},${sp.isReady},${sp.isHost},${sp.isBot},${sp.isConnected},$avatarHex"
        }
        val msg = "ROOM_STATE|${room.roomId}|${room.roomCode}|${room.privacy.id}|${room.mode.id}|${room.districtId}|${room.maxPlayers}|${room.phase.id}|${room.players.size}|$slots"
        connections.filter { it.roomId == room.roomId }.forEach { it.sendLine(msg) }
    }

    private fun broadcastWorldState(room: ServerRoom) {
        val states = room.players.values.joinToString(";") { sp ->
            val fx = String.format(Locale.US, "%.2f", sp.x)
            val fy = String.format(Locale.US, "%.2f", sp.y)
            val fz = String.format(Locale.US, "%.2f", sp.z)
            val fyaw = String.format(Locale.US, "%.2f", sp.yawRad)
            "${sp.playerId},${cleanField(sp.callsign)},${sp.characterId},${sp.team.id},$fx,$fy,$fz,$fyaw,${sp.hp},${sp.weaponId},${sp.ammoInMag},${sp.kills},${sp.isConnected},${sp.isAlive}"
        }
        val msg = "WORLD|${room.roomId}|${room.tickCounter}|${room.phase.id}|${room.remainingSec}|${room.winningTeam}|${room.players.size}|$states"
        connections.filter { it.roomId == room.roomId }.forEach { it.sendLine(msg) }
    }

    private fun broadcastSystemChat(room: ServerRoom, text: String) {
        val msg = "CHAT|sys_${System.nanoTime()}|${ChatChannel.SYSTEM.id}|system|SYSTEM|${TeamSide.SOLO.id}||${cleanField(text)}|${System.currentTimeMillis()}"
        connections.filter { it.roomId == room.roomId }.forEach { it.sendLine(msg) }
    }

    private fun logAudit(category: String, playerId: String, detail: String) {
        auditLogs.add(ServerAuditLogEntry(System.currentTimeMillis(), category, playerId, detail))
        if (auditLogs.size > 100) auditLogs.removeAt(0)
    }

    private fun cleanField(raw: String): String =
        raw.replace("|", "").replace(";", "").replace(",", "").replace("\n", " ").trim()
}
