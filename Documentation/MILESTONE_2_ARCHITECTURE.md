# BANGLADESH: DISTRICT WARRIORS — Milestone 2 Architecture (Android Kotlin / Jetpack Compose / OpenGL ES 2.0)

> **Platform Note:** This project continues the Android Kotlin + Jetpack Compose + OpenGL ES 2.0 + Room implementation built in this cloud Android Gradle environment (see `MILESTONE_1_ARCHITECTURE.md`). All Milestone 1 systems remain preserved and integrated.

## 1. Advanced Player Movement & Camera
- **`GameSimulation.kt` (`MovementStance`)**: Supports `IDLE`, `WALK`, `RUN`, `SPRINT`, `CROUCH`, `SWIMMING` (inside Buriganga Canal `waterZone` in `DhakaWorldData`), `VAULTING` (arc traversal over low tactical cover `vaultables`), and `DRIVING`.
- **1P / 3P Camera & ADS**: Seamless interpolation between third-person shoulder camera (with wall-collision ray clamping) and first-person eye camera, plus smooth FOV zoom (`currentFov`) driven by equipped `OpticAttachment`.
- **Health, Armor & Feedback**: Armor damage mitigation (`EquipmentTier`), directional screen damage flash, hit-marker reticle feedback, and automatic transition to `ChallengeOutcome.SPECTATOR_DEFEAT` on elimination.

## 2. Modular Weapon Catalog (8 Categories, 26 Prototypes)
- **`InventoryModels.kt` (`InventoryCatalog`)**:
  - **Assault Rifles (5):** `ar_bengal_akm` (AKM-inspired), `ar_dhaka_m4` (M4-inspired), `ar_padma_scar` (SCAR-inspired), `ar_sylhet_aug` (AUG-inspired), `ar_cox_groza` (Groza-inspired)
  - **SMGs (4):** `smg_ump`, `smg_vector`, `smg_uzi`, `smg_mp5`
  - **Sniper Rifles (3):** `sr_awm`, `sr_kar98`, `sr_m24`
  - **DMRs (3):** `dmr_mini14`, `dmr_sks`, `dmr_slr`
  - **Shotguns (3):** `sg_pump`, `sg_semi`, `sg_double` (multi-pellet raycasts)
  - **LMGs (2):** `lmg_m249`, `lmg_dp28`
  - **Pistols (3):** `pistol_p92`, `pistol_p1911`, `pistol_deagle`
  - **Melee (3):** `melee_knife`, `melee_bat`, `melee_pan`
- **Fire Modes & Ballistics:** `AUTO`, `SEMI`, `BURST`, `MELEE`, recoil kick, bullet spread, reserve ammo, magazine reload timers, headshot multiplier (1.45x), muzzle flash, tracer projectiles, and impact sparks.

## 3. Weapon Attachments & Customization
- **`AttachmentModels.kt` (`WeaponLoadoutAttachments`)**:
  - **Optics:** Iron Sights, Red Dot, Holographic, 2x, 4x, 6x, 8x Scopes (scales `adsFov` & ADS spread).
  - **Muzzles:** Standard, Suppressor (cuts noise radius & suppressed shot audio), Compensator (reduces recoil).
  - **Magazines:** Standard, Extended (1.4x capacity).
  - **Underbarrel:** None, Foregrip (reduces recoil & spread).

## 4. 3-Slot Inventory, Ground Loot & Consumables
- **Slots:** `PRIMARY`, `SECONDARY`, `MELEE` switchable via HUD and Bag UI.
- **Consumables & Armor:** `BANDAGE` (+25 HP), `FIRST_AID_KIT` (+65 HP / +15 Stamina), `ENERGY_DRINK` (+10 HP / +45 Stamina), `VEST_MK1` (22% reduction), `VEST_MK2` (38% reduction).
- **World Loot:** Interactive pickup (`WEAPON_CRATE`, `AMMO_BOX`, `MEDKIT_DROP`, `ARMOR_DROP`) and active-weapon drop back into the Dhaka arena.

## 5. Drivable Vehicles (5 Types)
- **`VehicleModels.kt` & `VehiclePhysicsStepper`**:
  - `CAR` (`veh_car`), `MOTORCYCLE` (`veh_moto`), `BUS` (`veh_bus`), `TRUCK` (`veh_truck`), and `BOAT` (`veh_boat` constrained to Buriganga Canal `waterZone`).
  - Acceleration, braking/reverse, speed-scaled steering, fuel consumption, building collision speed damping & collision damage, vehicle health, chase camera, procedural engine pulse audio, and holdable mobile driving controls.

## 6. Intelligent Enemy AI & NavGrid
- **`EnemyAiModels.kt` & `EnemyAiStepper`**:
  - All 8 states: `IDLE`, `PATROL`, `DETECT`, `CHASE`, `ATTACK`, `RETREAT`, `SEARCH`, `DEAD`.
  - `DhakaNavGrid` obstacle-aware steering and raycast line-of-sight checks.
  - Configurable difficulty (`EASY`, `NORMAL`, `HARD`) scaling AI health, damage, fire rate, and detection range.

## 7. Expanded Training Ground, Customization, Audio & Save v2
- **Training Ground:** Static & moving targets, Weapon Bench UI, Ammo/Heal Refill Station, 5-vehicle test track/canal, and on-demand Sparring Bot spawner.
- **Character Customization:** Preserves the 4 fictional placeholder slots (`Hero Alam`, `Khairul Dewan`, `Gaming With Talha`, `IShowSpeed`) with Outfits (including `PADMA_CAMO`), Headwear, Accessories, Color Tints, Emotes, and Animation Profiles rendered live in OpenGL ES 2.0.
- **Audio (`SoundEffectsEngine`):** Procedural PCM `AudioTrack` gunshots (suppressed vs unsuppressed), hit markers, and vehicle engine pulses.
- **Persistence (`GameDatabase` v2 with `MIGRATION_1_2`):** Persists all 3 weapon slots, 4 attachment categories, character customization selections, AI difficulty, and audio toggle alongside XP, Taka credits, and 64-district unlocks.
