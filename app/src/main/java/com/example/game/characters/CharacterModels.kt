package com.example.game.characters

import com.example.game.localization.AppLanguage

enum class OutfitStyle(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val primaryRgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>
) {
    DHAKA_EMERALD(
        id = "dhaka_emerald",
        labelEn = "Dhaka Emerald",
        labelBn = "ঢাকা এমারেল্ড",
        primaryRgb = Triple(0.06f, 0.56f, 0.38f),
        accentRgb = Triple(0.96f, 0.62f, 0.04f)
    ),
    CRIMSON_OPS(
        id = "crimson_ops",
        labelEn = "Crimson Ops",
        labelBn = "ক্রিমসন অপস",
        primaryRgb = Triple(0.76f, 0.14f, 0.20f),
        accentRgb = Triple(0.95f, 0.95f, 0.96f)
    ),
    NIGHT_OBSIDIAN(
        id = "night_obsidian",
        labelEn = "Night Obsidian",
        labelBn = "নাইট অবসিডিয়ান",
        primaryRgb = Triple(0.14f, 0.18f, 0.24f),
        accentRgb = Triple(0.04f, 0.72f, 0.84f)
    ),
    CYBER_GOLD(
        id = "cyber_gold",
        labelEn = "Cyber Gold",
        labelBn = "সাইবার গোল্ড",
        primaryRgb = Triple(0.75f, 0.52f, 0.08f),
        accentRgb = Triple(0.06f, 0.85f, 0.55f)
    ),
    PADMA_CAMO(
        id = "padma_camo",
        labelEn = "Padma Recon",
        labelBn = "পদ্মা রিকন",
        primaryRgb = Triple(0.24f, 0.36f, 0.24f),
        accentRgb = Triple(0.72f, 0.62f, 0.38f)
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): OutfitStyle = entries.firstOrNull { it.id == id } ?: DHAKA_EMERALD
    }
}

enum class HeadwearOption(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val rgb: Triple<Float, Float, Float>
) {
    NONE("head_none", "Default Hair", "সাধারণ", Triple(0.1f, 0.1f, 0.1f)),
    TACTICAL_CAP("head_cap", "Tactical Cap", "ট্যাকটিক্যাল ক্যাপ", Triple(0.12f, 0.42f, 0.28f)),
    COMBAT_HELMET("head_helmet", "Combat Helmet", "কমব্যাট হেলমেট", Triple(0.22f, 0.26f, 0.30f)),
    VISOR_SHADES("head_visor", "Night Visor", "নাইট ভিসর", Triple(0.05f, 0.75f, 0.88f)),
    HEADBAND("head_band", "Warrior Band", "ওয়ারিয়র ব্যান্ড", Triple(0.85f, 0.18f, 0.22f));

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): HeadwearOption = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class AccessoryOption(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    NONE("acc_none", "None", "কিছুই না"),
    TACTICAL_BACKPACK("acc_backpack", "Tactical Pack", "ট্যাকটিক্যাল ব্যাগ"),
    UTILITY_BELT("acc_belt", "Utility Belt", "ইউটিলিটি বেল্ট"),
    SHOULDER_ARMOR("acc_pauldrons", "Shoulder Armor", "শোল্ডার আর্মার");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): AccessoryOption = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class CharacterColorTint(
    val id: String,
    val labelEn: String,
    val rgb: Triple<Float, Float, Float>?
) {
    DEFAULT("tint_default", "Outfit Default", null),
    BENGAL_GREEN("tint_green", "Emerald", Triple(0.04f, 0.66f, 0.42f)),
    DHAKA_RED("tint_red", "Crimson", Triple(0.84f, 0.18f, 0.24f)),
    ROYAL_BLUE("tint_blue", "Cobalt", Triple(0.14f, 0.42f, 0.86f)),
    GOLDEN_AMBER("tint_amber", "Amber", Triple(0.92f, 0.60f, 0.08f)),
    STEALTH_SLATE("tint_slate", "Stealth", Triple(0.18f, 0.22f, 0.27f));

    companion object {
        fun fromId(id: String): CharacterColorTint = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

enum class EmoteAction(val id: String, val labelEn: String, val labelBn: String) {
    READY_STANCE("ready_stance", "Ready Stance", "প্রস্তুত ভঙ্গি"),
    SALUTE("salute", "Warrior Salute", "যোদ্ধা স্যালুট"),
    VICTORY_FIST("victory_fist", "Victory Raise", "বিজয় ভঙ্গি"),
    COMBAT_WARMUP("combat_warmup", "Combat Warmup", "কমব্যাট ওয়ার্মআপ");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): EmoteAction = entries.firstOrNull { it.id == id } ?: READY_STANCE
    }
}

enum class AnimationProfile(val id: String, val labelEn: String, val speedFactor: Float) {
    BALANCED("anim_balanced", "Standard Tactical", 1.0f),
    AGILE("anim_agile", "High-Tempo Agile", 1.15f),
    HEAVY("anim_heavy", "Heavy Vanguard", 0.90f)
}

enum class ExpressionSlot(val id: String, val labelEn: String, val labelBn: String) {
    FOCUSED("focused", "Focused", "মনোযোগী"),
    CONFIDENT("confident", "Confident", "আত্মবিশ্বাসী"),
    ALERT("alert", "Combat Alert", "সতর্ক");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

data class CharacterSlot(
    val id: String,
    val slotNumber: Int,
    val displayName: String,
    val roleEn: String,
    val roleBn: String,
    val bioEn: String,
    val bioBn: String,
    val defaultOutfit: OutfitStyle,
    val speedMultiplier: Float,
    val skinToneRgb: Triple<Float, Float, Float>,
    val hairRgb: Triple<Float, Float, Float>,
    val animationProfile: AnimationProfile = AnimationProfile.BALANCED,
    val isFictionalPlaceholder: Boolean = true
) {
    fun role(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) roleBn else roleEn
    fun bio(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) bioBn else bioEn
}

object CharacterCatalog {
    val slots: List<CharacterSlot> = listOf(
        CharacterSlot(
            id = "hero_alam",
            slotNumber = 1,
            displayName = "Hero Alam",
            roleEn = "Street Vanguard • Prototype Slot 1",
            roleBn = "স্ট্রিট ভ্যানগার্ড • প্রোটোটাইপ স্লট ১",
            bioEn = "Original fictional placeholder fighter specializing in plaza defense and bold tactical presence.",
            bioBn = "প্লাজা প্রতিরক্ষা ও সাহসী ট্যাকটিক্যাল অভিযানের জন্য কাল্পনিক প্রোটোটাইপ যোদ্ধা।",
            defaultOutfit = OutfitStyle.DHAKA_EMERALD,
            speedMultiplier = 1.0f,
            skinToneRgb = Triple(0.68f, 0.49f, 0.36f),
            hairRgb = Triple(0.09f, 0.09f, 0.10f),
            animationProfile = AnimationProfile.BALANCED
        ),
        CharacterSlot(
            id = "khairul_dewan",
            slotNumber = 2,
            displayName = "Khairul Dewan",
            roleEn = "Urban Strategist • Prototype Slot 2",
            roleBn = "আর্বান স্ট্র্যাটেজিস্ট • প্রোটোটাইপ স্লট ২",
            bioEn = "Original fictional placeholder scout tuned for precision range marksmanship and district recon.",
            bioBn = "নির্ভুল নিশানা ও জেলা রিকন অভিযানের জন্য কাল্পনিক প্রোটোটাইপ স্কাউট।",
            defaultOutfit = OutfitStyle.NIGHT_OBSIDIAN,
            speedMultiplier = 1.02f,
            skinToneRgb = Triple(0.72f, 0.54f, 0.40f),
            hairRgb = Triple(0.12f, 0.10f, 0.09f),
            animationProfile = AnimationProfile.BALANCED
        ),
        CharacterSlot(
            id = "gaming_with_talha",
            slotNumber = 3,
            displayName = "Gaming With Talha",
            roleEn = "Tactical Striker • Prototype Slot 3",
            roleBn = "ট্যাকটিক্যাল স্ট্রাইকার • প্রোটোটাইপ স্লট ৩",
            bioEn = "Original fictional placeholder squad specialist built for sustained street combat drills.",
            bioBn = "স্ট্রিট কমব্যাট ও স্কোয়াড ড্রিলের জন্য কাল্পনিক প্রোটোটাইপ বিশেষজ্ঞ।",
            defaultOutfit = OutfitStyle.CRIMSON_OPS,
            speedMultiplier = 1.0f,
            skinToneRgb = Triple(0.70f, 0.52f, 0.38f),
            hairRgb = Triple(0.08f, 0.08f, 0.10f),
            animationProfile = AnimationProfile.HEAVY
        ),
        CharacterSlot(
            id = "ishowspeed",
            slotNumber = 4,
            displayName = "IShowSpeed",
            roleEn = "Blitz Sprinter • Prototype Slot 4",
            roleBn = "ব্লিৎজ স্প্রিন্টার • প্রোটোটাইপ স্লট ৪",
            bioEn = "Original fictional placeholder agility runner designed for high-tempo parkour and pursuit challenges.",
            bioBn = "দ্রুতগতির পার্কোর ও পারস্যুট চ্যালেঞ্জের জন্য কাল্পনিক প্রোটোটাইপ রানার।",
            defaultOutfit = OutfitStyle.CYBER_GOLD,
            speedMultiplier = 1.06f,
            skinToneRgb = Triple(0.42f, 0.27f, 0.19f),
            hairRgb = Triple(0.06f, 0.06f, 0.06f),
            animationProfile = AnimationProfile.AGILE
        )
    )

    fun getById(id: String): CharacterSlot = slots.firstOrNull { it.id == id } ?: slots.first()
}
