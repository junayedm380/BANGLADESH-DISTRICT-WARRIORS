package com.example.game.social

import com.example.game.multiplayer.InputSanitizer
import com.example.game.multiplayer.PasswordHasher
import com.example.game.multiplayer.ReportReason
import com.example.game.progression.ProgressionCalculator
import com.example.game.save.FriendLinkEntity
import com.example.game.save.ModerationReportEntity
import com.example.game.save.PlayerAccountEntity
import com.example.game.save.SocialAccountDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID

class SocialAndAccountRepository(
    private val socialDao: SocialAccountDao
) {
    val activeAccountFlow: Flow<PlayerAccountSummary?> = socialDao.observeSignedInAccount()
        .map { entity -> entity?.toSummary() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val friendsFlow: Flow<List<FriendEntryUi>> = socialDao.observeSignedInAccount()
        .flatMapLatest { acc ->
            val ownerId = acc?.playerId ?: GUEST_OWNER_ID
            socialDao.observeFriends(ownerId).map { list ->
                list.map { link ->
                    val dir = PlayerDirectoryCatalog.entries.firstOrNull { it.playerId == link.friendPlayerId }
                    FriendEntryUi(
                        playerId = link.friendPlayerId,
                        username = link.friendUsername,
                        status = FriendStatus.fromId(link.statusId),
                        presence = dir?.presence ?: PresenceStatus.ONLINE,
                        activeRoomCode = dir?.activeRoomCode,
                        isMuted = link.isMuted
                    )
                }
            }
        }

    val reportsFlow: Flow<List<ModerationReportEntity>> = socialDao.observeModerationReports()

    suspend fun registerAccount(usernameRaw: String, passwordRaw: String): Result<PlayerAccountSummary> {
        val username = usernameRaw.trim()
        if (!InputSanitizer.isValidUsername(username)) {
            return Result.failure(IllegalArgumentException("Username must be 3–20 alphanumeric, '_' or '-' characters."))
        }
        if (!InputSanitizer.isValidPassword(passwordRaw)) {
            return Result.failure(IllegalArgumentException("Password must be 6–64 characters."))
        }
        val existing = socialDao.findAccountByUsername(username)
        if (existing != null) {
            return Result.failure(IllegalStateException("Username '$username' is already registered locally."))
        }
        val salt = PasswordHasher.generateSaltHex()
        val hash = PasswordHasher.hashPassword(passwordRaw, salt)
        val playerId = "BD-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        socialDao.signOutAll()
        val entity = PlayerAccountEntity(
            playerId = playerId,
            username = username,
            passwordHash = hash,
            passwordSalt = salt,
            xp = 0,
            matchesPlayed = 0,
            matchesWon = 0,
            eliminations = 0,
            isSignedIn = true
        )
        socialDao.upsertAccount(entity)
        // Seed one discoverable incoming friend request from local directory squad leader for instant testing
        socialDao.upsertFriend(
            FriendLinkEntity(
                friendPlayerId = "bot_host_1",
                ownerPlayerId = playerId,
                friendUsername = "DhakaLeader",
                statusId = FriendStatus.PENDING_INCOMING.id
            )
        )
        return Result.success(entity.toSummary())
    }

    suspend fun loginAccount(usernameRaw: String, passwordRaw: String): Result<PlayerAccountSummary> {
        val username = usernameRaw.trim()
        val entity = socialDao.findAccountByUsername(username)
            ?: return Result.failure(IllegalArgumentException("No local account found for '$username'."))
        if (!PasswordHasher.verifyPassword(passwordRaw, entity.passwordSalt, entity.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid password for '$username'."))
        }
        socialDao.signOutAll()
        val signedIn = entity.copy(isSignedIn = true)
        socialDao.upsertAccount(signedIn)
        return Result.success(signedIn.toSummary())
    }

    suspend fun logoutAccount() {
        socialDao.signOutAll()
    }

    suspend fun applyMatchStats(xpEarned: Int, won: Boolean, kills: Int) {
        val acc = socialDao.getSignedInAccountOnce() ?: return
        socialDao.upsertAccount(
            acc.copy(
                xp = acc.xp + xpEarned.coerceAtLeast(0),
                matchesPlayed = acc.matchesPlayed + 1,
                matchesWon = acc.matchesWon + if (won) 1 else 0,
                eliminations = acc.eliminations + kills.coerceAtLeast(0)
            )
        )
    }

    suspend fun searchPlayers(query: String): List<PlayerDirectoryEntry> {
        val q = query.trim().lowercase()
        val currentAcc = socialDao.getSignedInAccountOnce()
        val localAccounts = socialDao.getAllAccountsOnce()
            .filter { it.playerId != currentAcc?.playerId }
            .map { acc ->
                val rank = ProgressionCalculator.compute(acc.xp).rank.titleEn
                PlayerDirectoryEntry(
                    playerId = acc.playerId,
                    username = acc.username,
                    rankLabel = rank,
                    xp = acc.xp,
                    presence = if (acc.isSignedIn) PresenceStatus.ONLINE else PresenceStatus.OFFLINE,
                    isLocalAccount = true
                )
            }
        val combined = (localAccounts + PlayerDirectoryCatalog.entries)
            .distinctBy { it.playerId }
        return if (q.isBlank()) combined else combined.filter {
            it.username.lowercase().contains(q) || it.playerId.lowercase().contains(q)
        }
    }

    suspend fun sendFriendRequest(targetPlayerId: String, targetUsername: String) {
        val ownerId = socialDao.getSignedInAccountOnce()?.playerId ?: GUEST_OWNER_ID
        socialDao.upsertFriend(
            FriendLinkEntity(
                friendPlayerId = targetPlayerId,
                ownerPlayerId = ownerId,
                friendUsername = targetUsername,
                statusId = FriendStatus.PENDING_OUTGOING.id
            )
        )
    }

    suspend fun acceptFriendRequest(friendPlayerId: String) {
        val ownerId = socialDao.getSignedInAccountOnce()?.playerId ?: GUEST_OWNER_ID
        val current = socialDao.getFriendsOnce(ownerId).firstOrNull { it.friendPlayerId == friendPlayerId } ?: return
        socialDao.upsertFriend(
            current.copy(
                statusId = FriendStatus.ACCEPTED.id,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun rejectOrRemoveFriend(friendPlayerId: String) {
        val ownerId = socialDao.getSignedInAccountOnce()?.playerId ?: GUEST_OWNER_ID
        socialDao.deleteFriend(ownerId, friendPlayerId)
    }

    suspend fun toggleMuteFriend(friendPlayerId: String) {
        val ownerId = socialDao.getSignedInAccountOnce()?.playerId ?: GUEST_OWNER_ID
        val current = socialDao.getFriendsOnce(ownerId).firstOrNull { it.friendPlayerId == friendPlayerId } ?: return
        socialDao.upsertFriend(
            current.copy(
                isMuted = !current.isMuted,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun reportPlayer(
        targetPlayerId: String,
        targetUsername: String,
        reason: ReportReason,
        detail: String
    ) {
        val reporterId = socialDao.getSignedInAccountOnce()?.playerId ?: GUEST_OWNER_ID
        val safeDetail = InputSanitizer.sanitizeChatMessage(detail).ifBlank { reason.labelEn }
        socialDao.insertModerationReport(
            ModerationReportEntity(
                reporterPlayerId = reporterId,
                targetPlayerId = targetPlayerId,
                targetUsername = targetUsername,
                reasonId = reason.id,
                detail = safeDetail
            )
        )
    }

    private fun PlayerAccountEntity.toSummary(): PlayerAccountSummary = PlayerAccountSummary(
        playerId = playerId,
        username = username,
        xp = xp,
        matchesPlayed = matchesPlayed,
        matchesWon = matchesWon,
        eliminations = eliminations,
        isSignedIn = isSignedIn,
        createdAtMs = createdAtEpochMs
    )

    companion object {
        const val GUEST_OWNER_ID = "guest_local"
    }
}
