package com.example.game.social

import com.example.game.localization.AppLanguage
import com.example.game.modes.GameModeId

enum class FriendStatus(val id: String) {
    NONE("none"),
    PENDING_OUTGOING("pending_outgoing"),
    PENDING_INCOMING("pending_incoming"),
    ACCEPTED("accepted");

    companion object {
        fun fromId(raw: String): FriendStatus = entries.firstOrNull { it.id == raw } ?: NONE
    }
}

enum class PresenceStatus(val id: String, val labelEn: String, val labelBn: String) {
    ONLINE("online", "Online", "অনলাইন"),
    IN_ROOM("in_room", "In Lobby Room", "রুমে আছে"),
    IN_MATCH("in_match", "In Match", "ম্যাচে আছে"),
    OFFLINE("offline", "Offline", "অফলাইন");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(raw: String): PresenceStatus = entries.firstOrNull { it.id == raw } ?: ONLINE
    }
}

data class AchievementDef(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val isUnlocked: Boolean
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) titleBn else titleEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) descBn else descEn
}

data class PlayerAccountSummary(
    val playerId: String,
    val username: String,
    val xp: Int,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val eliminations: Int,
    val isSignedIn: Boolean,
    val createdAtMs: Long
)

data class PlayerDirectoryEntry(
    val playerId: String,
    val username: String,
    val rankLabel: String,
    val xp: Int,
    val presence: PresenceStatus,
    val activeRoomCode: String? = null,
    val isLocalAccount: Boolean = false
)

data class FriendEntryUi(
    val playerId: String,
    val username: String,
    val status: FriendStatus,
    val presence: PresenceStatus,
    val activeRoomCode: String?,
    val isMuted: Boolean
)

data class RoomInvite(
    val id: String,
    val targetPlayerId: String,
    val targetUsername: String,
    val roomCode: String,
    val mode: GameModeId,
    val sentAtMs: Long = System.currentTimeMillis()
)

object AchievementEvaluator {
    fun evaluate(
        xp: Int,
        matchesPlayed: Int,
        matchesWon: Int,
        eliminations: Int,
        unlockedDistrictsCount: Int,
        acceptedFriendsCount: Int
    ): List<AchievementDef> = listOf(
        AchievementDef(
            id = "ach_dhaka_rookie",
            titleEn = "Dhaka Defender",
            titleBn = "ঢাকা ডিফেন্ডার",
            descEn = "Earn at least 100 XP in combat or training.",
            descBn = "যুদ্ধ বা প্রশিক্ষণে কমপক্ষে ১০০ এক্সপি অর্জন করুন।",
            isUnlocked = xp >= 100
        ),
        AchievementDef(
            id = "ach_first_blood",
            titleEn = "Precision Striker",
            titleBn = "নিখুঁত যোদ্ধা",
            descEn = "Record 5 eliminations across solo or multiplayer matches.",
            descBn = "সোলো বা মাল্টিপ্লেয়ার ম্যাচে ৫টি এলিমিনেশন সম্পন্ন করুন।",
            isUnlocked = eliminations >= 5
        ),
        AchievementDef(
            id = "ach_squad_victory",
            titleEn = "District Victor",
            titleBn = "জেলা বিজয়ী",
            descEn = "Win at least 1 challenge or multiplayer match.",
            descBn = "কমপক্ষে ১টি চ্যালেঞ্জ বা মাল্টিপ্লেয়ার ম্যাচ জিতুন।",
            isUnlocked = matchesWon >= 1 || matchesPlayed >= 1 && xp >= 240
        ),
        AchievementDef(
            id = "ach_social_warrior",
            titleEn = "Brotherhood of 64",
            titleBn = "৬৪ জেলার ভ্রাতৃত্ব",
            descEn = "Connect with at least 1 friend or unlock Gazipur District.",
            descBn = "কমপক্ষে ১ জন বন্ধু যুক্ত করুন অথবা গাজীপুর জেলা আনলক করুন।",
            isUnlocked = acceptedFriendsCount >= 1 || unlockedDistrictsCount >= 2
        )
    )
}

object PlayerDirectoryCatalog {
    val entries: List<PlayerDirectoryEntry> = listOf(
        PlayerDirectoryEntry("bot_host_1", "DhakaLeader", "Captain", 1420, PresenceStatus.IN_ROOM, activeRoomCode = "BD401"),
        PlayerDirectoryEntry("bot_host_2", "ChawkRanger", "Sergeant", 960, PresenceStatus.IN_ROOM, activeRoomCode = "BD202"),
        PlayerDirectoryEntry("dir_sylhet_scout", "SylhetScout", "Corporal", 680, PresenceStatus.ONLINE, activeRoomCode = "BD401"),
        PlayerDirectoryEntry("dir_ctg_ops", "ChattogramOps", "Major", 2150, PresenceStatus.ONLINE),
        PlayerDirectoryEntry("dir_raj_medic", "RajshahiMedic", "Lieutenant", 1180, PresenceStatus.IN_MATCH),
        PlayerDirectoryEntry("dir_khulna_heavy", "KhulnaHeavy", "Sergeant", 840, PresenceStatus.OFFLINE)
    )
}
