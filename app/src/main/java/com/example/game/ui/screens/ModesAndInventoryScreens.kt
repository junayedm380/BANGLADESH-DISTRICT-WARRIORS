package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.inventory.InventoryCatalog
import com.example.game.inventory.MagazineAttachment
import com.example.game.inventory.MuzzleAttachment
import com.example.game.inventory.OpticAttachment
import com.example.game.inventory.UnderbarrelAttachment
import com.example.game.inventory.WeaponCategory
import com.example.game.inventory.WeaponSlotType
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.modes.GameModeId
import com.example.game.save.PlayerProgressState
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder

@Composable
fun ModesScreen(
    strings: GameStrings,
    lang: AppLanguage,
    onLaunchMode: (GameModeId) -> Unit,
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
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("modes_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                }
                Spacer(Modifier.width(4.dp))
                Text(strings.gameModes, style = MaterialTheme.typography.headlineSmall)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Image(
                        painter = painterResource(id = R.drawable.img_training_ground_1795446926840),
                        contentDescription = strings.trainingGround,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                items(GameModeId.entries) { mode ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                        border = BorderStroke(1.dp, if (mode.isPlayableNow) BengalEmeraldBright else TacticalSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.title(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (mode.isPlayableNow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = if (mode.isPlayableNow) BengalEmerald.copy(alpha = 0.2f) else DhakaAmber.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (mode.isPlayableNow) strings.playableNow else strings.comingSoon,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mode.isPlayableNow) BengalEmeraldBright else DhakaAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(mode.description(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { onLaunchMode(mode) },
                                enabled = mode.isPlayableNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BengalEmeraldBright,
                                    contentColor = ObsidianBg,
                                    disabledContainerColor = TacticalSurfaceBorder,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("mode_launch_${mode.id}")
                            ) {
                                Icon(if (mode.isPlayableNow) Icons.Filled.PlayArrow else Icons.Filled.Lock, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (mode.isPlayableNow) strings.playableNow else strings.comingSoon)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryScreen(
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onSelectWeapon: (String, WeaponSlotType) -> Unit,
    onUpdateAttachments: (OpticAttachment?, MuzzleAttachment?, MagazineAttachment?, UnderbarrelAttachment?) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(WeaponCategory.ASSAULT_RIFLE) }
    var showAttachmentsPanel by remember { mutableStateOf(false) }

    val primaryW = InventoryCatalog.getWeapon(progress.save.selectedWeaponId)
    val secondaryW = InventoryCatalog.getWeapon(progress.save.secondaryWeaponId)
    val meleeW = InventoryCatalog.getWeapon(progress.save.meleeWeaponId)
    val curAttachments = progress.save.attachments()

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
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("inventory_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(strings.inventory, style = MaterialTheme.typography.headlineSmall)
                }

                FilterChip(
                    selected = showAttachmentsPanel,
                    onClick = { showAttachmentsPanel = !showAttachmentsPanel },
                    leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null) },
                    label = { Text(strings.attachmentsTitle) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DhakaAmber, selectedLabelColor = ObsidianBg),
                    modifier = Modifier.testTag("toggle_attachments_button")
                )
            }

            // Active 3-Slot Loadout Summary
            Surface(
                color = TacticalSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${strings.equipPrimary}: ${primaryW.name(lang)}", style = MaterialTheme.typography.labelMedium, color = BengalEmeraldBright)
                    Text("${strings.equipSecondary}: ${secondaryW.name(lang)}", style = MaterialTheme.typography.labelMedium, color = DhakaAmber)
                    Text("${strings.equipMelee}: ${meleeW.name(lang)}", style = MaterialTheme.typography.labelMedium, color = TacticalCyan)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (showAttachmentsPanel) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        AttachmentGroupCard("Optic / Scope", OpticAttachment.entries.map { it to it.label(lang) }, curAttachments.optic) {
                            onUpdateAttachments(it, null, null, null)
                        }
                    }
                    item {
                        AttachmentGroupCard("Muzzle", MuzzleAttachment.entries.map { it to it.label(lang) }, curAttachments.muzzle) {
                            onUpdateAttachments(null, it, null, null)
                        }
                    }
                    item {
                        AttachmentGroupCard("Magazine", MagazineAttachment.entries.map { it to it.label(lang) }, curAttachments.magazine) {
                            onUpdateAttachments(null, null, it, null)
                        }
                    }
                    item {
                        AttachmentGroupCard("Underbarrel", UnderbarrelAttachment.entries.map { it to it.label(lang) }, curAttachments.underbarrel) {
                            onUpdateAttachments(null, null, null, it)
                        }
                    }
                }
            } else {
                // 8 Weapon Category Tabs
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WeaponCategory.entries) { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label(lang)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg),
                            modifier = Modifier.testTag("weapon_cat_${cat.id}")
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val weaponsInCategory = InventoryCatalog.byCategory(selectedCategory)
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(weaponsInCategory, key = { it.id }) { weapon ->
                        val targetSlot = when (weapon.category) {
                            WeaponCategory.PISTOL -> WeaponSlotType.SECONDARY
                            WeaponCategory.MELEE -> WeaponSlotType.MELEE
                            else -> WeaponSlotType.PRIMARY
                        }
                        val isEquipped = when (targetSlot) {
                            WeaponSlotType.PRIMARY -> weapon.id == progress.save.selectedWeaponId
                            WeaponSlotType.SECONDARY -> weapon.id == progress.save.secondaryWeaponId
                            WeaponSlotType.MELEE -> weapon.id == progress.save.meleeWeaponId
                        }
                        val accent = Color(weapon.tracerRgb.first, weapon.tracerRgb.second, weapon.tracerRgb.third)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, if (isEquipped) BengalEmeraldBright else TacticalSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(weapon.name(lang), style = MaterialTheme.typography.titleMedium)
                                        Text(weapon.categoryLabel(lang), style = MaterialTheme.typography.bodySmall, color = accent)
                                    }
                                    Button(
                                        onClick = { onSelectWeapon(weapon.id, targetSlot) },
                                        enabled = !isEquipped,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BengalEmeraldBright,
                                            contentColor = ObsidianBg,
                                            disabledContainerColor = BengalEmerald.copy(alpha = 0.25f),
                                            disabledContentColor = BengalEmeraldBright
                                        ),
                                        modifier = Modifier.testTag("equip_weapon_${weapon.id}")
                                    ) {
                                        if (isEquipped) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.equipped)
                                        } else {
                                            val slotLabel = when (targetSlot) {
                                                WeaponSlotType.PRIMARY -> strings.equipPrimary
                                                WeaponSlotType.SECONDARY -> strings.equipSecondary
                                                WeaponSlotType.MELEE -> strings.equipMelee
                                            }
                                            Text(slotLabel)
                                        }
                                    }
                                }

                                StatBarRow(strings.damageStat, "${weapon.damage}", weapon.damage / 115f, accent)
                                StatBarRow(strings.fireRateStat, "${1000 / weapon.fireCooldownMs}/s", (500f / weapon.fireCooldownMs).coerceIn(0.15f, 1f), TacticalCyan)
                                if (!weapon.isMelee) {
                                    val effMag = if (weapon.supportsAttachments) curAttachments.effectiveMagSize(weapon.magSize) else weapon.magSize
                                    StatBarRow(strings.magSizeStat, "$effMag", effMag / 100f, DhakaAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> AttachmentGroupCard(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, TacticalSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = DhakaAmber)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { (item, label) ->
                    FilterChip(
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBarRow(label: String, value: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = color,
            trackColor = TacticalSurfaceBorder,
            modifier = Modifier.weight(1f).height(6.dp)
        )
        Text(value, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(42.dp))
    }
}
