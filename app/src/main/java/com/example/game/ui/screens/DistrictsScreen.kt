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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.districts.DistrictCatalog
import com.example.game.districts.DistrictEntry
import com.example.game.districts.Division
import com.example.game.districts.SequentialUnlockConfig
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.save.PlayerProgressState
import com.example.game.world.DistrictTravelGraph
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder

private enum class DistrictViewTab { WORLD_MAP, DIRECTORY_LIST }

@Composable
fun DistrictsScreen(
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onTravelToDistrict: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(DistrictViewTab.WORLD_MAP) }
    var selectedDivision by remember { mutableStateOf<Division?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrictId by remember { mutableStateOf(progress.currentDistrictId) }

    val selectedDistrict = remember(selectedDistrictId) { DistrictCatalog.getById(selectedDistrictId) }

    val filteredDistricts = remember(selectedDivision, searchQuery) {
        DistrictCatalog.allDistricts.filter { entry ->
            val matchesDiv = selectedDivision == null || entry.division == selectedDivision
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                entry.nameEn.lowercase().contains(q) ||
                entry.nameBn.contains(searchQuery.trim()) ||
                entry.challengeTitleEn.lowercase().contains(q) ||
                entry.biome.labelEn.lowercase().contains(q)
            matchesDiv && matchesSearch
        }
    }

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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("districts_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.districts, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${progress.unlockedDistrictIds.size}/64 ${strings.unlockedCountLabel} • ${DistrictCatalog.playableDistricts.size}/64 3D",
                        style = MaterialTheme.typography.bodySmall,
                        color = BengalEmeraldBright
                    )
                }
            }

            // View Mode Tabs: Interactive Map vs 64 District Directory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == DistrictViewTab.WORLD_MAP,
                    onClick = { activeTab = DistrictViewTab.WORLD_MAP },
                    leadingIcon = { Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.worldMapTab) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmeraldBright, selectedLabelColor = ObsidianBg),
                    modifier = Modifier.testTag("tab_world_map")
                )
                FilterChip(
                    selected = activeTab == DistrictViewTab.DIRECTORY_LIST,
                    onClick = { activeTab = DistrictViewTab.DIRECTORY_LIST },
                    leadingIcon = { Icon(Icons.Filled.TravelExplore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.districtListTab) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TacticalCyan, selectedLabelColor = ObsidianBg),
                    modifier = Modifier.testTag("tab_district_list")
                )
            }

            Spacer(Modifier.height(6.dp))

            // 8 Division Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDivision == null,
                        onClick = { selectedDivision = null },
                        label = { Text(strings.allDivisions) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BengalEmerald,
                            selectedLabelColor = ObsidianBg
                        )
                    )
                }
                items(Division.entries) { division ->
                    FilterChip(
                        selected = selectedDivision == division,
                        onClick = { selectedDivision = division },
                        label = { Text(division.label(lang)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BengalEmerald,
                            selectedLabelColor = ObsidianBg
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (activeTab == DistrictViewTab.WORLD_MAP) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        BangladeshWorldMapCard(
                            districts = filteredDistricts,
                            selectedDistrict = selectedDistrict,
                            selectedDivision = selectedDivision,
                            progress = progress,
                            lang = lang,
                            onSelectDistrict = { selectedDistrictId = it.id }
                        )
                    }
                    item {
                        DistrictInspectorCard(
                            district = selectedDistrict,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onFastTravel = { onTravelToDistrict(selectedDistrict.id, false) },
                            onDeploy = { onTravelToDistrict(selectedDistrict.id, true) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchDistrictHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("district_search_input")
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredDistricts, key = { it.id }) { district ->
                        DistrictDirectoryRowCard(
                            district = district,
                            progress = progress,
                            strings = strings,
                            lang = lang,
                            onSelectOnMap = {
                                selectedDistrictId = district.id
                                activeTab = DistrictViewTab.WORLD_MAP
                            },
                            onDeploy = { onTravelToDistrict(district.id, true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictInspectorCard(
    district: DistrictEntry,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onFastTravel: () -> Unit,
    onDeploy: () -> Unit
) {
    val isUnlocked = district.id in progress.unlockedDistrictIds
    val isCurrent = district.id == progress.currentDistrictId
    val missionDone = progress.isMissionDone(district.id)
    val challengeDone = progress.isChallengeDone(district.id)
    val completionPct = progress.completionPercent(district.id)
    val prereqId = SequentialUnlockConfig.prerequisiteDistrictId(district.id, progress.districtOrder)
    val prereqEntry = prereqId?.let { DistrictCatalog.getById(it) }
    val connectedRoutes = remember(district.id) { DistrictTravelGraph.getConnectionsFrom(district.id) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(
            1.5.dp,
            if (isCurrent) DhakaAmber else if (district.isEnvironmentReady) BengalEmeraldBright else TacticalSurfaceBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("district_inspector_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (district.id == "dhaka") {
                Image(
                    painter = painterResource(id = R.drawable.img_dhaka_hero_1795446926341),
                    contentDescription = district.name(lang),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${district.unlockOrder} • ${district.name(lang)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${district.division.label(lang)} • ${district.biome.label(lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalCyan
                    )
                }
                Surface(
                    color = if (district.isEnvironmentReady) BengalEmerald.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = district.buildStatus.label(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (district.isEnvironmentReady) BengalEmeraldBright else DhakaAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${strings.landmarkLabel}: ${district.landmark(lang)}",
                style = MaterialTheme.typography.labelMedium,
                color = DhakaAmber
            )
            Text(
                text = district.description(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Completion percentage bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.completionLabel, style = MaterialTheme.typography.labelSmall)
                    Text("$completionPct%", style = MaterialTheme.typography.labelSmall, color = BengalEmeraldBright)
                }
                LinearProgressIndicator(
                    progress = { completionPct / 100f },
                    color = BengalEmeraldBright,
                    trackColor = TacticalSurfaceBorder,
                    modifier = Modifier.fillMaxWidth().height(7.dp)
                )
            }

            // Mission + Challenge Checklist
            Surface(
                color = ObsidianBg.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${if (missionDone) "✓" else "○"} ${strings.missionLabel}: ${district.missionTitle(lang)} (+${district.missionXpReward} XP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (missionDone) BengalEmeraldBright else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "   ${district.missionObjective(lang)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${if (challengeDone) "✓" else "○"} ${strings.challengeLabel}: ${district.challengeTitle(lang)} (+${district.xpReward} XP • ৳${district.takaReward})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (challengeDone) BengalEmeraldBright else DhakaAmber
                    )
                }
            }

            if (prereqEntry != null && !isUnlocked) {
                Text(
                    text = "${strings.prerequisiteLabel}: ${prereqEntry.name(lang)} (100%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (connectedRoutes.isNotEmpty()) {
                Text(
                    text = "${strings.connectedRoutesLabel}: ${
                        connectedRoutes.take(3).joinToString(" • ") {
                            "${DistrictCatalog.getById(it.toId).name(lang)} (${it.distanceKm}km)"
                        }
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onFastTravel,
                    enabled = isUnlocked && !isCurrent,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("fast_travel_district_button")
                ) {
                    Text(if (isCurrent) strings.activeDistrictLabel else strings.fastTravelLabel)
                }

                Button(
                    onClick = onDeploy,
                    enabled = isUnlocked && district.isEnvironmentReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BengalEmeraldBright,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier.weight(1.2f).heightIn(min = 48.dp).testTag("deploy_district_button")
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Filled.PlayArrow else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (district.isEnvironmentReady) strings.deployToDistrictLabel else strings.comingSoon)
                }
            }
        }
    }
}

@Composable
private fun DistrictDirectoryRowCard(
    district: DistrictEntry,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onSelectOnMap: () -> Unit,
    onDeploy: () -> Unit
) {
    val isUnlocked = district.id in progress.unlockedDistrictIds
    val completionPct = progress.completionPercent(district.id)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, if (district.isEnvironmentReady) BengalEmeraldBright else TacticalSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${district.unlockOrder} • ${district.name(lang)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${district.division.label(lang)} • ${district.biome.label(lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalCyan
                    )
                }

                Surface(
                    color = when {
                        completionPct == 100 -> BengalEmerald.copy(alpha = 0.25f)
                        district.isEnvironmentReady -> TacticalCyan.copy(alpha = 0.18f)
                        isUnlocked -> DhakaAmber.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                completionPct == 100 -> Icons.Filled.CheckCircle
                                isUnlocked -> Icons.Filled.PlayArrow
                                else -> Icons.Filled.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (district.isEnvironmentReady) BengalEmeraldBright else DhakaAmber
                        )
                        Text(
                            text = "${district.buildStatus.label(lang)} • $completionPct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (district.isEnvironmentReady) BengalEmeraldBright else DhakaAmber
                        )
                    }
                }
            }

            Text(
                text = "${strings.missionLabel}: ${district.missionTitle(lang)} • ${strings.challengeLabel}: ${district.challengeTitle(lang)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectOnMap,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.worldMapTab)
                }

                if (district.isEnvironmentReady && isUnlocked) {
                    Button(
                        onClick = onDeploy,
                        colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("launch_${district.id}_district_button")
                    ) {
                        Text(strings.playSolo)
                    }
                }
            }
        }
    }
}
