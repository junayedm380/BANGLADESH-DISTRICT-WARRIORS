package com.example.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.challenges.ChallengeCatalog
import com.example.game.challenges.ChallengeOutcome
import com.example.game.dialogue.DialogueCatalog
import com.example.game.districts.DistrictCatalog
import com.example.game.engine.GlRenderMode
import com.example.game.engine.MovementStance
import com.example.game.engine.NearbyInteraction
import com.example.game.inventory.ConsumableType
import com.example.game.inventory.InventoryCatalog
import com.example.game.inventory.WeaponCategory
import com.example.game.inventory.WeaponSlotType
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.modes.GameModeId
import com.example.game.save.GraphicsQuality
import com.example.game.save.PlayerProgressState
import com.example.game.ui.GameViewModel
import com.example.game.ui.components.GlViewport
import com.example.game.world.DistrictTravelGraph
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun GamePlayScreen(
    mode: GameModeId,
    viewModel: GameViewModel,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onExitToLobby: () -> Unit
) {
    LaunchedEffect(mode) {
        viewModel.startPlayableMode(mode)
    }

    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val streamTelemetry by viewModel.streamingTelemetry.collectAsStateWithLifecycle()
    val worldBanner by viewModel.worldBanner.collectAsStateWithLifecycle()
    val graphicsQuality = GraphicsQuality.fromId(progress.save.graphicsQualityId)
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = progress.save.hapticsEnabled

    var showPauseMenu by remember { mutableStateOf(false) }
    var activeDialogueIndex by remember { mutableStateOf<Int?>(null) }
    var showBagModal by remember { mutableStateOf(false) }
    var showWeaponBench by remember { mutableStateOf(false) }
    var showTravelModal by remember { mutableStateOf(false) }

    LaunchedEffect(worldBanner) {
        if (worldBanner != null) {
            delay(3600L)
            viewModel.clearWorldBanner()
        }
    }

    // Holdable driving states
    var gasPressed by remember { mutableStateOf(false) }
    var brakePressed by remember { mutableStateOf(false) }
    var steerLeftPressed by remember { mutableStateOf(false) }
    var steerRightPressed by remember { mutableStateOf(false) }

    LaunchedEffect(gasPressed, brakePressed, steerLeftPressed, steerRightPressed) {
        val steer = (if (steerLeftPressed) -1f else 0f) + (if (steerRightPressed) 1f else 0f)
        viewModel.simulation.setVehicleInputs(
            throttle = if (gasPressed) 1f else 0f,
            brakeReverse = if (brakePressed) 1f else 0f,
            steer = steer
        )
    }

    BackHandler {
        when {
            activeDialogueIndex != null -> activeDialogueIndex = null
            showTravelModal -> showTravelModal = false
            showBagModal -> showBagModal = false
            showWeaponBench -> showWeaponBench = false
            else -> showPauseMenu = !showPauseMenu
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        // 1. Fullscreen 3D Viewport
        GlViewport(
            simulation = viewModel.simulation,
            renderMode = GlRenderMode.PLAYABLE_WORLD,
            graphicsQuality = graphicsQuality,
            modifier = Modifier.fillMaxSize(),
            onSimulationTick = { viewModel.onRenderTick() }
        )

        // 2. Camera Look Drag Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.simulation.applyLookDelta(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // 3. Damage Feedback Flash Vignette
        if (hud.damageFlashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CrimsonStrike.copy(alpha = hud.damageFlashAlpha))
            )
        }

        // 4. Center Crosshair & Hit Marker (On foot)
        if (hud.outcome == ChallengeOutcome.IN_PROGRESS && hud.activeVehicle == null && activeDialogueIndex == null && !showPauseMenu) {
            Canvas(modifier = Modifier.size(34.dp).align(Alignment.Center)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val gap = if (hud.isAiming) 3f else 6f
                val arm = if (hud.isAiming) 12f else 9f
                val color = if (hud.isAiming) DhakaAmber else Color(0xFF10D98B)
                drawLine(color, Offset(c.x - arm - gap, c.y), Offset(c.x - gap, c.y), strokeWidth = 2.5f)
                drawLine(color, Offset(c.x + gap, c.y), Offset(c.x + arm + gap, c.y), strokeWidth = 2.5f)
                drawLine(color, Offset(c.x, c.y - arm - gap), Offset(c.x, c.y - gap), strokeWidth = 2.5f)
                drawLine(color, Offset(c.x, c.y + gap), Offset(c.x, c.y + arm + gap), strokeWidth = 2.5f)

                if (hud.hitMarkerAlpha > 0.05f) {
                    val hmColor = CrimsonStrike.copy(alpha = hud.hitMarkerAlpha)
                    val d = 9f
                    drawLine(hmColor, Offset(c.x - d, c.y - d), Offset(c.x - 3f, c.y - 3f), strokeWidth = 3f)
                    drawLine(hmColor, Offset(c.x + d, c.y - d), Offset(c.x + 3f, c.y - 3f), strokeWidth = 3f)
                    drawLine(hmColor, Offset(c.x - d, c.y + d), Offset(c.x - 3f, c.y + 3f), strokeWidth = 3f)
                    drawLine(hmColor, Offset(c.x + d, c.y + d), Offset(c.x + 3f, c.y + 3f), strokeWidth = 3f)
                }
            }
        }

        // 5. Safe-area HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            // Top-Left: Health, Stamina, Armor, Mode Objective, Stance
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TacticalSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeDistrictEntry = DistrictCatalog.getById(hud.activeDistrictId)
                        val challengeDef = ChallengeCatalog.forDistrict(hud.activeDistrictId)
                        val missionDone = hud.districtMissionCompleted || progress.isMissionDone(hud.activeDistrictId)
                        val objText = when {
                            hud.isMultiplayerSession -> "${mode.title(lang)} • Squad ${hud.remotePlayersCount + 1}"
                            hud.mode == GameModeId.SOLO -> "${activeDistrictEntry.name(lang)} • ${challengeDef.title(lang)} (${hud.enemiesEliminated}/${hud.totalEnemies})"
                            else -> "${activeDistrictEntry.name(lang)} • ${strings.trainingGround} • Hits: ${hud.trainingHitsSession}"
                        }
                        Text(objText, style = MaterialTheme.typography.labelLarge, color = DhakaAmber)
                        Text(
                            text = "${if (missionDone) "✓" else "○"} ${activeDistrictEntry.missionTitle(lang)} • ${streamTelemetry.activeLod.label} (${streamTelemetry.visibleChunkCount}/${streamTelemetry.totalChunkCount}) ${"%.1f".format(streamTelemetry.estimatedMemoryMb)}MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (missionDone) BengalEmeraldBright else TacticalCyan
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("HP ${hud.health}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            LinearProgressIndicator(
                                progress = { hud.health / 100f },
                                modifier = Modifier.width(120.dp).height(7.dp),
                                color = if (hud.health > 35) BengalEmeraldBright else CrimsonStrike,
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ST ${hud.stamina}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                            LinearProgressIndicator(
                                progress = { hud.stamina / 100f },
                                modifier = Modifier.width(120.dp).height(5.dp),
                                color = TacticalCyan,
                                trackColor = Color.White.copy(alpha = 0.14f)
                            )
                        }
                        Text(
                            text = "${hud.stance.name} • ${hud.armorTier.name(lang)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hud.stance == MovementStance.SWIMMING) TacticalCyan else Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            // World Travel / Mission Banner
            worldBanner?.let { banner ->
                Surface(
                    color = ObsidianBg.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BengalEmeraldBright),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 54.dp)
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelMedium,
                        color = BengalEmeraldBright,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            // Top-Right: Bag, Camera Switch (1P/3P), Language, Pause
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { showBagModal = true },
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DhakaAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("hud_bag_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Backpack, contentDescription = strings.bagLabel, tint = DhakaAmber, modifier = Modifier.size(16.dp))
                        Text(strings.bagLabel, style = MaterialTheme.typography.labelMedium, color = DhakaAmber)
                    }
                }

                Surface(
                    onClick = { viewModel.toggleCameraMode() },
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BengalEmeraldBright.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("camera_switch_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = null, tint = BengalEmeraldBright, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (hud.isFirstPerson) strings.cameraMode1P else strings.cameraMode3P,
                            style = MaterialTheme.typography.labelMedium,
                            color = BengalEmeraldBright
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.toggleLanguage() },
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                    modifier = Modifier.testTag("hud_language_button")
                ) {
                    Text(lang.displayLabel, style = MaterialTheme.typography.labelMedium, color = DhakaAmber, modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp))
                }

                Surface(
                    onClick = { showPauseMenu = true },
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                    modifier = Modifier.testTag("hud_pause_button")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = strings.pauseLabel, tint = Color.White, modifier = Modifier.padding(7.dp).size(18.dp))
                }
            }

            if (hud.isMultiplayerSession) {
                MultiplayerInGameOverlays(
                    viewModel = viewModel,
                    strings = strings,
                    lang = lang
                )
            }

            // Bottom Gameplay Controls (Vehicle Driving HUD vs On-Foot Combat HUD)
            if (hud.outcome == ChallengeOutcome.IN_PROGRESS) {
                val activeVeh = hud.activeVehicle
                if (activeVeh != null) {
                    // VEHICLE DRIVING HUD
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BengalEmeraldBright),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(activeVeh.type.name(lang), style = MaterialTheme.typography.labelLarge, color = BengalEmeraldBright)
                            Text(
                                text = "SPD ${(abs(activeVeh.speed) * 3.6f).toInt()} km/h  •  ${strings.vehicleHpLabel} ${activeVeh.health}/${activeVeh.type.maxHealth}  •  ${strings.fuelLabel} ${activeVeh.fuel.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Left: Steer Left / Right
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HoldableControlButton(
                            label = "◀",
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            color = TacticalCyan,
                            testTag = "veh_steer_left",
                            onHoldChanged = { steerLeftPressed = it }
                        )
                        HoldableControlButton(
                            label = "▶",
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            color = TacticalCyan,
                            testTag = "veh_steer_right",
                            onHoldChanged = { steerRightPressed = it }
                        )
                    }

                    // Right: Exit Vehicle, Brake/Reverse, Gas
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.enterOrExitVehicle() },
                            colors = ButtonDefaults.buttonColors(containerColor = DhakaAmber, contentColor = ObsidianBg),
                            modifier = Modifier.testTag("veh_exit_button")
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.exitVehicleLabel)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HoldableControlButton(
                                label = strings.brakeReverseLabel,
                                icon = null,
                                color = CrimsonStrike,
                                testTag = "veh_brake_button",
                                onHoldChanged = { brakePressed = it }
                            )
                            HoldableControlButton(
                                label = strings.gasLabel,
                                icon = null,
                                color = BengalEmeraldBright,
                                testTag = "veh_gas_button",
                                onHoldChanged = { gasPressed = it }
                            )
                        }
                    }
                } else {
                    // ON-FOOT COMBAT & MOVEMENT HUD
                    VirtualMoveThumbstick(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 6.dp),
                        onMoveChanged = { x, y -> viewModel.simulation.setMoveInput(x, y) }
                    )

                    // Center-Bottom: 3-Slot Weapon Switcher + Contextual Interaction Button
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hud.nearbyInteraction != NearbyInteraction.NONE) {
                            val promptText = when (hud.nearbyInteraction) {
                                NearbyInteraction.LOOT_NEARBY -> {
                                    val lootName = hud.nearbyLoot?.weaponId?.let { InventoryCatalog.getWeapon(it).name(lang) }
                                        ?: hud.nearbyLoot?.consumable?.name(lang)
                                        ?: hud.nearbyLoot?.kind?.name
                                        ?: ""
                                    "${strings.pickupLootPrompt}: $lootName"
                                }
                                NearbyInteraction.VEHICLE_NEARBY -> "${strings.enterVehiclePrompt}: ${hud.nearbyVehicle?.type?.name(lang) ?: ""}"
                                NearbyInteraction.NPC_COMMANDER -> strings.talkToCommander
                                NearbyInteraction.WEAPON_BENCH -> strings.openWeaponBenchPrompt
                                NearbyInteraction.AMMO_STATION -> "${strings.reloadLabel} & HP"
                                NearbyInteraction.AI_PRACTICE_PAD -> strings.spawnAiPracticePrompt
                                NearbyInteraction.MISSION_BEACON -> strings.startChallengePrompt
                                NearbyInteraction.MISSION_OBJECTIVE -> strings.missionObjectivePrompt
                                NearbyInteraction.TRAVEL_PORTAL -> strings.borderTravelPrompt
                                NearbyInteraction.NONE -> ""
                            }
                            Button(
                                onClick = {
                                    when (hud.nearbyInteraction) {
                                        NearbyInteraction.LOOT_NEARBY -> viewModel.pickupNearbyLoot()
                                        NearbyInteraction.VEHICLE_NEARBY -> viewModel.enterOrExitVehicle()
                                        NearbyInteraction.NPC_COMMANDER -> activeDialogueIndex = 0
                                        NearbyInteraction.WEAPON_BENCH -> showWeaponBench = true
                                        NearbyInteraction.AMMO_STATION -> viewModel.refillAmmoAndHealth()
                                        NearbyInteraction.AI_PRACTICE_PAD -> viewModel.spawnPracticeBot()
                                        NearbyInteraction.MISSION_BEACON -> viewModel.startPlayableMode(GameModeId.SOLO)
                                        NearbyInteraction.MISSION_OBJECTIVE -> viewModel.completeDistrictMissionObjective()
                                        NearbyInteraction.TRAVEL_PORTAL -> showTravelModal = true
                                        NearbyInteraction.NONE -> Unit
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DhakaAmber, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("hud_interact_button")
                            ) {
                                Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(promptText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // 3-Slot Quick Weapon Switcher
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WeaponSlotChip("1 ${hud.primaryWeapon.name(lang).substringBefore(" ")}", hud.activeSlot == WeaponSlotType.PRIMARY, "slot_primary_btn") {
                                viewModel.switchWeaponSlot(WeaponSlotType.PRIMARY)
                            }
                            WeaponSlotChip("2 ${hud.secondaryWeapon.name(lang).substringBefore(" ")}", hud.activeSlot == WeaponSlotType.SECONDARY, "slot_secondary_btn") {
                                viewModel.switchWeaponSlot(WeaponSlotType.SECONDARY)
                            }
                            WeaponSlotChip("3 ${hud.meleeWeapon.name(lang).substringBefore(" ")}", hud.activeSlot == WeaponSlotType.MELEE, "slot_melee_btn") {
                                viewModel.switchWeaponSlot(WeaponSlotType.MELEE)
                            }
                        }
                    }

                    // Right Tactical Action Cluster
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hud.activeWeapon.supportedFireModes.size > 1) {
                                HudPillButton(
                                    label = hud.activeFireMode.label(lang),
                                    active = true,
                                    activeColor = TacticalCyan,
                                    testTag = "hud_firemode_button",
                                    onClick = { viewModel.cycleFireMode() }
                                )
                            }
                            if (!hud.activeWeapon.isMelee) {
                                Surface(
                                    onClick = { viewModel.reloadWeapon() },
                                    color = Color.Black.copy(alpha = 0.68f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                                    modifier = Modifier.testTag("hud_reload_button")
                                ) {
                                    Text(
                                        text = if (hud.isReloading) "${strings.reloadLabel}..." else "${hud.ammoInMag}/${hud.magSize} (${hud.reserveAmmo})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (hud.ammoInMag <= 4) CrimsonStrike else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!hud.activeWeapon.isMelee) {
                                HudPillButton(strings.aimLabel, hud.isAiming, DhakaAmber, "hud_aim_button") { viewModel.toggleAim() }
                            }
                            HudPillButton(strings.sprintLabel, hud.isSprinting, TacticalCyan, "hud_sprint_button") { viewModel.setSprint(!hud.isSprinting) }
                            HudPillButton(strings.crouchLabel, hud.isCrouching, BengalEmeraldBright, "hud_crouch_button") { viewModel.toggleCrouch() }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { viewModel.jumpOrVault() },
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f)),
                                modifier = Modifier.size(54.dp).testTag("hud_jump_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.DirectionsRun, contentDescription = strings.jumpLabel, tint = Color.White)
                                }
                            }

                            Surface(
                                onClick = {
                                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.fireWeapon()
                                },
                                color = CrimsonStrike.copy(alpha = 0.90f),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(70.dp).testTag("hud_fire_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.GpsFixed, contentDescription = strings.fireLabel, tint = Color.White, modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. In-Game Bag / Consumables & Drop Modal
        if (showBagModal) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, DhakaAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${strings.bagLabel} • ${hud.armorTier.name(lang)}", style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                            Surface(onClick = { showBagModal = false }, color = Color.Transparent) {
                                Icon(Icons.Filled.Close, contentDescription = strings.closeLabel, tint = Color.White)
                            }
                        }

                        ConsumableRow(ConsumableType.BANDAGE.name(lang), hud.bandagesCount, "+25 HP", strings.useLabel) {
                            viewModel.useConsumable(ConsumableType.BANDAGE)
                        }
                        ConsumableRow(ConsumableType.FIRST_AID_KIT.name(lang), hud.firstAidCount, "+65 HP", strings.useLabel) {
                            viewModel.useConsumable(ConsumableType.FIRST_AID_KIT)
                        }
                        ConsumableRow(ConsumableType.ENERGY_DRINK.name(lang), hud.energyCount, "+45 ST", strings.useLabel) {
                            viewModel.useConsumable(ConsumableType.ENERGY_DRINK)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(
                                onClick = { viewModel.dropActiveItem() },
                                enabled = hud.activeSlot != WeaponSlotType.MELEE,
                                modifier = Modifier.testTag("bag_drop_weapon_button")
                            ) {
                                Text("${strings.dropLabel}: ${hud.activeWeapon.name(lang)}")
                            }
                            Button(onClick = { showBagModal = false }) { Text(strings.closeLabel) }
                        }
                    }
                }
            }
        }

        // 7. Training Ground Weapon Testing Bench Modal
        if (showWeaponBench) {
            var benchCat by remember { mutableStateOf(WeaponCategory.ASSAULT_RIFLE) }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, BengalEmeraldBright),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.openWeaponBenchPrompt, style = MaterialTheme.typography.titleMedium, color = BengalEmeraldBright)
                            Surface(onClick = { showWeaponBench = false }, color = Color.Transparent) {
                                Icon(Icons.Filled.Close, contentDescription = strings.closeLabel, tint = Color.White)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(WeaponCategory.entries) { cat ->
                                FilterChip(
                                    selected = cat == benchCat,
                                    onClick = { benchCat = cat },
                                    label = { Text(cat.label(lang)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                )
                            }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(InventoryCatalog.byCategory(benchCat)) { wep ->
                                Surface(
                                    onClick = {
                                        viewModel.switchTestingWeapon(wep)
                                        showWeaponBench = false
                                    },
                                    color = ObsidianBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(wep.name(lang), style = MaterialTheme.typography.titleSmall)
                                            Text("DMG ${wep.damage} • ${wep.inspiredBy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(strings.equipPrimary, style = MaterialTheme.typography.labelMedium, color = BengalEmeraldBright)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Bilingual NPC Dialogue Overlay
        val dIdx = activeDialogueIndex
        if (dIdx != null) {
            val lines = DialogueCatalog.trainingOfficerLines
            val line = lines[dIdx.coerceIn(lines.indices)]
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f)).windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.5.dp, BengalEmeraldBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(line.speaker(lang), style = MaterialTheme.typography.titleSmall, color = BengalEmeraldBright)
                        Text(line.text(lang), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (dIdx < lines.lastIndex) {
                                Button(onClick = { activeDialogueIndex = dIdx + 1 }, colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg)) {
                                    Text(strings.nextDialogue)
                                }
                            } else {
                                Button(onClick = { activeDialogueIndex = null }, colors = ButtonDefaults.buttonColors(containerColor = DhakaAmber, contentColor = ObsidianBg)) {
                                    Text(strings.closeLabel)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Spectator Mode Overlay
        if (hud.outcome == ChallengeOutcome.SPECTATOR_DEFEAT) {
            Box(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.92f)),
                    border = BorderStroke(1.5.dp, CrimsonStrike),
                    modifier = Modifier.fillMaxWidth().testTag("spectator_overlay")
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.spectatorTitle, style = MaterialTheme.typography.headlineSmall, color = CrimsonStrike)
                        Text(strings.spectatorSubtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.startPlayableMode(GameModeId.SOLO) },
                                colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                modifier = Modifier.weight(1f).testTag("spectator_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.retryChallenge)
                            }
                            OutlinedButton(onClick = onExitToLobby, modifier = Modifier.weight(1f).testTag("spectator_lobby_button")) {
                                Text(strings.returnToLobby)
                            }
                        }
                    }
                }
            }
        }

        // 9b. Border Corridor Fast Travel Modal
        if (showTravelModal) {
            val neighbors = DistrictTravelGraph.getConnectionsFrom(hud.activeDistrictId)
            val playableUnlocked = DistrictCatalog.playableDistricts.filter { it.id in progress.unlockedDistrictIds && it.id != hud.activeDistrictId }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.5.dp, DhakaAmber),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.borderTravelPrompt, style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                            Surface(onClick = { showTravelModal = false }, color = Color.Transparent) {
                                Icon(Icons.Filled.Close, contentDescription = strings.closeLabel, tint = Color.White)
                            }
                        }
                        Text(
                            text = "${strings.activeDistrictLabel}: ${DistrictCatalog.getById(hud.activeDistrictId).name(lang)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TacticalCyan
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(playableUnlocked) { dest ->
                                val edge = neighbors.firstOrNull { it.toId == dest.id }
                                Surface(
                                    onClick = {
                                        showTravelModal = false
                                        viewModel.travelToDistrict(dest.id, alsoStreamIntoWorld = true) {
                                            viewModel.startPlayableMode(mode)
                                        }
                                    },
                                    color = ObsidianBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BengalEmeraldBright.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dest.name(lang), style = MaterialTheme.typography.titleSmall, color = BengalEmeraldBright)
                                            Text(
                                                text = edge?.routeName(lang) ?: dest.biome.label(lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(strings.fastTravelLabel, style = MaterialTheme.typography.labelMedium, color = DhakaAmber)
                                    }
                                }
                            }
                        }
                        if (playableUnlocked.isEmpty()) {
                            Text(
                                text = "${strings.prerequisiteLabel}: Clear District Mission + Challenge to unlock next route.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 10. Victory Overlay
        if (hud.outcome == ChallengeOutcome.VICTORY) {
            val activeDef = ChallengeCatalog.forDistrict(hud.activeDistrictId)
            val districtEntry = DistrictCatalog.getById(hud.activeDistrictId)
            val pct = progress.completionPercent(hud.activeDistrictId)
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(2.dp, BengalEmeraldBright),
                    modifier = Modifier.fillMaxWidth().testTag("victory_overlay")
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.victoryTitle, style = MaterialTheme.typography.headlineSmall, color = BengalEmeraldBright)
                        Text("${districtEntry.name(lang)} • ${activeDef.title(lang)} ($pct%)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("+${activeDef.xpReward} XP  •  +৳${activeDef.takaReward}", style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                        if (pct < 100) {
                            Text(
                                text = "${strings.missionLabel}: ${districtEntry.missionObjective(lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalCyan
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.startPlayableMode(GameModeId.SOLO) },
                                colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                modifier = Modifier.weight(1f)
                            ) { Text(strings.playAgain) }
                            OutlinedButton(onClick = onExitToLobby, modifier = Modifier.weight(1f)) { Text(strings.returnToLobby) }
                        }
                    }
                }
            }
        }

        // 11. Pause Menu
        if (showPauseMenu) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.pauseLabel, style = MaterialTheme.typography.headlineSmall)
                            Surface(onClick = { showPauseMenu = false }, color = Color.Transparent) {
                                Icon(Icons.Filled.Close, contentDescription = strings.closeLabel, tint = Color.White)
                            }
                        }
                        Button(onClick = { showPauseMenu = false }, colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg), modifier = Modifier.fillMaxWidth()) {
                            Text(strings.closeLabel)
                        }
                        OutlinedButton(onClick = { showPauseMenu = false; viewModel.startPlayableMode(mode) }, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.retryChallenge)
                        }
                        OutlinedButton(
                            onClick = {
                                if (hud.isMultiplayerSession) viewModel.socialDelegate.leaveRoom()
                                onExitToLobby()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pause_exit_lobby_button")
                        ) {
                            Text(strings.returnToLobby)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumableRow(name: String, count: Int, effect: String, useLabel: String, onUse: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("$name (x$count)", style = MaterialTheme.typography.titleSmall)
            Text(effect, style = MaterialTheme.typography.labelSmall, color = BengalEmeraldBright)
        }
        Button(onClick = onUse, enabled = count > 0, colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg)) {
            Text(useLabel)
        }
    }
}

@Composable
private fun WeaponSlotChip(label: String, selected: Boolean, testTag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) BengalEmerald.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) BengalEmeraldBright else TacticalSurfaceBorder),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) BengalEmeraldBright else Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun HoldableControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color,
    testTag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, color),
        modifier = Modifier
            .size(width = 76.dp, height = 56.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onHoldChanged(true)
                    waitForUpOrCancellation()
                    onHoldChanged(false)
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(30.dp))
            } else {
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HudPillButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (active) activeColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (active) activeColor else TacticalSurfaceBorder),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) activeColor else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun VirtualMoveThumbstick(
    modifier: Modifier = Modifier,
    onMoveChanged: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 90f

    Box(
        modifier = modifier
            .size(132.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knobOffset = Offset.Zero; onMoveChanged(0f, 0f) },
                    onDragCancel = { knobOffset = Offset.Zero; onMoveChanged(0f, 0f) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = hypot(raw.x, raw.y)
                        val clamped = if (dist > maxRadiusPx) Offset(raw.x * (maxRadiusPx / dist), raw.y * (maxRadiusPx / dist)) else raw
                        knobOffset = clamped
                        onMoveChanged((clamped.x / maxRadiusPx).coerceIn(-1f, 1f), (-clamped.y / maxRadiusPx).coerceIn(-1f, 1f))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = size.minDimension * 0.46f, center = center)
            drawCircle(color = BengalEmeraldBright.copy(alpha = 0.45f), radius = size.minDimension * 0.46f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            drawCircle(color = BengalEmeraldBright.copy(alpha = 0.85f), radius = 28f, center = center + knobOffset)
        }
    }
}
