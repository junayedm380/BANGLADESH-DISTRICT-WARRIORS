package com.example.game.challenges

import com.example.game.districts.DistrictCatalog
import com.example.game.localization.AppLanguage

enum class ChallengeType(val labelEn: String, val labelBn: String) {
    SHOOTING("Shooting", "শুটিং"),
    SURVIVAL("Survival", "সারভাইভাল"),
    OBSTACLE_COURSE("Obstacle Course", "অবস্ট্যাকল কোর্স"),
    RACING("Racing", "রেসিং"),
    ESCAPE("Escape", "পলায়ন"),
    TEAM_BATTLE("Team Battle", "টিম ব্যাটল"),
    BOSS_FIGHT("Boss Fight", "বস ফাইট"),
    VEHICLE_MISSION("Vehicle Mission", "যানবাহন মিশন"),
    TIMED_PARKOUR("Timed Parkour", "টাইমড পার্কোর"),
    MIXED("Mixed Challenge", "মিশ্র চ্যালেঞ্জ");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

data class ChallengeDefinition(
    val id: String,
    val districtId: String,
    val type: ChallengeType,
    val titleEn: String,
    val titleBn: String,
    val briefingEn: String,
    val briefingBn: String,
    val targetDroneCount: Int,
    val timeLimitSeconds: Int,
    val xpReward: Int,
    val takaReward: Int
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) titleBn else titleEn
    fun briefing(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) briefingBn else briefingEn
}

enum class ChallengeOutcome {
    IN_PROGRESS,
    VICTORY,
    SPECTATOR_DEFEAT
}

object ChallengeCatalog {
    val dhakaPlazaPrototype = ChallengeDefinition(
        id = "challenge_dhaka",
        districtId = "dhaka",
        type = ChallengeType.MIXED,
        titleEn = "Dhaka Plaza Defense",
        titleBn = "ঢাকা প্লাজা প্রতিরক্ষা",
        briefingEn = "Neutralize all 6 hostile tactical drones patrolling the Dhaka plaza before your health drops to zero.",
        briefingBn = "আপনার হেলথ শূন্যে নামার আগেই ঢাকা প্লাজায় টহলরত ৬টি শত্রু ট্যাকটিক্যাল ড্রোন ধ্বংস করুন।",
        targetDroneCount = 6,
        timeLimitSeconds = 180,
        xpReward = 250,
        takaReward = 1000
    )

    fun forDistrict(districtId: String): ChallengeDefinition {
        if (districtId == "dhaka") return dhakaPlazaPrototype
        val entry = DistrictCatalog.getById(districtId)
        return ChallengeDefinition(
            id = entry.challengeId,
            districtId = entry.id,
            type = ChallengeType.MIXED,
            titleEn = entry.challengeTitleEn,
            titleBn = entry.challengeTitleBn,
            briefingEn = "Clear all 6 tactical hostiles and secure ${entry.nameEn} (${entry.landmarkEn}).",
            briefingBn = "${entry.nameBn} (${entry.landmarkBn}) এলাকায় টহলরত ৬টি শত্রু ইউনিট নিষ্ক্রিয় করুন।",
            targetDroneCount = 6,
            timeLimitSeconds = 180,
            xpReward = entry.xpReward,
            takaReward = entry.takaReward
        )
    }
}
