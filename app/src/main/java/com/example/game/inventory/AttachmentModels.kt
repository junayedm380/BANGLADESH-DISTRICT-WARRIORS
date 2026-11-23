package com.example.game.inventory

import com.example.game.localization.AppLanguage

enum class OpticAttachment(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val adsFov: Float,
    val spreadMultiplier: Float
) {
    NONE("optic_none", "Iron Sights", "আয়রন সাইট", 54f, 0.82f),
    RED_DOT("optic_red_dot", "Red Dot", "রেড ডট", 48f, 0.72f),
    HOLOGRAPHIC("optic_holo", "Holographic", "হলোগ্রাফিক", 46f, 0.70f),
    SCOPE_2X("optic_2x", "2x Scope", "২x স্কোপ", 38f, 0.58f),
    SCOPE_4X("optic_4x", "4x Scope", "৪x স্কোপ", 28f, 0.44f),
    SCOPE_6X("optic_6x", "6x Scope", "৬x স্কোপ", 20f, 0.34f),
    SCOPE_8X("optic_8x", "8x Scope", "৮x স্কোপ", 14f, 0.24f);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): OpticAttachment = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class MuzzleAttachment(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val recoilMultiplier: Float,
    val noiseRadiusMultiplier: Float,
    val isSuppressed: Boolean
) {
    NONE("muzzle_none", "Standard Barrel", "স্ট্যান্ডার্ড ব্যারেল", 1.0f, 1.0f, false),
    SUPPRESSOR("muzzle_suppressor", "Suppressor", "সাপ্রেসার", 0.90f, 0.45f, true),
    COMPENSATOR("muzzle_compensator", "Compensator", "কম্পেনসেটর", 0.68f, 1.05f, false);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): MuzzleAttachment = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class MagazineAttachment(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val capacityMultiplier: Float
) {
    STANDARD("mag_standard", "Standard Mag", "স্ট্যান্ডার্ড ম্যাগ", 1.0f),
    EXTENDED("mag_extended", "Extended Magazine", "এক্সটেন্ডেড ম্যাগাজিন", 1.4f);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): MagazineAttachment = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

enum class UnderbarrelAttachment(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val recoilMultiplier: Float,
    val spreadMultiplier: Float
) {
    NONE("grip_none", "No Grip", "গ্রিপ নেই", 1.0f, 1.0f),
    FOREGRIP("grip_foregrip", "Foregrip", "ফোরগ্রিপ", 0.74f, 0.76f);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn

    companion object {
        fun fromId(id: String): UnderbarrelAttachment = entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class WeaponLoadoutAttachments(
    val optic: OpticAttachment = OpticAttachment.RED_DOT,
    val muzzle: MuzzleAttachment = MuzzleAttachment.NONE,
    val magazine: MagazineAttachment = MagazineAttachment.STANDARD,
    val underbarrel: UnderbarrelAttachment = UnderbarrelAttachment.NONE
) {
    fun effectiveMagSize(baseMag: Int): Int =
        if (baseMag <= 0) 0 else kotlin.math.ceil(baseMag * magazine.capacityMultiplier).toInt().coerceAtLeast(baseMag)

    fun effectiveRecoil(baseRecoil: Float): Float =
        baseRecoil * muzzle.recoilMultiplier * underbarrel.recoilMultiplier

    fun effectiveSpread(baseSpread: Float, isAiming: Boolean): Float {
        val adsFactor = if (isAiming) optic.spreadMultiplier else 1.0f
        return baseSpread * adsFactor * underbarrel.spreadMultiplier
    }
}
