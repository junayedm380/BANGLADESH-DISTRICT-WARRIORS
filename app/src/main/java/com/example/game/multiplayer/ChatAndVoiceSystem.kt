package com.example.game.multiplayer

import com.example.game.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ChatChannel(val id: String, val labelEn: String, val labelBn: String) {
    GLOBAL("GLOBAL", "All Chat", "সবার চ্যাট"),
    TEAM("TEAM", "Team Chat", "টিম চ্যাট"),
    PRIVATE("PRIVATE", "Private", "প্রাইভেট"),
    SYSTEM("SYSTEM", "System", "সিস্টেম");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(raw: String): ChatChannel = entries.firstOrNull { it.id == raw } ?: GLOBAL
    }
}

data class ChatMessage(
    val id: String,
    val channel: ChatChannel,
    val senderId: String,
    val senderName: String,
    val team: TeamSide,
    val recipientId: String? = null,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class VoiceParticipantState(
    val playerId: String,
    val callsign: String,
    val isMicEnabled: Boolean = true,
    val isMutedByLocal: Boolean = false,
    val isSpeaking: Boolean = false
)

/**
 * Clean abstraction for integrating a compatible licensed voice provider (e.g., Agora, Vivox, WebRTC)
 * while supplying safe local-test mute/deafen controls when external voice SDK credentials are absent.
 */
interface IVoiceChatProvider {
    val providerName: String
    val isLocalTestStub: Boolean
    val localMicMuted: StateFlow<Boolean>
    val localDeafened: StateFlow<Boolean>
    val participants: StateFlow<List<VoiceParticipantState>>

    fun setLocalMicMuted(muted: Boolean)
    fun setLocalDeafened(deafened: Boolean)
    fun togglePlayerMute(playerId: String)
    fun syncRoomParticipants(players: List<NetworkPlayerSlot>)
}

class LocalTestVoiceProvider : IVoiceChatProvider {
    override val providerName: String = "Vivox/Agora Integration Point (Local Test Audio Stub)"
    override val isLocalTestStub: Boolean = true

    private val _localMicMuted = MutableStateFlow(true)
    override val localMicMuted: StateFlow<Boolean> = _localMicMuted.asStateFlow()

    private val _localDeafened = MutableStateFlow(false)
    override val localDeafened: StateFlow<Boolean> = _localDeafened.asStateFlow()

    private val _participants = MutableStateFlow<List<VoiceParticipantState>>(emptyList())
    override val participants: StateFlow<List<VoiceParticipantState>> = _participants.asStateFlow()

    private val mutedPlayerIds = HashSet<String>()

    override fun setLocalMicMuted(muted: Boolean) {
        _localMicMuted.value = muted
    }

    override fun setLocalDeafened(deafened: Boolean) {
        _localDeafened.value = deafened
    }

    override fun togglePlayerMute(playerId: String) {
        if (playerId in mutedPlayerIds) {
            mutedPlayerIds.remove(playerId)
        } else {
            mutedPlayerIds.add(playerId)
        }
        _participants.value = _participants.value.map { p ->
            if (p.playerId == playerId) p.copy(isMutedByLocal = playerId in mutedPlayerIds) else p
        }
    }

    override fun syncRoomParticipants(players: List<NetworkPlayerSlot>) {
        _participants.value = players.map { p ->
            VoiceParticipantState(
                playerId = p.playerId,
                callsign = p.callsign,
                isMicEnabled = p.isConnected,
                isMutedByLocal = p.playerId in mutedPlayerIds,
                isSpeaking = false
            )
        }
    }
}
