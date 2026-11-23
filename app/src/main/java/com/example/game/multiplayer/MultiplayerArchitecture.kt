package com.example.game.multiplayer

import com.example.game.localization.AppLanguage
import com.example.game.modes.GameModeId

enum class RoomPrivacy(val id: String, val labelEn: String, val labelBn: String) {
    PUBLIC("public", "Public Room", "পাবলিক রুম"),
    PRIVATE_CODE("private", "Private Room", "প্রাইভেট রুম");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

enum class TeamSide(val id: String, val labelEn: String, val labelBn: String) {
    ALPHA("ALPHA", "Team Alpha", "টিম আলফা"),
    BRAVO("BRAVO", "Team Bravo", "টিম ব্রাভো"),
    SOLO("SOLO", "Solo / Free", "সোলো / মুক্ত");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(raw: String): TeamSide = entries.firstOrNull { it.id == raw } ?: ALPHA
    }
}

enum class MatchPhase(val id: String) {
    WAITING("WAITING"),
    IN_MATCH("IN_MATCH"),
    FINISHED("FINISHED");

    companion object {
        fun fromId(raw: String): MatchPhase = entries.firstOrNull { it.id == raw } ?: WAITING
    }
}

enum class ConnectionState(val labelEn: String, val labelBn: String) {
    OFFLINE_LOCAL_PROTOTYPE("Local Mode", "লোকাল মোড"),
    CONNECTING("Connecting...", "সংযুক্ত হচ্ছে..."),
    IN_ROOM("Connected (Local Test Server)", "সংযুক্ত (লোকাল টেস্ট সার্ভার)"),
    DISCONNECTED("Disconnected", "বিচ্ছিন্ন"),
    RECONNECTING("Reconnecting...", "পুনঃসংযোগ চলছে...");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

data class NetworkPlayerSlot(
    val playerId: String,
    val callsign: String,
    val characterId: String,
    val team: TeamSide = TeamSide.ALPHA,
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val isBot: Boolean = false,
    val isConnected: Boolean = true,
    val avatarColorHex: Long = 0xFF10B981L
)

data class RoomSessionInfo(
    val roomId: String,
    val roomCode: String,
    val privacy: RoomPrivacy,
    val mode: GameModeId = GameModeId.FOUR_PLAYER_SQUAD,
    val districtId: String = "dhaka",
    val maxPlayers: Int = 4,
    val phase: MatchPhase = MatchPhase.WAITING,
    val players: List<NetworkPlayerSlot> = emptyList(),
    val endpointLabel: String = "127.0.0.1 (Local Authoritative Test Mode)"
)

data class PublicRoomSummary(
    val roomId: String,
    val roomCode: String,
    val hostName: String,
    val mode: GameModeId,
    val districtId: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val pingMs: Int,
    val privacy: RoomPrivacy = RoomPrivacy.PUBLIC
)

data class RemotePlayerSnapshot(
    val playerId: String,
    val callsign: String,
    val characterId: String,
    val team: TeamSide,
    val x: Float,
    val y: Float,
    val z: Float,
    val yawRad: Float,
    val health: Int,
    val weaponId: String,
    val ammoInMag: Int,
    val kills: Int,
    val isConnected: Boolean,
    val isAlive: Boolean
)

data class WorldStateSnapshot(
    val roomId: String,
    val tick: Long,
    val phase: MatchPhase,
    val remainingSec: Int,
    val winningTeam: String? = null,
    val players: List<RemotePlayerSnapshot> = emptyList()
)

enum class NetworkSolutionEvaluation(
    val engineName: String,
    val suitabilityNote: String
) {
    ANDROID_LOOPBACK_AUTHORITATIVE_TCP(
        engineName = "Local Authoritative TCP Server (Integrated Test Runtime)",
        suitabilityNote = "Active in this Android build: real loopback TCP ServerSocket + multi-client state sync, anti-cheat checks, and room management without external cloud credentials."
    ),
    PHOTON_FUSION_OR_REALTIME(
        engineName = "Photon Fusion / Realtime (Unity Target Evaluation)",
        suitabilityNote = "Recommended for Unity 6 cross-platform release if host/cloud relay is chosen: compact state snapshots & mobile NAT punchthrough."
    ),
    UNITY_NETCODE_FOR_GAMEOBJECTS(
        engineName = "Unity Netcode + Dedicated Server (Authoritative Evaluation)",
        suitabilityNote = "Best for competitive Team Battle & Survival anti-cheat when paired with PlayFab or custom ASP.NET Core account services."
    )
}

interface INetworkTransport {
    val connectionState: ConnectionState
    suspend fun connect(endpoint: String): Result<Unit>
    suspend fun disconnect()
    fun sendSnapshot(payload: ByteArray)
}

interface IMatchmakingService {
    suspend fun createRoom(privacy: RoomPrivacy, districtId: String): Result<RoomSessionInfo>
    suspend fun joinRoomByCode(roomCode: String): Result<RoomSessionInfo>
    suspend fun findPublicMatch(districtId: String, squadSize: Int): Result<RoomSessionInfo>
}

interface IRoomSessionManager {
    val activeRoom: RoomSessionInfo?
    suspend fun setLocalReady(ready: Boolean)
    suspend fun leaveRoom()
}
