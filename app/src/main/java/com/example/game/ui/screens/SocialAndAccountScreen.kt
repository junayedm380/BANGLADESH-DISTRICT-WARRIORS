package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.multiplayer.ReportReason
import com.example.game.progression.ProgressionCalculator
import com.example.game.save.PlayerProgressState
import com.example.game.social.AchievementEvaluator
import com.example.game.social.FriendEntryUi
import com.example.game.social.FriendStatus
import com.example.game.social.PresenceStatus
import com.example.game.ui.GameViewModel
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder

private enum class SocialTab { ACCOUNT, FRIENDS }

@Composable
fun SocialAndAccountScreen(
    viewModel: GameViewModel,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onJoinFriendRoom: (String) -> Unit,
    onBack: () -> Unit
) {
    val delegate = viewModel.socialDelegate
    val activeAccount by delegate.activeAccount.collectAsStateWithLifecycle()
    val friends by delegate.friends.collectAsStateWithLifecycle()
    val searchResults by delegate.searchResults.collectAsStateWithLifecycle()
    val authMessage by delegate.authStatusMessage.collectAsStateWithLifecycle()
    val activeRoom by viewModel.multiplayerCoordinator.activeRoomFlow.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(SocialTab.ACCOUNT) }
    var isRegisterMode by remember { mutableStateOf(true) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<FriendEntryUi?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .align(Alignment.TopCenter)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("social_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(strings.friendsAndAccount, style = MaterialTheme.typography.headlineSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentTab == SocialTab.ACCOUNT,
                        onClick = { currentTab = SocialTab.ACCOUNT },
                        label = { Text("ID & STATS", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg),
                        modifier = Modifier.testTag("tab_account")
                    )
                    FilterChip(
                        selected = currentTab == SocialTab.FRIENDS,
                        onClick = { currentTab = SocialTab.FRIENDS },
                        label = { Text("${strings.friendsTab} (${friends.count { it.status == FriendStatus.ACCEPTED }})", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TacticalCyan, selectedLabelColor = ObsidianBg),
                        modifier = Modifier.testTag("tab_friends")
                    )
                }
            }

            if (authMessage != null) {
                Surface(
                    color = TacticalCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TacticalCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(authMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                        TextButton(onClick = { delegate.clearAuthMessage() }) { Text(strings.closeLabel) }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentTab == SocialTab.ACCOUNT) {
                    val acc = activeAccount
                    if (acc == null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = isRegisterMode,
                                            onClick = { isRegisterMode = true },
                                            label = { Text(strings.registerTab) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                        )
                                        FilterChip(
                                            selected = !isRegisterMode,
                                            onClick = { isRegisterMode = false },
                                            label = { Text(strings.loginTab) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TacticalCyan, selectedLabelColor = ObsidianBg)
                                        )
                                    }
                                    Text(
                                        "Local salted PBKDF2 Room v3 accounts (External PlayFab/ASP.NET Core backend unconfigured).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = usernameInput,
                                        onValueChange = { usernameInput = it.take(20) },
                                        label = { Text(strings.usernameHint) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                                    )
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it.take(64) },
                                        label = { Text(strings.passwordHint) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                                    )
                                    Button(
                                        onClick = {
                                            if (isRegisterMode) {
                                                delegate.registerAccount(usernameInput, passwordInput)
                                            } else {
                                                delegate.loginAccount(usernameInput, passwordInput)
                                            }
                                            passwordInput = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_submit_button")
                                    ) {
                                        Text(if (isRegisterMode) strings.registerTab else strings.loginTab)
                                    }
                                }
                            }
                        }
                    } else {
                        val prog = ProgressionCalculator.compute(maxOf(acc.xp, progress.save.xp))
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = BorderStroke(1.dp, BengalEmeraldBright),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(acc.username, style = MaterialTheme.typography.headlineSmall, color = BengalEmeraldBright)
                                            Text("${strings.playerIdLabel}: ${acc.playerId}", style = MaterialTheme.typography.labelMedium, color = DhakaAmber)
                                        }
                                        OutlinedButton(onClick = { delegate.logoutAccount() }, modifier = Modifier.testTag("auth_logout_button")) {
                                            Text(strings.logoutLabel, color = CrimsonStrike)
                                        }
                                    }
                                    Text("${strings.rankLabel}: ${prog.rank.title(lang)} (LVL ${prog.level}) • ${prog.xp} XP", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${strings.statsLabel}: Matches ${acc.matchesPlayed} • Wins ${acc.matchesWon} • Eliminations ${maxOf(acc.eliminations, progress.save.targetsEliminatedCount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TacticalCyan
                                    )
                                }
                            }
                        }

                        item {
                            Text(strings.achievementsLabel, style = MaterialTheme.typography.titleMedium, color = DhakaAmber)
                        }

                        val achievements = AchievementEvaluator.evaluate(
                            xp = maxOf(acc.xp, progress.save.xp),
                            matchesPlayed = acc.matchesPlayed,
                            matchesWon = acc.matchesWon,
                            eliminations = maxOf(acc.eliminations, progress.save.targetsEliminatedCount),
                            unlockedDistrictsCount = progress.unlockedDistrictIds.size,
                            acceptedFriendsCount = friends.count { it.status == FriendStatus.ACCEPTED }
                        )
                        items(achievements, key = { it.id }) { ach ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = BorderStroke(1.dp, if (ach.isUnlocked) BengalEmeraldBright else TacticalSurfaceBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (ach.isUnlocked) DhakaAmber else Color.Gray)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ach.title(lang), style = MaterialTheme.typography.titleSmall, color = if (ach.isUnlocked) Color.White else Color.Gray)
                                        Text(ach.description(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(if (ach.isUnlocked) "UNLOCKED" else "LOCKED", style = MaterialTheme.typography.labelSmall, color = if (ach.isUnlocked) BengalEmeraldBright else Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    // FRIENDS & SEARCH TAB
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                delegate.refreshSearch(it)
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            placeholder = { Text(strings.searchPlayersHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("friend_search_input")
                        )
                    }

                    val pendingIncoming = friends.filter { it.status == FriendStatus.PENDING_INCOMING }
                    val accepted = friends.filter { it.status == FriendStatus.ACCEPTED }
                    val outgoing = friends.filter { it.status == FriendStatus.PENDING_OUTGOING }

                    if (pendingIncoming.isNotEmpty()) {
                        item { Text("Incoming Requests (${pendingIncoming.size})", style = MaterialTheme.typography.titleSmall, color = DhakaAmber) }
                        items(pendingIncoming, key = { "in_${it.playerId}" }) { f ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = BorderStroke(1.dp, DhakaAmber),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(f.username, style = MaterialTheme.typography.titleSmall)
                                        Text(f.playerId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { delegate.acceptFriendRequest(f.playerId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                            modifier = Modifier.testTag("accept_friend_${f.playerId}")
                                        ) { Text(strings.acceptLabel) }
                                        OutlinedButton(onClick = { delegate.rejectOrRemoveFriend(f.playerId) }) { Text(strings.rejectLabel) }
                                    }
                                }
                            }
                        }
                    }

                    if (accepted.isNotEmpty()) {
                        item { Text("${strings.friendsTab} (${accepted.size})", style = MaterialTheme.typography.titleSmall, color = BengalEmeraldBright) }
                        items(accepted, key = { "fr_${it.playerId}" }) { f ->
                            val dotColor = when (f.presence) {
                                PresenceStatus.ONLINE, PresenceStatus.IN_ROOM -> BengalEmeraldBright
                                PresenceStatus.IN_MATCH -> DhakaAmber
                                PresenceStatus.OFFLINE -> Color.Gray
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                                border = BorderStroke(1.dp, TacticalSurfaceBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                                            Column {
                                                Text(f.username, style = MaterialTheme.typography.titleSmall)
                                                Text("${f.presence.label(lang)}${if (f.activeRoomCode != null) " • Room ${f.activeRoomCode}" else ""}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                                            }
                                        }
                                        IconButton(onClick = { reportTarget = f }) {
                                            Icon(Icons.Filled.Flag, contentDescription = strings.reportLabel, tint = CrimsonStrike, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (f.activeRoomCode != null) {
                                            Button(
                                                onClick = {
                                                    delegate.joinRoomByCode(f.activeRoomCode, progress.save)
                                                    onJoinFriendRoom(f.activeRoomCode)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg)
                                            ) { Text(strings.joinFriendRoomLabel, style = MaterialTheme.typography.labelSmall) }
                                        }
                                        if (activeRoom != null) {
                                            OutlinedButton(onClick = { delegate.inviteFriendToActiveRoom(f.playerId, f.username) }) {
                                                Text("INVITE", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        OutlinedButton(onClick = { delegate.toggleMuteFriend(f.playerId) }) {
                                            Text(if (f.isMuted) strings.unmuteLabel else strings.muteLabel, style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(onClick = { delegate.rejectOrRemoveFriend(f.playerId) }) {
                                            Text(strings.removeLabel, style = MaterialTheme.typography.labelSmall, color = CrimsonStrike)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (outgoing.isNotEmpty()) {
                        item { Text("Sent Requests (${outgoing.size})", style = MaterialTheme.typography.labelLarge, color = TacticalCyan) }
                        items(outgoing, key = { "out_${it.playerId}" }) { f ->
                            Text("• ${f.username} (Pending)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    item { Text("Discover Players (Local Directory)", style = MaterialTheme.typography.titleSmall, color = TacticalCyan) }
                    val existingIds = friends.map { it.playerId }.toSet()
                    items(searchResults.filter { it.playerId !in existingIds }, key = { "dir_${it.playerId}" }) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, TacticalSurfaceBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.username, style = MaterialTheme.typography.titleSmall)
                                    Text("${entry.rankLabel} • ${entry.xp} XP • ${entry.presence.label(lang)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = { delegate.sendFriendRequest(entry.playerId, entry.username) },
                                    modifier = Modifier.testTag("add_friend_${entry.playerId}")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.addFriendLabel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val toReport = reportTarget
    if (toReport != null) {
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("${strings.reportLabel}: ${toReport.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportReason.entries.forEach { reason ->
                        OutlinedButton(
                            onClick = {
                                delegate.reportPlayer(toReport.playerId, toReport.username, reason, "Friend panel report")
                                reportTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (lang == AppLanguage.BENGALI) reason.labelBn else reason.labelEn)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { reportTarget = null }) { Text(strings.closeLabel) } }
        )
    }
}
