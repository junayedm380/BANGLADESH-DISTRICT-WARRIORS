package com.example.game.save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM player_save WHERE profileId = 1 LIMIT 1")
    fun observeSave(): Flow<PlayerSaveEntity?>

    @Query("SELECT * FROM player_save WHERE profileId = 1 LIMIT 1")
    suspend fun getSaveOnce(): PlayerSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSave(save: PlayerSaveEntity)
}

@Dao
interface SocialAccountDao {
    @Query("SELECT * FROM player_accounts WHERE isSignedIn = 1 LIMIT 1")
    fun observeSignedInAccount(): Flow<PlayerAccountEntity?>

    @Query("SELECT * FROM player_accounts WHERE isSignedIn = 1 LIMIT 1")
    suspend fun getSignedInAccountOnce(): PlayerAccountEntity?

    @Query("SELECT * FROM player_accounts WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findAccountByUsername(username: String): PlayerAccountEntity?

    @Query("SELECT * FROM player_accounts ORDER BY xp DESC")
    suspend fun getAllAccountsOnce(): List<PlayerAccountEntity>

    @Query("UPDATE player_accounts SET isSignedIn = 0")
    suspend fun signOutAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: PlayerAccountEntity)

    @Query("SELECT * FROM friend_links WHERE ownerPlayerId = :ownerId ORDER BY updatedAtEpochMs DESC")
    fun observeFriends(ownerId: String): Flow<List<FriendLinkEntity>>

    @Query("SELECT * FROM friend_links WHERE ownerPlayerId = :ownerId")
    suspend fun getFriendsOnce(ownerId: String): List<FriendLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriend(friend: FriendLinkEntity)

    @Query("DELETE FROM friend_links WHERE ownerPlayerId = :ownerId AND friendPlayerId = :friendId")
    suspend fun deleteFriend(ownerId: String, friendId: String)

    @Insert
    suspend fun insertModerationReport(report: ModerationReportEntity)

    @Query("SELECT * FROM moderation_reports ORDER BY createdAtEpochMs DESC")
    fun observeModerationReports(): Flow<List<ModerationReportEntity>>
}
