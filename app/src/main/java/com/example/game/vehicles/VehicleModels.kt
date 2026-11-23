package com.example.game.vehicles

import com.example.game.engine.Vec3
import com.example.game.localization.AppLanguage

enum class VehicleType(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val maxSpeed: Float,
    val reverseSpeed: Float,
    val acceleration: Float,
    val turnRateRad: Float,
    val maxHealth: Int,
    val maxFuel: Float,
    val fuelBurnPerSec: Float,
    val length: Float,
    val width: Float,
    val height: Float,
    val bodyRgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>,
    val isWatercraft: Boolean = false
) {
    CAR(
        id = "veh_car",
        nameEn = "Dhaka Tactical Sedan",
        nameBn = "ঢাকা ট্যাকটিক্যাল সেডান",
        maxSpeed = 16.5f,
        reverseSpeed = 6.5f,
        acceleration = 14f,
        turnRateRad = 2.1f,
        maxHealth = 220,
        maxFuel = 100f,
        fuelBurnPerSec = 1.6f,
        length = 3.9f,
        width = 1.9f,
        height = 1.35f,
        bodyRgb = Triple(0.10f, 0.52f, 0.36f),
        accentRgb = Triple(0.95f, 0.68f, 0.12f)
    ),
    MOTORCYCLE(
        id = "veh_moto",
        nameEn = "Bengal Street Moto",
        nameBn = "বেঙ্গল স্ট্রিট মোটরসাইকেল",
        maxSpeed = 20.5f,
        reverseSpeed = 4.0f,
        acceleration = 19f,
        turnRateRad = 2.9f,
        maxHealth = 130,
        maxFuel = 100f,
        fuelBurnPerSec = 1.2f,
        length = 2.1f,
        width = 0.8f,
        height = 1.05f,
        bodyRgb = Triple(0.82f, 0.16f, 0.22f),
        accentRgb = Triple(0.94f, 0.94f, 0.96f)
    ),
    BUS(
        id = "veh_bus",
        nameEn = "Metro City Bus",
        nameBn = "মেট্রো সিটি বাস",
        maxSpeed = 12.0f,
        reverseSpeed = 4.5f,
        acceleration = 8.5f,
        turnRateRad = 1.35f,
        maxHealth = 450,
        maxFuel = 100f,
        fuelBurnPerSec = 2.2f,
        length = 6.2f,
        width = 2.4f,
        height = 2.5f,
        bodyRgb = Triple(0.78f, 0.18f, 0.20f),
        accentRgb = Triple(0.08f, 0.68f, 0.42f)
    ),
    TRUCK(
        id = "veh_truck",
        nameEn = "Padma Cargo Truck",
        nameBn = "পদ্মা কার্গো ট্রাক",
        maxSpeed = 13.0f,
        reverseSpeed = 5.0f,
        acceleration = 9.5f,
        turnRateRad = 1.5f,
        maxHealth = 380,
        maxFuel = 100f,
        fuelBurnPerSec = 2.0f,
        length = 5.2f,
        width = 2.3f,
        height = 2.2f,
        bodyRgb = Triple(0.18f, 0.42f, 0.56f),
        accentRgb = Triple(0.95f, 0.65f, 0.12f)
    ),
    BOAT(
        id = "veh_boat",
        nameEn = "Buriganga Patrol Boat",
        nameBn = "বুড়িগঙ্গা প্যাট্রোল বোট",
        maxSpeed = 15.0f,
        reverseSpeed = 5.0f,
        acceleration = 11.5f,
        turnRateRad = 2.0f,
        maxHealth = 240,
        maxFuel = 100f,
        fuelBurnPerSec = 1.5f,
        length = 4.2f,
        width = 1.8f,
        height = 1.0f,
        bodyRgb = Triple(0.12f, 0.36f, 0.54f),
        accentRgb = Triple(0.12f, 0.85f, 0.55f),
        isWatercraft = true
    );

    fun name(lang: AppLanguage): String = if (lang == AppLanguage.BENGALI) nameBn else nameEn
}

data class VehicleEntity(
    val id: String,
    val type: VehicleType,
    var pos: Vec3,
    var yawRad: Float,
    var speed: Float = 0f,
    var health: Int = type.maxHealth,
    var fuel: Float = type.maxFuel
) {
    val isOperational: Boolean get() = health > 0
}
