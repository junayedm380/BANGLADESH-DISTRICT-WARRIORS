package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.characters.AccessoryOption
import com.example.game.characters.CharacterCatalog
import com.example.game.characters.CharacterColorTint
import com.example.game.characters.EmoteAction
import com.example.game.characters.HeadwearOption
import com.example.game.characters.OutfitStyle
import com.example.game.districts.DistrictCatalog
import com.example.game.engine.GlRenderMode
import com.example.game.inventory.InventoryCatalog
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.progression.ProgressionCalculator
import com.example.game.save.GraphicsQuality
import com.example.game.save.PlayerProgressState
import com.example.game.ui.GameViewModel
import com.example.game.ui.components.GlViewport
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder
import com.example.ui.theme.TacticalSurfaceElevated

private enum class CustomizerTab { OUTFIT, HEADWEAR, ACCESSORY, COLOR, EMOTE }

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onPlaySolo: () -> Unit,
    onPlayTrainingGround: () -> Unit,
    onOpenDistricts: () -> Unit,
    onOpenModes: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenMultiplayer: () -> Unit = {},
    onOpenSocial: () -> Unit = {}
) {
    LaunchedEffect(
        progress.save.selectedCharacterId,
        progress.save.selectedOutfitId,
        progress.save.selectedHeadwearId,
        progress.save.selectedAccessoryId,
        progress.save.selectedColorTintId,
        progress.save.selectedEmoteId
    ) {
        viewModel.syncShowcaseState()
    }

    val selectedChar = CharacterCatalog.getById(progress.save.selectedCharacterId)
    val selectedOutfit = OutfitStyle.fromId(progress.save.selectedOutfitId)
    val selectedHeadwear = HeadwearOption.fromId(progress.save.selectedHeadwearId)
    val selectedAccessory = AccessoryOption.fromId(progress.save.selectedAccessoryId)
    val selectedTint = CharacterColorTint.fromId(progress.save.selectedColorTintId)
    val selectedEmote = EmoteAction.fromId(progress.save.selectedEmoteId)
    val progSummary = ProgressionCalculator.compute(progress.save.xp)
    val graphicsQuality = GraphicsQuality.fromId(progress.save.graphicsQualityId)

    var customizerTab by remember { mutableStateOf(CustomizerTab.OUTFIT) }

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_warrior_emblem_1795446926345),
                        contentDescription = strings.gameTitle,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BengalEmeraldBright.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.gameTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.gameSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = DhakaAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { viewModel.toggleLanguage() },
                        color = TacticalSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BengalEmeraldBright.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = strings.languageLabel, modifier = Modifier.size(16.dp), tint = BengalEmeraldBright)
                            Text(lang.displayLabel, style = MaterialTheme.typography.labelLarge, color = BengalEmeraldBright)
                        }
                    }

                    IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("profile_button")) {
                        Icon(Icons.Filled.Person, contentDescription = strings.profile, tint = TacticalCyan)
                    }
                    IconButton(onClick = onOpenDocs, modifier = Modifier.testTag("docs_button")) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = strings.docsAndArchitecture, tint = DhakaAmber)
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Filled.Settings, contentDescription = strings.settings, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Status / Loadout Strip
            Surface(
                color = TacticalSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val activeDist = DistrictCatalog.getById(progress.currentDistrictId)
                        Text(
                            text = "${progress.save.callsign} • LVL ${progSummary.level} • ${activeDist.name(lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = BengalEmeraldBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${progSummary.xp} XP • ৳${progress.save.takaCredits} • ${InventoryCatalog.getWeapon(progress.save.selectedWeaponId).name(lang)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(color = DhakaAmber.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "M4 WORLD",
                            style = MaterialTheme.typography.labelSmall,
                            color = DhakaAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3D Character Display & Modular Customizer
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(315.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                viewModel.simulation.rotateShowcase(dragAmount)
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dhaka_hero_1795446926341),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.22f
                    )

                    GlViewport(
                        simulation = viewModel.simulation,
                        renderMode = GlRenderMode.LOBBY_SHOWCASE,
                        graphicsQuality = graphicsQuality,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent, Color.Black.copy(alpha = 0.84f))
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedChar.displayName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(selectedChar.role(lang), style = MaterialTheme.typography.bodySmall, color = BengalEmeraldBright)
                        }
                        Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = strings.dragToRotate3d,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Bottom Customization Deck (Outfit, Headwear, Accessory, Color, Emote)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val tabs = listOf(
                                CustomizerTab.OUTFIT to strings.outfitLabel,
                                CustomizerTab.HEADWEAR to strings.headwearLabel,
                                CustomizerTab.ACCESSORY to strings.accessoryLabel,
                                CustomizerTab.COLOR to strings.colorTintLabel,
                                CustomizerTab.EMOTE to strings.emoteLabel
                            )
                            items(tabs) { (tab, label) ->
                                Surface(
                                    onClick = { customizerTab = tab },
                                    color = if (customizerTab == tab) DhakaAmber else Color.Black.copy(alpha = 0.62f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (customizerTab == tab) ObsidianBg else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            when (customizerTab) {
                                CustomizerTab.OUTFIT -> items(OutfitStyle.entries) { outfit ->
                                    FilterChip(
                                        selected = outfit == selectedOutfit,
                                        onClick = { viewModel.selectOutfit(outfit) },
                                        label = { Text(outfit.label(lang), style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                    )
                                }
                                CustomizerTab.HEADWEAR -> items(HeadwearOption.entries) { hw ->
                                    FilterChip(
                                        selected = hw == selectedHeadwear,
                                        onClick = { viewModel.selectHeadwear(hw) },
                                        label = { Text(hw.label(lang), style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                    )
                                }
                                CustomizerTab.ACCESSORY -> items(AccessoryOption.entries) { acc ->
                                    FilterChip(
                                        selected = acc == selectedAccessory,
                                        onClick = { viewModel.selectAccessory(acc) },
                                        label = { Text(acc.label(lang), style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                    )
                                }
                                CustomizerTab.COLOR -> items(CharacterColorTint.entries) { tint ->
                                    FilterChip(
                                        selected = tint == selectedTint,
                                        onClick = { viewModel.selectColorTint(tint) },
                                        label = { Text(tint.labelEn, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                    )
                                }
                                CustomizerTab.EMOTE -> items(EmoteAction.entries) { emote ->
                                    FilterChip(
                                        selected = emote == selectedEmote,
                                        onClick = { viewModel.selectEmote(emote) },
                                        label = { Text(emote.label(lang), style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                    )
                                }
                            }
                        }

                        Text(
                            text = strings.placeholderNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4 Fictional Character Slots
            Text(strings.characterSlotsTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CharacterCatalog.slots) { slot ->
                    val isSelected = slot.id == selectedChar.id
                    Card(
                        onClick = { viewModel.selectCharacter(slot.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) BengalEmerald.copy(alpha = 0.16f) else TacticalSurface
                        ),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BengalEmeraldBright else TacticalSurfaceBorder),
                        modifier = Modifier.width(165.dp).testTag("character_slot_${slot.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(slot.defaultOutfit.primaryRgb.first, slot.defaultOutfit.primaryRgb.second, slot.defaultOutfit.primaryRgb.third))
                                )
                                Text("#${slot.slotNumber}", style = MaterialTheme.typography.labelSmall, color = DhakaAmber)
                            }
                            Text(slot.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(slot.role(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Primary Play Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlaySolo,
                    colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 14.dp),
                    modifier = Modifier.weight(1.2f).testTag("play_solo_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.playSolo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onPlayTrainingGround,
                    border = BorderStroke(1.dp, DhakaAmber),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                    modifier = Modifier.weight(1f).testTag("training_ground_button")
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = DhakaAmber)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.trainingGround, style = MaterialTheme.typography.labelLarge, color = DhakaAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Multiplayer & Social Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LobbyNavTile(
                    icon = Icons.Filled.Groups,
                    label = strings.multiplayerHub,
                    sublabel = "127.0.0.1 TEST",
                    accent = BengalEmeraldBright,
                    modifier = Modifier.weight(1f).testTag("multiplayer_entry_button"),
                    onClick = onOpenMultiplayer
                )
                LobbyNavTile(
                    icon = Icons.Filled.Group,
                    label = strings.friendsAndAccount,
                    sublabel = "ID & FRIENDS",
                    accent = TacticalCyan,
                    modifier = Modifier.weight(1f).testTag("social_entry_button"),
                    onClick = onOpenSocial
                )
            }

            // Secondary Entries
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LobbyNavTile(
                    icon = Icons.Filled.Explore,
                    label = strings.districts,
                    sublabel = "${progress.unlockedDistrictIds.size}/64",
                    accent = BengalEmeraldBright,
                    modifier = Modifier.weight(1f).testTag("districts_entry_button"),
                    onClick = onOpenDistricts
                )
                LobbyNavTile(
                    icon = Icons.Filled.SportsEsports,
                    label = strings.gameModes,
                    sublabel = "8 / 10",
                    accent = TacticalCyan,
                    modifier = Modifier.weight(1f).testTag("modes_entry_button"),
                    onClick = onOpenModes
                )
                LobbyNavTile(
                    icon = Icons.Filled.Backpack,
                    label = strings.inventory,
                    sublabel = "${InventoryCatalog.weapons.size} Weapons",
                    accent = DhakaAmber,
                    modifier = Modifier.weight(1f).testTag("inventory_entry_button"),
                    onClick = onOpenInventory
                )
            }

            Text(
                text = strings.offlineModeNotice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun LobbyNavTile(
    icon: ImageVector,
    label: String,
    sublabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = TacticalSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TacticalSurfaceBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}
