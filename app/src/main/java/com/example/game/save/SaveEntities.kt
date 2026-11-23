package com.example.game.save

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.game.ai.AiDifficulty
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponLoadoutAttachments

enum class GraphicsQuality(val id: String, val labelEn: String, val labelBn: String) {
    LOW("low", "Low (Battery)", "লো (ব্যাটারি)"),
    MEDIUM("medium", "Medium (Balanced)", "মিডিয়াম (সুষম)"),
    HIGH("high", "High (Cinematic)", "হাই (সিনেম্যাটিক)");

    companion object {
        fun fromId(id: String): GraphicsQuality = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

@Entity(tableName = "player_save")
data class PlayerSaveEntity(
    @PrimaryKey val profileId: Int = 1,
    val callsign: String = "Warrior-BD01",
    val selectedCharacterId: String = "hero_alam",
    val selectedWeaponId: String = "ar_dhaka_m4",
    val secondaryWeaponId: String = "pistol_p92",
    val meleeWeaponId: String = "melee_knife",
    val selectedOutfitId: String = "dhaka_emerald",
    val selectedHeadwearId: String = "head_none",
    val selectedAccessoryId: String = "acc_none",
    val selectedColorTintId: String = "tint_default",
    val selectedEmoteId: String = "ready_stance",
    val opticAttachmentId: String = OpticAttachment.RED_DOT.id,
    val muzzleAttachmentId: String = MuzzleAttachment.NONE.id,
    val magazineAttachmentId: String = MagazineAttachment.STANDARD.id,
    val underbarrelAttachmentId: String = UnderbarrelAttachment.NONE.id,
    val aiDifficultyId: String = AiDifficulty.NORMAL.id,
    val audioEnabled: Boolean = true,
    val languageCode: String = "en",
    val cameraSensitivity: Float = 1.0f,
    val graphicsQualityId: String = GraphicsQuality.MEDIUM.id,
    val invertYAxis: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val xp: Int = 0,
    val takaCredits: Int = 500,
    val unlockedDistrictIdsCsv: String = "dhaka",
    val completedDistrictIdsCsv: String = "",
    val completedChallengeIdsCsv: String = "",
    val challengesClearedCount: Int = 0,
    val targetsEliminatedCount: Int = 0,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val currentDistrictId: String = "dhaka",
    val completedMissionDistrictIdsCsv: String = "",
    val configurableDistrictOrderCsv: String = "",
    val discoveredTravelNodesCsv: String = "dhaka"
) {
    fun attachments(): WeaponLoadoutAttachments = WeaponLoadoutAttachments(
        optic = OpticAttachment.fromId(opticAttachmentId),
        muzzle = MuzzleAttachment.fromId(muzzleAttachmentId),
        magazine = MagazineAttachment.fromId(magazineAttachmentId),
        underbarrel = UnderbarrelAttachment.fromId(underbarrelAttachmentId)
    )
}

@Entity(tableName = "player_accounts")
data class PlayerAccountEntity(
    @PrimaryKey val playerId: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val xp: Int = 0,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val eliminations: Int = 0,
    val isSignedIn: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "friend_links",
    primaryKeys = ["ownerPlayerId", "friendPlayerId"]
)
data class FriendLinkEntity(
    val friendPlayerId: String,
    val ownerPlayerId: String,
    val friendUsername: String,
    val statusId: String,
    val isMuted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "moderation_reports")
data class ModerationReportEntity(
    @PrimaryKey(autoGenerate = true) val reportId: Long = 0,
    val reporterPlayerId: String,
    val targetPlayerId: String,
    val targetUsername: String,
    val reasonId: String,
    val detail: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
