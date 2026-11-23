package com.example.game.progression

import com.example.game.localization.AppLanguage

enum class WarriorRank(
    val levelThreshold: Int,
    val titleEn: String,
    val titleBn: String
) {
    RECRUIT(1, "District Recruit", "জেলা রিক্রুট"),
    DHAKA_DEFENDER(2, "Dhaka Defender", "ঢাকা ডিফেন্ডার"),
    DISTRICT_RANGER(4, "District Ranger", "জেলা রেঞ্জার"),
    DIVISION_VANGUARD(7, "Division Vanguard", "বিভাগীয় ভ্যানগার্ড"),
    BENGAL_COMMANDER(10, "Bengal Commander", "বেঙ্গল কমান্ডার");

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) titleBn else titleEn
}

data class ProgressionSummary(
    val xp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val rank: WarriorRank
) {
    val progressFraction: Float
        get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

object ProgressionCalculator {
    private const val XP_PER_LEVEL = 250

    fun compute(xp: Int): ProgressionSummary {
        val safeXp = xp.coerceAtLeast(0)
        val level = (safeXp / XP_PER_LEVEL) + 1
        val xpIntoLevel = safeXp % XP_PER_LEVEL
        val rank = WarriorRank.entries.lastOrNull { level >= it.levelThreshold } ?: WarriorRank.RECRUIT
        return ProgressionSummary(
            xp = safeXp,
            level = level,
            xpIntoLevel = xpIntoLevel,
            xpForNextLevel = XP_PER_LEVEL,
            rank = rank
        )
    }
}
