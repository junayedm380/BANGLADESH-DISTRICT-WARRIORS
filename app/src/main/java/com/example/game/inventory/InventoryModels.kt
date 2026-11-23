package com.example.game.inventory

import com.example.game.localization.AppLanguage

enum class WeaponCategory(val id: String, val labelEn: String, val labelBn: String) {
    ASSAULT_RIFLE("ar", "Assault Rifles", "অ্যাসল্ট রাইফেল"),
    SMG("smg", "SMGs", "এসএমজি"),
    SNIPER("sr", "Sniper Rifles", "স্নাইপার রাইফেল"),
    DMR("dmr", "DMRs", "ডিএমআর"),
    SHOTGUN("sg", "Shotguns", "শটগান"),
    LMG("lmg", "LMGs", "এলএমজি"),
    PISTOL("pistol", "Pistols", "পিস্তল"),
    MELEE("melee", "Melee", "মিলি অস্ত্র");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

enum class FireMode(val labelEn: String, val labelBn: String) {
    AUTO("AUTO", "অটো"),
    SEMI("SEMI", "সেমি"),
    BURST("BURST", "বার্স্ট"),
    MELEE("MELEE", "মিলি");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) labelBn else labelEn
}

enum class WeaponSlotType {
    PRIMARY,
    SECONDARY,
    MELEE
}

data class WeaponSpec(
    val id: String,
    val category: WeaponCategory,
    val nameEn: String,
    val nameBn: String,
    val inspiredBy: String,
    val damage: Int,
    val fireCooldownMs: Long,
    val magSize: Int,
    val defaultReserveAmmo: Int,
    val reloadDurationMs: Long,
    val projectileSpeed: Float,
    val baseSpreadDeg: Float,
    val baseRecoil: Float,
    val rangeMeters: Float,
    val pelletsPerShot: Int = 1,
    val supportedFireModes: List<FireMode> = listOf(FireMode.AUTO, FireMode.SEMI),
    val tracerRgb: Triple<Float, Float, Float>,
    val modelLength: Float = 0.65f,
    val modelWidth: Float = 0.09f,
    val supportsAttachments: Boolean = true
) {
    val isMelee: Boolean get() = category == WeaponCategory.MELEE
    fun name(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
    fun categoryLabel(lang: AppLanguage): String = "${category.label(lang)} • $inspiredBy"
}

enum class ConsumableType(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val healAmount: Int,
    val staminaAmount: Int
) {
    BANDAGE("med_bandage", "Tactical Bandage", "ট্যাকটিক্যাল ব্যান্ডেজ", 25, 0),
    FIRST_AID_KIT("med_first_aid", "Dhaka First Aid Kit", "ঢাকা ফার্স্ট এইড কিট", 65, 15),
    ENERGY_DRINK("med_energy", "Bengal Energy Drink", "বেঙ্গল এনার্জি ড্রিংক", 10, 45);

    fun name(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
}

enum class EquipmentTier(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val damageReduction: Float
) {
    NONE("eq_none", "No Armor", "বর্ম নেই", 0f),
    VEST_MK1("eq_vest_1", "Dhaka Tactical Vest Mk1", "ট্যাকটিক্যাল ভেস্ট Mk1", 0.22f),
    VEST_MK2("eq_vest_2", "Bengal Heavy Vest Mk2", "বেঙ্গল হেভি ভেস্ট Mk2", 0.38f);

    fun name(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
}

enum class GroundLootKind {
    WEAPON_CRATE,
    AMMO_BOX,
    MEDKIT_DROP,
    ARMOR_DROP
}

data class GroundLootItem(
    val id: String,
    val kind: GroundLootKind,
    val x: Float,
    val y: Float,
    val z: Float,
    val weaponId: String? = null,
    val consumable: ConsumableType? = null,
    val ammoAmount: Int = 30,
    var collected: Boolean = false
)

object InventoryCatalog {
    val weapons: List<WeaponSpec> = listOf(
        // Assault Rifles (5)
        WeaponSpec(
            id = "ar_bengal_akm",
            category = WeaponCategory.ASSAULT_RIFLE,
            nameEn = "Bengal-47 Rifle",
            nameBn = "বেঙ্গল-৪৭ রাইফেল",
            inspiredBy = "AKM-Inspired Prototype",
            damage = 38,
            fireCooldownMs = 135L,
            magSize = 30,
            defaultReserveAmmo = 120,
            reloadDurationMs = 1450L,
            projectileSpeed = 52f,
            baseSpreadDeg = 2.4f,
            baseRecoil = 0.22f,
            rangeMeters = 90f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.58f, 0.14f),
            modelLength = 0.72f
        ),
        WeaponSpec(
            id = "ar_dhaka_m4",
            category = WeaponCategory.ASSAULT_RIFLE,
            nameEn = "Dhaka-M4 Carbine",
            nameBn = "ঢাকা-এম৪ কার্বাইন",
            inspiredBy = "M4-Inspired Prototype",
            damage = 34,
            fireCooldownMs = 115L,
            magSize = 30,
            defaultReserveAmmo = 120,
            reloadDurationMs = 1300L,
            projectileSpeed = 54f,
            baseSpreadDeg = 1.8f,
            baseRecoil = 0.16f,
            rangeMeters = 95f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.BURST, FireMode.SEMI),
            tracerRgb = Triple(0.15f, 0.95f, 0.55f),
            modelLength = 0.68f
        ),
        WeaponSpec(
            id = "ar_padma_scar",
            category = WeaponCategory.ASSAULT_RIFLE,
            nameEn = "Padma-SCAR Tactical",
            nameBn = "পদ্মা-স্কার ট্যাকটিক্যাল",
            inspiredBy = "SCAR-Inspired Prototype",
            damage = 37,
            fireCooldownMs = 130L,
            magSize = 28,
            defaultReserveAmmo = 112,
            reloadDurationMs = 1380L,
            projectileSpeed = 55f,
            baseSpreadDeg = 1.7f,
            baseRecoil = 0.17f,
            rangeMeters = 95f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.SEMI),
            tracerRgb = Triple(0.96f, 0.78f, 0.22f),
            modelLength = 0.70f
        ),
        WeaponSpec(
            id = "ar_sylhet_aug",
            category = WeaponCategory.ASSAULT_RIFLE,
            nameEn = "Sylhet-AUG Bullpup",
            nameBn = "সিলেট-অগ বুলপাপ",
            inspiredBy = "AUG-Inspired Prototype",
            damage = 33,
            fireCooldownMs = 110L,
            magSize = 30,
            defaultReserveAmmo = 120,
            reloadDurationMs = 1350L,
            projectileSpeed = 56f,
            baseSpreadDeg = 1.6f,
            baseRecoil = 0.14f,
            rangeMeters = 95f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.SEMI),
            tracerRgb = Triple(0.25f, 0.88f, 0.72f),
            modelLength = 0.62f
        ),
        WeaponSpec(
            id = "ar_cox_groza",
            category = WeaponCategory.ASSAULT_RIFLE,
            nameEn = "Cox-Groza Compact",
            nameBn = "কক্স-গ্রোজা কমপ্যাক্ট",
            inspiredBy = "Groza-Inspired Prototype",
            damage = 40,
            fireCooldownMs = 125L,
            magSize = 30,
            defaultReserveAmmo = 120,
            reloadDurationMs = 1400L,
            projectileSpeed = 50f,
            baseSpreadDeg = 2.2f,
            baseRecoil = 0.20f,
            rangeMeters = 85f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.35f, 0.28f),
            modelLength = 0.56f
        ),

        // SMGs (4)
        WeaponSpec(
            id = "smg_ump",
            category = WeaponCategory.SMG,
            nameEn = "Mirpur-UMP 45",
            nameBn = "মিরপুর-ইউএমপি ৪৫",
            inspiredBy = "UMP-Inspired Prototype",
            damage = 28,
            fireCooldownMs = 105L,
            magSize = 25,
            defaultReserveAmmo = 125,
            reloadDurationMs = 1150L,
            projectileSpeed = 44f,
            baseSpreadDeg = 2.5f,
            baseRecoil = 0.13f,
            rangeMeters = 65f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.BURST, FireMode.SEMI),
            tracerRgb = Triple(0.20f, 0.85f, 0.96f),
            modelLength = 0.50f
        ),
        WeaponSpec(
            id = "smg_vector",
            category = WeaponCategory.SMG,
            nameEn = "Gulshan-Vector V9",
            nameBn = "গুলশান-ভেক্টর ভি৯",
            inspiredBy = "Vector-Inspired Prototype",
            damage = 22,
            fireCooldownMs = 68L,
            magSize = 33,
            defaultReserveAmmo = 165,
            reloadDurationMs = 1100L,
            projectileSpeed = 42f,
            baseSpreadDeg = 2.8f,
            baseRecoil = 0.10f,
            rangeMeters = 55f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.BURST, FireMode.SEMI),
            tracerRgb = Triple(0.32f, 0.95f, 0.85f),
            modelLength = 0.45f
        ),
        WeaponSpec(
            id = "smg_uzi",
            category = WeaponCategory.SMG,
            nameEn = "OldDhaka-Micro UZI",
            nameBn = "পুরানঢাকা-মাইক্রো উজি",
            inspiredBy = "UZI-Inspired Prototype",
            damage = 21,
            fireCooldownMs = 65L,
            magSize = 35,
            defaultReserveAmmo = 175,
            reloadDurationMs = 1050L,
            projectileSpeed = 40f,
            baseSpreadDeg = 3.2f,
            baseRecoil = 0.11f,
            rangeMeters = 50f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.80f, 0.25f),
            modelLength = 0.38f
        ),
        WeaponSpec(
            id = "smg_mp5",
            category = WeaponCategory.SMG,
            nameEn = "Banani-MP5 Spec",
            nameBn = "বনানী-এমপি৫ স্পেক",
            inspiredBy = "MP5-Inspired Prototype",
            damage = 26,
            fireCooldownMs = 88L,
            magSize = 30,
            defaultReserveAmmo = 150,
            reloadDurationMs = 1100L,
            projectileSpeed = 45f,
            baseSpreadDeg = 2.0f,
            baseRecoil = 0.11f,
            rangeMeters = 65f,
            supportedFireModes = listOf(FireMode.AUTO, FireMode.BURST, FireMode.SEMI),
            tracerRgb = Triple(0.18f, 0.92f, 0.65f),
            modelLength = 0.52f
        ),

        // Sniper Rifles (3)
        WeaponSpec(
            id = "sr_awm",
            category = WeaponCategory.SNIPER,
            nameEn = "Sundarban-AWM Magnum",
            nameBn = "সুন্দরবন-এডব্লিউএম ম্যাগনাম",
            inspiredBy = "AWM-Inspired Prototype",
            damage = 115,
            fireCooldownMs = 1150L,
            magSize = 5,
            defaultReserveAmmo = 25,
            reloadDurationMs = 2100L,
            projectileSpeed = 90f,
            baseSpreadDeg = 0.4f,
            baseRecoil = 0.45f,
            rangeMeters = 140f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.12f, 0.98f, 0.50f),
            modelLength = 0.94f
        ),
        WeaponSpec(
            id = "sr_kar98",
            category = WeaponCategory.SNIPER,
            nameEn = "Heritage-K98 Bolt",
            nameBn = "হেরিটেজ-কে৯৮ বোল্ট",
            inspiredBy = "Kar98k-Inspired Prototype",
            damage = 95,
            fireCooldownMs = 980L,
            magSize = 5,
            defaultReserveAmmo = 30,
            reloadDurationMs = 1900L,
            projectileSpeed = 82f,
            baseSpreadDeg = 0.5f,
            baseRecoil = 0.38f,
            rangeMeters = 130f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.72f, 0.25f),
            modelLength = 0.88f
        ),
        WeaponSpec(
            id = "sr_m24",
            category = WeaponCategory.SNIPER,
            nameEn = "Chittagong-M24 Longshot",
            nameBn = "চট্টগ্রাম-এম২৪ লংশট",
            inspiredBy = "M24-Inspired Prototype",
            damage = 102,
            fireCooldownMs = 1020L,
            magSize = 5,
            defaultReserveAmmo = 30,
            reloadDurationMs = 1950L,
            projectileSpeed = 85f,
            baseSpreadDeg = 0.45f,
            baseRecoil = 0.40f,
            rangeMeters = 135f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.20f, 0.85f, 0.98f),
            modelLength = 0.90f
        ),

        // DMRs (3)
        WeaponSpec(
            id = "dmr_mini14",
            category = WeaponCategory.DMR,
            nameEn = "Teesta-Mini14 Scout",
            nameBn = "তিস্তা-মিনি১৪ স্কাউট",
            inspiredBy = "Mini14-Inspired Prototype",
            damage = 54,
            fireCooldownMs = 230L,
            magSize = 20,
            defaultReserveAmmo = 80,
            reloadDurationMs = 1550L,
            projectileSpeed = 72f,
            baseSpreadDeg = 0.9f,
            baseRecoil = 0.22f,
            rangeMeters = 115f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.15f, 0.92f, 0.95f),
            modelLength = 0.78f
        ),
        WeaponSpec(
            id = "dmr_sks",
            category = WeaponCategory.DMR,
            nameEn = "Jamuna-SKS Marksman",
            nameBn = "যমুনা-এসকেএস মার্কসম্যান",
            inspiredBy = "SKS-Inspired Prototype",
            damage = 62,
            fireCooldownMs = 270L,
            magSize = 10,
            defaultReserveAmmo = 60,
            reloadDurationMs = 1500L,
            projectileSpeed = 70f,
            baseSpreadDeg = 1.0f,
            baseRecoil = 0.26f,
            rangeMeters = 115f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.65f, 0.20f),
            modelLength = 0.80f
        ),
        WeaponSpec(
            id = "dmr_slr",
            category = WeaponCategory.DMR,
            nameEn = "Meghna-SLR Heavy",
            nameBn = "মেঘনা-এসএলআর হেভি",
            inspiredBy = "SLR-Inspired Prototype",
            damage = 70,
            fireCooldownMs = 310L,
            magSize = 10,
            defaultReserveAmmo = 60,
            reloadDurationMs = 1650L,
            projectileSpeed = 74f,
            baseSpreadDeg = 1.1f,
            baseRecoil = 0.30f,
            rangeMeters = 120f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.42f, 0.22f),
            modelLength = 0.82f
        ),

        // Shotguns (3)
        WeaponSpec(
            id = "sg_pump",
            category = WeaponCategory.SHOTGUN,
            nameEn = "Khulna-12 Pump",
            nameBn = "খুলনা-১২ পাম্প",
            inspiredBy = "Pump-Action Prototype",
            damage = 22,
            fireCooldownMs = 680L,
            magSize = 8,
            defaultReserveAmmo = 32,
            reloadDurationMs = 1750L,
            projectileSpeed = 48f,
            baseSpreadDeg = 6.2f,
            baseRecoil = 0.36f,
            rangeMeters = 36f,
            pelletsPerShot = 6,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.54f, 0.12f),
            modelLength = 0.72f
        ),
        WeaponSpec(
            id = "sg_semi",
            category = WeaponCategory.SHOTGUN,
            nameEn = "Barishal-Semi 12G",
            nameBn = "বরিশাল-সেমি ১২জি",
            inspiredBy = "Semi-Automatic Prototype",
            damage = 20,
            fireCooldownMs = 360L,
            magSize = 7,
            defaultReserveAmmo = 35,
            reloadDurationMs = 1600L,
            projectileSpeed = 48f,
            baseSpreadDeg = 6.8f,
            baseRecoil = 0.32f,
            rangeMeters = 34f,
            pelletsPerShot = 5,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.62f, 0.18f),
            modelLength = 0.70f
        ),
        WeaponSpec(
            id = "sg_double",
            category = WeaponCategory.SHOTGUN,
            nameEn = "Rajshahi-Double Barrel",
            nameBn = "রাজশাহী-ডাবল ব্যারেল",
            inspiredBy = "Double-Barrel Prototype",
            damage = 24,
            fireCooldownMs = 450L,
            magSize = 2,
            defaultReserveAmmo = 28,
            reloadDurationMs = 1450L,
            projectileSpeed = 50f,
            baseSpreadDeg = 7.4f,
            baseRecoil = 0.44f,
            rangeMeters = 32f,
            pelletsPerShot = 8,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.38f, 0.14f),
            modelLength = 0.74f
        ),

        // LMGs (2)
        WeaponSpec(
            id = "lmg_m249",
            category = WeaponCategory.LMG,
            nameEn = "Bravo-M249 SAW",
            nameBn = "ব্রাভো-এম২৪৯ এসএডব্লিউ",
            inspiredBy = "M249-Inspired Prototype",
            damage = 32,
            fireCooldownMs = 95L,
            magSize = 100,
            defaultReserveAmmo = 200,
            reloadDurationMs = 2600L,
            projectileSpeed = 56f,
            baseSpreadDeg = 2.6f,
            baseRecoil = 0.19f,
            rangeMeters = 95f,
            supportedFireModes = listOf(FireMode.AUTO),
            tracerRgb = Triple(0.98f, 0.85f, 0.25f),
            modelLength = 0.80f,
            modelWidth = 0.13f
        ),
        WeaponSpec(
            id = "lmg_dp28",
            category = WeaponCategory.LMG,
            nameEn = "Delta-DP28 Disc LMG",
            nameBn = "ডেল্টা-ডিপি২৮ এলএমজি",
            inspiredBy = "DP-28-Inspired Prototype",
            damage = 39,
            fireCooldownMs = 125L,
            magSize = 47,
            defaultReserveAmmo = 141,
            reloadDurationMs = 2200L,
            projectileSpeed = 55f,
            baseSpreadDeg = 2.4f,
            baseRecoil = 0.20f,
            rangeMeters = 95f,
            supportedFireModes = listOf(FireMode.AUTO),
            tracerRgb = Triple(0.96f, 0.65f, 0.20f),
            modelLength = 0.82f,
            modelWidth = 0.14f
        ),

        // Pistols (3)
        WeaponSpec(
            id = "pistol_p92",
            category = WeaponCategory.PISTOL,
            nameEn = "Dhaka-P92 Sidearm",
            nameBn = "ঢাকা-পি৯২ সাইডআর্ম",
            inspiredBy = "P92-Inspired Prototype",
            damage = 32,
            fireCooldownMs = 210L,
            magSize = 15,
            defaultReserveAmmo = 75,
            reloadDurationMs = 1000L,
            projectileSpeed = 44f,
            baseSpreadDeg = 1.9f,
            baseRecoil = 0.14f,
            rangeMeters = 55f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.72f, 0.16f),
            modelLength = 0.30f,
            modelWidth = 0.06f
        ),
        WeaponSpec(
            id = "pistol_p1911",
            category = WeaponCategory.PISTOL,
            nameEn = "Comilla-1911 Classic",
            nameBn = "কুমিল্লা-১৯১১ ক্লাসিক",
            inspiredBy = "P1911-Inspired Prototype",
            damage = 42,
            fireCooldownMs = 250L,
            magSize = 7,
            defaultReserveAmmo = 49,
            reloadDurationMs = 1050L,
            projectileSpeed = 44f,
            baseSpreadDeg = 1.8f,
            baseRecoil = 0.18f,
            rangeMeters = 55f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.95f, 0.68f, 0.22f),
            modelLength = 0.31f,
            modelWidth = 0.06f
        ),
        WeaponSpec(
            id = "pistol_deagle",
            category = WeaponCategory.PISTOL,
            nameEn = "Tiger-50 Eagle",
            nameBn = "টাইগার-৫০ ঈগল",
            inspiredBy = "Desert Eagle-Inspired Prototype",
            damage = 64,
            fireCooldownMs = 380L,
            magSize = 7,
            defaultReserveAmmo = 35,
            reloadDurationMs = 1250L,
            projectileSpeed = 52f,
            baseSpreadDeg = 2.0f,
            baseRecoil = 0.30f,
            rangeMeters = 65f,
            supportedFireModes = listOf(FireMode.SEMI),
            tracerRgb = Triple(0.98f, 0.88f, 0.32f),
            modelLength = 0.34f,
            modelWidth = 0.07f
        ),

        // Melee (3)
        WeaponSpec(
            id = "melee_knife",
            category = WeaponCategory.MELEE,
            nameEn = "Bengal Tactical Knife",
            nameBn = "বেঙ্গল ট্যাকটিক্যাল ছুরি",
            inspiredBy = "Combat Knife Prototype",
            damage = 55,
            fireCooldownMs = 360L,
            magSize = 0,
            defaultReserveAmmo = 0,
            reloadDurationMs = 0L,
            projectileSpeed = 0f,
            baseSpreadDeg = 0f,
            baseRecoil = 0.05f,
            rangeMeters = 3.2f,
            supportedFireModes = listOf(FireMode.MELEE),
            tracerRgb = Triple(0.85f, 0.90f, 0.96f),
            modelLength = 0.28f,
            modelWidth = 0.04f,
            supportsAttachments = false
        ),
        WeaponSpec(
            id = "melee_bat",
            category = WeaponCategory.MELEE,
            nameEn = "Dhaka Slugger Bat",
            nameBn = "ঢাকা স্লাগার ব্যাট",
            inspiredBy = "Baseball Bat Prototype",
            damage = 68,
            fireCooldownMs = 520L,
            magSize = 0,
            defaultReserveAmmo = 0,
            reloadDurationMs = 0L,
            projectileSpeed = 0f,
            baseSpreadDeg = 0f,
            baseRecoil = 0.08f,
            rangeMeters = 3.6f,
            supportedFireModes = listOf(FireMode.MELEE),
            tracerRgb = Triple(0.75f, 0.56f, 0.32f),
            modelLength = 0.62f,
            modelWidth = 0.07f,
            supportsAttachments = false
        ),
        WeaponSpec(
            id = "melee_pan",
            category = WeaponCategory.MELEE,
            nameEn = "Cast-Iron Tawa Pan",
            nameBn = "লোহার তাওয়া প্যান",
            inspiredBy = "Frying Pan Prototype",
            damage = 82,
            fireCooldownMs = 580L,
            magSize = 0,
            defaultReserveAmmo = 0,
            reloadDurationMs = 0L,
            projectileSpeed = 0f,
            baseSpreadDeg = 0f,
            baseRecoil = 0.10f,
            rangeMeters = 3.4f,
            supportedFireModes = listOf(FireMode.MELEE),
            tracerRgb = Triple(0.28f, 0.32f, 0.38f),
            modelLength = 0.44f,
            modelWidth = 0.22f,
            supportsAttachments = false
        )
    )

    fun byCategory(category: WeaponCategory): List<WeaponSpec> =
        weapons.filter { it.category == category }

    fun getWeapon(id: String): WeaponSpec {
        val mappedId = when (id) {
            "bd74_carbine" -> "ar_dhaka_m4"
            "dhaka9_pistol" -> "pistol_p92"
            "range_marksman" -> "dmr_mini14"
            else -> id
        }
        return weapons.firstOrNull { it.id == mappedId } ?: weapons.first()
    }
}
