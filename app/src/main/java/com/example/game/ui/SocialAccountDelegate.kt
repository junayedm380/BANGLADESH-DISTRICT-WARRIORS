package com.example.game.ui

import com.example.game.modes.GameModeId
import com.example.game.multiplayer.ChatChannel
import com.example.game.multiplayer.MultiplayerSessionCoordinator
import com.example.game.multiplayer.ReportReason
import com.example.game.multiplayer.RoomPrivacy
import com.example.game.multiplayer.TeamSide
import com.example.game.save.ModerationReportEntity
import com.example.game.save.PlayerProgressRepository
import com.example.game.save.PlayerSaveEntity
import com.example.game.social.FriendEntryUi
import com.example.game.social.PlayerAccountSummary
import com.example.game.social.PlayerDirectoryEntry
import com.example.game.social.RoomInvite
import com.example.game.social.SocialAndAccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Clean delegate owned by GameViewModel managing Player Accounts, Friends, Moderation Reports,
 * Room Invites, and the Local Authoritative Multiplayer Session Coordinator.
 */
class SocialAccountDelegate(
    private val scope: CoroutineScope,
    private val progressRepo: PlayerProgressRepository,
    private val socialRepo: SocialAndAccountRepository,
    val coordinator: MultiplayerSessionCoordinator
) {
    val activeAccount: StateFlow<PlayerAccountSummary?> = socialRepo.activeAccountFlow
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    val friends: StateFlow<List<FriendEntryUi>> = socialRepo.friendsFlow
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ModerationReportEntity>> = socialRepo.reportsFlow
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<PlayerDirectoryEntry>>(emptyList())
    val searchResults: StateFlow<List<PlayerDirectoryEntry>> = _searchResults.asStateFlow()

    private val _authStatusMessage = MutableStateFlow<String?>(null)
    val authStatusMessage: StateFlow<String?> = _authStatusMessage.asStateFlow()

    private val _sentInvites = MutableStateFlow<List<RoomInvite>>(emptyList())
    val sentInvites: StateFlow<List<RoomInvite>> = _sentInvites.asStateFlow()

    init {
        refreshSearch("")
    }

    fun effectiveIdentity(save: PlayerSaveEntity): Pair<String, String> {
        val acc = activeAccount.value
        return if (acc != null) acc.playerId to acc.username else "local_${save.profileId}" to save.callsign
    }

    fun registerAccount(username: String, password: String) {
        scope.launch {
            val res = socialRepo.registerAccount(username, password)
            _authStatusMessage.value = res.fold(
                onSuccess = {
                    progressRepo.updateCallsign(it.username)
                    refreshSearch("")
                    "Account created & signed in: ${it.username} (${it.playerId})"
                },
                onFailure = { it.message ?: "Registration failed." }
            )
        }
    }

    fun loginAccount(username: String, password: String) {
        scope.launch {
            val res = socialRepo.loginAccount(username, password)
            _authStatusMessage.value = res.fold(
                onSuccess = {
                    progressRepo.updateCallsign(it.username)
                    refreshSearch("")
                    "Signed in as ${it.username} (${it.playerId})"
                },
                onFailure = { it.message ?: "Login failed." }
            )
        }
    }

    fun logoutAccount() {
        scope.launch {
            socialRepo.logoutAccount()
            _authStatusMessage.value = "Signed out cleanly."
        }
    }

    fun clearAuthMessage() {
        _authStatusMessage.value = null
    }

    fun refreshSearch(query: String) {
        scope.launch {
            _searchResults.value = socialRepo.searchPlayers(query)
        }
    }

    fun sendFriendRequest(targetId: String, targetUsername: String) {
        scope.launch {
            socialRepo.sendFriendRequest(targetId, targetUsername)
            _authStatusMessage.value = "Friend request sent to $targetUsername."
        }
    }

    fun acceptFriendRequest(friendId: String) {
        scope.launch { socialRepo.acceptFriendRequest(friendId) }
    }

    fun rejectOrRemoveFriend(friendId: String) {
        scope.launch { socialRepo.rejectOrRemoveFriend(friendId) }
    }

    fun toggleMuteFriend(friendId: String) {
        scope.launch { socialRepo.toggleMuteFriend(friendId) }
    }

    fun reportPlayer(targetId: String, targetUsername: String, reason: ReportReason, detail: String) {
        scope.launch {
            socialRepo.reportPlayer(targetId, targetUsername, reason, detail)
            _authStatusMessage.value = "Moderation report saved for $targetUsername."
        }
    }

    fun inviteFriendToActiveRoom(friendId: String, friendUsername: String) {
        val room = coordinator.activeRoomFlow.value ?: return
        val invite = RoomInvite(
            id = "inv_${System.nanoTime()}",
            targetPlayerId = friendId,
            targetUsername = friendUsername,
            roomCode = room.roomCode,
            mode = room.mode
        )
        _sentInvites.value = (_sentInvites.value + invite).takeLast(12)
        coordinator.primaryClient.sendChat(
            ChatChannel.SYSTEM,
            "",
            "Invited $friendUsername to room ${room.roomCode}."
        )
    }

    fun ensureConnected(save: PlayerSaveEntity) {
        val (pid, uname) = effectiveIdentity(save)
        scope.launch {
            coordinator.ensureLocalServerAndConnect(pid, uname, save.selectedCharacterId, save.selectedWeaponId)
        }
    }

    fun createRoom(privacy: RoomPrivacy, mode: GameModeId, districtId: String, save: PlayerSaveEntity) {
        val (pid, uname) = effectiveIdentity(save)
        scope.launch {
            coordinator.createRoom(privacy, mode, districtId, pid, uname, save.selectedCharacterId, save.selectedWeaponId)
        }
    }

    fun quickMatch(mode: GameModeId, districtId: String, save: PlayerSaveEntity) {
        val (pid, uname) = effectiveIdentity(save)
        scope.launch {
            coordinator.quickMatch(mode, districtId, pid, uname, save.selectedCharacterId, save.selectedWeaponId)
        }
    }

    fun joinRoomByCode(code: String, save: PlayerSaveEntity) {
        val (pid, uname) = effectiveIdentity(save)
        scope.launch {
            coordinator.joinRoomByCode(code, pid, uname, save.selectedCharacterId, save.selectedWeaponId)
        }
    }

    fun launchFourPlayerTestRoom(mode: GameModeId, districtId: String, save: PlayerSaveEntity) {
        val (pid, uname) = effectiveIdentity(save)
        scope.launch {
            coordinator.launchFourPlayerTestRoom(mode, districtId, pid, uname, save.selectedCharacterId, save.selectedWeaponId)
        }
    }

    fun toggleReady(currentReady: Boolean) {
        scope.launch { coordinator.primaryClient.setLocalReady(!currentReady) }
    }

    fun setTeam(team: TeamSide) {
        coordinator.primaryClient.setTeam(team)
    }

    fun startMultiplayerMatch() {
        coordinator.primaryClient.startMatch()
    }

    fun sendChat(channel: ChatChannel, recipientId: String = "", text: String) {
        coordinator.primaryClient.sendChat(channel, recipientId, text)
    }

    fun simulateDrop() {
        scope.launch { coordinator.triggerDisconnectForTest() }
    }

    fun reconnect() {
        scope.launch { coordinator.reconnectLocalClient() }
    }

    fun testServerAnticheatTamper() {
        coordinator.primaryClient.sendInvalidRewardTamperForTest()
    }

    fun leaveRoom() {
        coordinator.leaveCurrentRoom()
    }
}
