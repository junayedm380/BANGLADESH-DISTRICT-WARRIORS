package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.ai.AiDifficulty
import com.example.game.characters.CharacterCatalog
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.multiplayer.NetworkSolutionEvaluation
import com.example.game.progression.ProgressionCalculator
import com.example.game.save.GraphicsQuality
import com.example.game.save.PlayerProgressState
import com.example.game.ui.GameViewModel
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onBack: () -> Unit
) {
    var resetMessage by remember { mutableStateOf(false) }
    val graphicsQuality = GraphicsQuality.fromId(progress.save.graphicsQualityId)
    val aiDifficulty = AiDifficulty.fromId(progress.save.aiDifficultyId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                }
                Spacer(Modifier.width(4.dp))
                Text(strings.settings, style = MaterialTheme.typography.headlineSmall)
            }

            SettingsCard {
                Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLanguage.entries.forEach { option ->
                        FilterChip(
                            selected = option == lang,
                            onClick = { viewModel.setLanguage(option) },
                            label = { Text(if (option == AppLanguage.ENGLISH) "English" else "বাংলা (Bengali)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                        )
                    }
                }
            }

            SettingsCard {
                Text(strings.aiDifficultyLabel, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = diff == aiDifficulty,
                            onClick = { viewModel.updateSettings(aiDifficulty = diff) },
                            label = { Text(diff.label(lang)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CrimsonStrike, selectedLabelColor = ObsidianBg)
                        )
                    }
                }
            }

            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.cameraSensitivity, style = MaterialTheme.typography.titleSmall)
                    Text("${"%.1f".format(progress.save.cameraSensitivity)}x", style = MaterialTheme.typography.labelLarge, color = BengalEmeraldBright)
                }
                Slider(
                    value = progress.save.cameraSensitivity,
                    onValueChange = { viewModel.updateSettings(sensitivity = it) },
                    valueRange = 0.4f..2.2f
                )
            }

            SettingsCard {
                Text(strings.graphicsQuality, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsQuality.entries.forEach { preset ->
                        FilterChip(
                            selected = preset == graphicsQuality,
                            onClick = { viewModel.updateSettings(graphicsQuality = preset) },
                            label = { Text(if (lang == AppLanguage.BENGALI) preset.labelBn else preset.labelEn) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DhakaAmber, selectedLabelColor = ObsidianBg)
                        )
                    }
                }
            }

            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.audioEnabledLabel, style = MaterialTheme.typography.titleSmall)
                    Switch(checked = progress.save.audioEnabled, onCheckedChange = { viewModel.updateSettings(audioEnabled = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.invertYAxis, style = MaterialTheme.typography.titleSmall)
                    Switch(checked = progress.save.invertYAxis, onCheckedChange = { viewModel.updateSettings(invertY = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.hapticFeedback, style = MaterialTheme.typography.titleSmall)
                    Switch(checked = progress.save.hapticsEnabled, onCheckedChange = { viewModel.updateSettings(hapticsEnabled = it) })
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.resetLocalSave()
                    resetMessage = true
                },
                border = BorderStroke(1.dp, CrimsonStrike),
                modifier = Modifier.fillMaxWidth().testTag("reset_save_button")
            ) {
                Text(strings.resetProgress, color = CrimsonStrike)
            }

            if (resetMessage) {
                Text(strings.resetConfirmNotice, style = MaterialTheme.typography.bodySmall, color = BengalEmeraldBright)
            }
        }
    }
}

@Composable
fun ProfileScreen(
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onBack: () -> Unit
) {
    val prog = ProgressionCalculator.compute(progress.save.xp)
    val charSlot = CharacterCatalog.getById(progress.save.selectedCharacterId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                }
                Spacer(Modifier.width(4.dp))
                Text(strings.profile, style = MaterialTheme.typography.headlineSmall)
            }

            SettingsCard {
                Text(progress.save.callsign, style = MaterialTheme.typography.headlineMedium, color = BengalEmeraldBright)
                Text("${strings.rankLabel}: ${prog.rank.title(lang)} (LVL ${prog.level})", style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                LinearProgressIndicator(
                    progress = { prog.progressFraction },
                    color = BengalEmeraldBright,
                    trackColor = TacticalSurfaceBorder,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Text("${prog.xpIntoLevel} / ${prog.xpForNextLevel} XP • Total ${prog.xp} XP", style = MaterialTheme.typography.labelSmall)
            }

            SettingsCard {
                Text("Selected Warrior: ${charSlot.displayName}", style = MaterialTheme.typography.titleSmall)
                Text("${strings.unlockedCountLabel}: ${progress.unlockedDistrictIds.size} / 64", style = MaterialTheme.typography.bodyMedium)
                Text("${strings.completedChallengesLabel}: ${progress.save.challengesClearedCount}", style = MaterialTheme.typography.bodyMedium)
                Text("${strings.targetsHitLabel}: ${progress.save.targetsEliminatedCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Taka Credits: ৳${progress.save.takaCredits}", style = MaterialTheme.typography.bodyMedium, color = DhakaAmber)
            }

            SettingsCard {
                Text(strings.cloudSyncLabel, style = MaterialTheme.typography.titleSmall, color = TacticalCyan)
                Text("Status: ${progress.syncStatus.name}", style = MaterialTheme.typography.labelSmall, color = DhakaAmber)
                Text(strings.cloudSyncOfflineNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ArchitectureDocsScreen(
    strings: GameStrings,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("docs_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                }
                Spacer(Modifier.width(4.dp))
                Text(strings.docsAndArchitecture, style = MaterialTheme.typography.headlineSmall)
            }

            SettingsCard {
                Text("Milestone 2 Deliverables (Implemented & Verified)", style = MaterialTheme.typography.titleMedium, color = BengalEmeraldBright)
                Text(
                    "• Advanced Player Controller: Walk, Run, Sprint, Jump, Crouch, Swimming (Buriganga Canal), and Vaulting over low cover.\n" +
                        "• Modular Weapon System: All 8 categories (26 original inspired weapons across Assault Rifles, SMGs, Snipers, DMRs, Shotguns, LMGs, Pistols, and Melee).\n" +
                        "• Combat & Attachments: 3-slot loadout switching, ADS scope zoom, Fire Modes (AUTO/SEMI/BURST), Recoil, Bullet Spread, Muzzle Flash, Impact Sparks, Procedural Audio SFX, Optics (Red Dot, Holo, 2x–8x), Suppressor, Compensator, Extended Mag, Foregrip.\n" +
                        "• Drivable Vehicles: Car, Motorcycle, Bus, Truck, and Boat with Entry/Exit, Acceleration/Brake/Reverse/Steer, Fuel, Vehicle Health, Chase Camera, and Engine Audio.\n" +
                        "• Intelligent Enemy AI: 8 states (Idle, Patrol, Detect, Chase, Attack, Retreat, Search, Dead) with NavGrid obstacle avoidance and Easy/Normal/Hard difficulty.\n" +
                        "• Ground Loot, Medkits, Armor & Training Ground Weapon Bench + AI Sparring Bot.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SettingsCard {
                Text("Milestone 3 Deliverables (Multiplayer, Social & Security)", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
                Text(
                    "• Loopback Authoritative TCP Server (127.0.0.1): Supports 4-player rooms, movement clamping, raycast hit verification, ammo checks, tamper rejection, and audit logs.\n" +
                        "• Matchmaking & Lobby: Quick Match, Public Room Browser, Private Room Codes, Ready/Unready, Team Alpha/Bravo assignment, and 4-Player Test Room launcher.\n" +
                        "• Player Accounts & Friends (Room v3): Salted PBKDF2 accounts (zero hardcoded credentials), stats/achievements, player search, requests, invites, room join, mute & moderation reports.\n" +
                        "• Comms & Reconnection: Global/Team/Private text chat, Vivox/Agora IVoiceChatProvider integration boundary, session token reconnect.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SettingsCard {
                Text("Milestone 4 Deliverables (64-District Open World & Streaming)", style = MaterialTheme.typography.titleMedium, color = BengalEmeraldBright)
                Text(
                    "• Complete 64-District Interactive Bangladesh Map: All 8 Divisions, Jamuna–Padma–Meghna deltas, road/bridge/river routes, pinch-zoom & division filtering.\n" +
                        "• Configurable Sequential Unlocking (Room v4): First district (Dhaka) unlocked; completing both District Mission + District Challenge (100%) unlocks the next district.\n" +
                        "• 10 Streamed Regional 3D Biomes: Dhaka Metropolis, Gazipur Industrial/Forest, Chattogram Port & Hills, Cox's Bazar Marine Beach, Sylhet Tea Highlands, Khulna Sundarbans Mangroves, Rajshahi Padma Plains, Rangpur Agro Villages, Barishal Floating Canals, and Mymensingh Brahmaputra Bank (remaining 54 districts honestly tagged STAGED EXPANSION).\n" +
                        "• Async WorldStreamingManager & Performance: Single-active + bounded LRU cache unloading, LOD distance/frustum chunk culling, Projectile/Impact ObjectPools, and scalable quality profiles for Android, Windows, and iOS.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SettingsCard {
                Text("Multiplayer Architecture Evaluation", style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                NetworkSolutionEvaluation.entries.forEach { candidate ->
                    Text("${candidate.engineName}: ${candidate.suitabilityNote}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, TacticalSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
