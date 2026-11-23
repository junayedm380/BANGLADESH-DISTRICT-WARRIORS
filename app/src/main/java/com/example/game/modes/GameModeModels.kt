package com.example.game.modes

import com.example.game.localization.AppLanguage

enum class GameModeId(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val isPlayableNow: Boolean,
    val requiresMultiplayer: Boolean,
    val defaultMaxPlayers: Int = 1,
    val supportsTeams: Boolean = false
) {
    SOLO(
        id = "solo",
        titleEn = "Solo (Dhaka Challenge)",
        titleBn = "সোলো (ঢাকা চ্যালেঞ্জ)",
        descEn = "Playable solo or solo-matchmade Dhaka arena challenge against hostile raiders.",
        descBn = "একক বা সোলো ম্যাচমেকিংয়ে ঢাকা এরিনায় শত্রুদের বিরুদ্ধে চ্যালেঞ্জ।",
        isPlayableNow = true,
        requiresMultiplayer = false,
        defaultMaxPlayers = 1,
        supportsTeams = false
    ),
    TRAINING_GROUND(
        id = "training_ground",
        titleEn = "Training Ground",
        titleBn = "ট্রেনিং গ্রাউন্ড",
        descEn = "Free-roam Dhaka Training Ground with 26 weapons, vehicles, AI sparring & Commander Karim.",
        descBn = "২৬টি অস্ত্র, যানবাহন, এআই অনুশীলন ও কমান্ডার করিমসহ ফ্রি-রোম ট্রেনিং গ্রাউন্ড।",
        isPlayableNow = true,
        requiresMultiplayer = false,
        defaultMaxPlayers = 1,
        supportsTeams = false
    ),
    DUO(
        id = "duo",
        titleEn = "Duo Co-Op",
        titleBn = "ডুও কো-অপ",
        descEn = "2-player cooperative tactical squad synchronized over the local authoritative server.",
        descBn = "লোকাল অথরিটেটিভ সার্ভারে ২-জনের কো-অপ ট্যাকটিক্যাল স্কোয়াড।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 2,
        supportsTeams = false
    ),
    FOUR_PLAYER_SQUAD(
        id = "four_player_squad",
        titleEn = "Four-Player Squad",
        titleBn = "৪-জনের স্কোয়াড",
        descEn = "4-player squad operations with room codes, team slots, ready checks, and synced combat.",
        descBn = "রুম কোড, টিম স্লট, রেডি চেক এবং সিঙ্কড কমব্যাটসহ ৪-জনের স্কোয়াড।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = true
    ),
    ONLINE_OPEN_WORLD(
        id = "online_open_world",
        titleEn = "Online Open World",
        titleBn = "অনলাইন ওপেন ওয়ার্ল্ড",
        descEn = "Shared 4-player Dhaka open-world free roam with vehicles, loot, and social chat.",
        descBn = "যানবাহন, লুট এবং চ্যাটসহ ৪-জনের শেয়ার্ড ঢাকা ওপেন ওয়ার্ল্ড।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = false
    ),
    DISTRICT_CHALLENGE(
        id = "district_challenge",
        titleEn = "District Challenge",
        titleBn = "জেলা চ্যালেঞ্জ",
        descEn = "Co-op or solo district capture challenge on Dhaka with server-validated XP & unlocks.",
        descBn = "সার্ভার-যাচাইকৃত এক্সপি ও জেলা আনলকসহ কো-অপ বা একক জেলা চ্যালেঞ্জ।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = false
    ),
    SURVIVAL_BATTLE(
        id = "survival_battle",
        titleEn = "Survival Battle",
        titleBn = "সারভাইভাল ব্যাটল",
        descEn = "Last-squad-standing combat with loot crates, raiders, and authoritative hit detection.",
        descBn = "লুট ক্রেট, শত্রু ও সার্ভার-যাচাইকৃত হিটসহ শেষ টিকে থাকার সারভাইভাল ব্যাটল।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = true
    ),
    TEAM_BATTLE(
        id = "team_battle",
        titleEn = "Team Battle (2v2)",
        titleBn = "টিম ব্যাটল (২ বনাম ২)",
        descEn = "Team Alpha vs Team Bravo competitive Dhaka arena battle with team chat & spectator mode.",
        descBn = "টিম চ্যাট ও স্পেক্টেটর মোডসহ টিম আলফা বনাম টিম ব্রাভো ঢাকা এরিনা ব্যাটল।",
        isPlayableNow = true,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = true
    ),
    WEAPON_TESTING_ARENA(
        id = "weapon_testing_arena",
        titleEn = "Weapon Testing Arena",
        titleBn = "ওয়েপন টেস্টিং এরিনা",
        descEn = "Dedicated ballistics & attachment range (open now via Training Ground Weapon Bench).",
        descBn = "বিশেষায়িত ব্যালিস্টিকস রেঞ্জ (এখন ট্রেনিং গ্রাউন্ড বেঞ্চে চালু)।",
        isPlayableNow = false,
        requiresMultiplayer = false,
        defaultMaxPlayers = 1,
        supportsTeams = false
    ),
    RACING(
        id = "racing",
        titleEn = "Racing",
        titleBn = "রেসিং",
        descEn = "Competitive time-trial & street-circuit races across cars, bikes & boats (planned phase).",
        descBn = "গাড়ি, মোটরসাইকেল ও নৌকার স্ট্রিট-সার্কিট রেসিং (পরবর্তী ধাপ)।",
        isPlayableNow = false,
        requiresMultiplayer = true,
        defaultMaxPlayers = 4,
        supportsTeams = false
    );

    // Compatibility alias for Milestone 1/2 unit tests
    val isPlayableInMilestone1: Boolean
        get() = this == SOLO || this == TRAINING_GROUND

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) titleBn else titleEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) descBn else descEn
}
