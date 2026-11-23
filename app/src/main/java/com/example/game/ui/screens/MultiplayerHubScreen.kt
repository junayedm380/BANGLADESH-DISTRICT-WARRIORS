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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.characters.CharacterCatalog
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.modes.GameModeId
import com.example.game.multiplayer.ChatChannel
import com.example.game.multiplayer.ConnectionState
import com.example.game.multiplayer.MatchPhase
import com.example.game.multiplayer.NetworkPlayerSlot
import com.example.game.multiplayer.ReportReason
import com.example.game.multiplayer.RoomPrivacy
import com.example.game.multiplayer.RoomSessionInfo
import com.example.game.multiplayer.TeamSide
import com.example.game.save.PlayerProgressState
import com.example.game.social.FriendStatus
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
fun MultiplayerHubScreen(
    viewModel: GameViewModel,
    progress: PlayerProgressState,
    strings: GameStrings,
    lang: AppLanguage,
    onEnterMultiplayerMatch: (GameModeId) -> Unit,
    onBack: () -> Unit
) {
    val delegate = viewModel.socialDelegate
    val coord = viewModel.multiplayerCoordinator
    val connection by coord.connectionFlow.collectAsStateWithLifecycle()
    val activeRoom by coord.activeRoomFlow.collectAsStateWithLifecycle()
    val publicRooms by coord.publicRoomsFlow.collectAsStateWithLifecycle()
    val chatMessages by coord.chatMessagesFlow.collectAsStateWithLifecycle()
    val errorNotice by coord.errorNoticeFlow.collectAsStateWithLifecycle()
    val auditLogs by coord.auditLogs.collectAsStateWithLifecycle()
    val friends by delegate.friends.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf(GameModeId.FOUR_PLAYER_SQUAD) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<NetworkPlayerSlot?>(null) }

    LaunchedEffect(Unit) {
        delegate.ensureConnected(progress.save)
    }

    LaunchedEffect(activeRoom?.phase) {
        val room = activeRoom
        if (room != null && room.phase == MatchPhase.IN_MATCH) {
            onEnterMultiplayerMatch(room.mode)
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
                .widthIn(max = 860.dp)
                .align(Alignment.TopCenter)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("mp_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backLabel)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(strings.multiplayerHub, style = MaterialTheme.typography.headlineSmall)
                }
                Surface(
                    color = when (connection) {
                        ConnectionState.IN_ROOM -> BengalEmerald.copy(alpha = 0.22f)
                        ConnectionState.DISCONNECTED -> CrimsonStrike.copy(alpha = 0.22f)
                        else -> DhakaAmber.copy(alpha = 0.18f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = connection.label(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (connection == ConnectionState.IN_ROOM) BengalEmeraldBright else DhakaAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Explicit Honest Labeling Banner
            Surface(
                color = DhakaAmber.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, DhakaAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = strings.localTestModeBanner,
                    style = MaterialTheme.typography.labelSmall,
                    color = DhakaAmber,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            if (errorNotice != null) {
                Surface(
                    color = CrimsonStrike.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CrimsonStrike),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(errorNotice.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                        if (connection == ConnectionState.DISCONNECTED) {
                            TextButton(onClick = { delegate.reconnect() }) { Text(strings.reconnectLabel, color = BengalEmeraldBright) }
                        } else {
                            TextButton(onClick = { coord.primaryClient.clearError() }) { Text(strings.closeLabel) }
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val room = activeRoom
                if (room == null) {
                    // 1. 4-Player Test Launch Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, BengalEmeraldBright),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.fourPlayerTestLabel, style = MaterialTheme.typography.titleMedium, color = BengalEmeraldBright)
                                Text(
                                    "Connects 4 real 127.0.0.1 sockets with 4 distinct characters, assigns teams, readies all slots, and syncs live movement & combat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val mpModes = GameModeId.entries.filter { it.isPlayableNow && it != GameModeId.TRAINING_GROUND }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(mpModes) { m ->
                                        FilterChip(
                                            selected = m == selectedMode,
                                            onClick = { selectedMode = m },
                                            label = { Text(m.title(lang), style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BengalEmerald, selectedLabelColor = ObsidianBg)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { delegate.launchFourPlayerTestRoom(selectedMode, "dhaka", progress.save) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                        modifier = Modifier.weight(1f).testTag("launch_4p_test_button")
                                    ) {
                                        Icon(Icons.Filled.Groups, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.fourPlayerTestLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(
                                        onClick = { delegate.quickMatch(selectedMode, "dhaka", progress.save) },
                                        modifier = Modifier.testTag("quick_match_button")
                                    ) {
                                        Text(strings.quickMatchLabel)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Room Creation & Code Join
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, TacticalSurfaceBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${strings.createRoomLabel} / ${strings.joinByCodeLabel}", style = MaterialTheme.typography.titleSmall, color = TacticalCyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { delegate.createRoom(RoomPrivacy.PUBLIC, selectedMode, "dhaka", progress.save) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = ObsidianBg),
                                        modifier = Modifier.weight(1f).testTag("create_public_room_button")
                                    ) { Text(strings.publicRoomsLabel) }
                                    OutlinedButton(
                                        onClick = { delegate.createRoom(RoomPrivacy.PRIVATE_CODE, selectedMode, "dhaka", progress.save) },
                                        modifier = Modifier.weight(1f).testTag("create_private_room_button")
                                    ) { Text(strings.privateRoomLabel) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = joinCodeInput,
                                        onValueChange = { joinCodeInput = it.uppercase().take(10) },
                                        placeholder = { Text(strings.roomCodeHint) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("room_code_input")
                                    )
                                    Button(
                                        onClick = { delegate.joinRoomByCode(joinCodeInput, progress.save) },
                                        enabled = joinCodeInput.length >= 4,
                                        modifier = Modifier.testTag("join_room_code_button")
                                    ) { Text(strings.joinByCodeLabel) }
                                }
                            }
                        }
                    }

                    // 3. Public Room Browser
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.publicRoomsLabel, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { coord.primaryClient.requestRoomList() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh rooms", tint = TacticalCyan)
                            }
                        }
                    }

                    items(publicRooms, key = { it.roomId }) { summary ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, TacticalSurfaceBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${summary.roomCode} • ${summary.mode.title(lang)}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("Host: ${summary.hostName} • ${summary.districtId.uppercase()} • ${summary.playerCount}/${summary.maxPlayers} • ${summary.pingMs}ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { delegate.joinRoomByCode(summary.roomCode, progress.save) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg)
                                ) { Text("JOIN") }
                            }
                        }
                    }
                } else {
                    // ACTIVE MULTIPLAYER ROOM LOBBY
                    item {
                        ActiveRoomHeaderCard(
                            room = room,
                            lang = lang,
                            strings = strings,
                            connection = connection,
                            onSimDrop = { delegate.simulateDrop() },
                            onReconnect = { delegate.reconnect() },
                            onLeave = { delegate.leaveRoom() }
                        )
                    }

                    // 4-Player Lobby Slots
                    items(room.players, key = { it.playerId }) { slot ->
                        val charSlot = CharacterCatalog.getById(slot.characterId)
                        val isLocal = slot.playerId == coord.primaryClient.localPlayerId
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, if (slot.isReady) BengalEmeraldBright else TacticalSurfaceBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(slot.avatarColorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(slot.callsign.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, color = ObsidianBg, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${slot.callsign}${if (slot.isHost) " (HOST)" else ""}${if (isLocal) " (YOU)" else ""}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${charSlot.displayName} • ${slot.team.label(lang)} • ${if (slot.isConnected) "Online" else "Reconnecting"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TacticalCyan
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        color = if (slot.isReady) BengalEmerald.copy(alpha = 0.25f) else DhakaAmber.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (slot.isReady) strings.readyLabel else strings.unreadyLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (slot.isReady) BengalEmeraldBright else DhakaAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (!isLocal) {
                                        IconButton(onClick = { reportTarget = slot }) {
                                            Icon(Icons.Filled.Flag, contentDescription = strings.reportLabel, tint = CrimsonStrike, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Room Controls (Ready, Team Switch, Invite Friends, Start Match)
                    item {
                        val localSlot = room.players.firstOrNull { it.playerId == coord.primaryClient.localPlayerId }
                        val isHost = localSlot?.isHost == true
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { delegate.toggleReady(localSlot?.isReady == true) },
                                    modifier = Modifier.weight(1f).testTag("room_ready_button")
                                ) {
                                    Text(if (localSlot?.isReady == true) strings.unreadyLabel else strings.readyLabel)
                                }
                                if (room.mode.supportsTeams) {
                                    OutlinedButton(
                                        onClick = {
                                            val next = if (localSlot?.team == TeamSide.ALPHA) TeamSide.BRAVO else TeamSide.ALPHA
                                            delegate.setTeam(next)
                                        },
                                        modifier = Modifier.weight(1f).testTag("room_switch_team_button")
                                    ) {
                                        Text("Team: ${localSlot?.team?.id ?: "ALPHA"}")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showInviteDialog = true },
                                    modifier = Modifier.testTag("room_invite_button")
                                ) {
                                    Icon(Icons.Filled.GroupAdd, contentDescription = strings.inviteFriendsLabel, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (isHost) {
                                Button(
                                    onClick = { delegate.startMultiplayerMatch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                                    modifier = Modifier.fillMaxWidth().testTag("room_start_match_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.startMatchLabel, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Voice & Text Chat Panel
                    item {
                        RoomCommsCard(
                            viewModel = viewModel,
                            room = room,
                            chatMessages = chatMessages,
                            strings = strings,
                            lang = lang
                        )
                    }

                    // Server Authority & Anti-Cheat Audit Log Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                            border = BorderStroke(1.dp, TacticalSurfaceBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Security, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(18.dp))
                                        Text("Server Authority Audit Log", style = MaterialTheme.typography.titleSmall, color = TacticalCyan)
                                    }
                                    TextButton(onClick = { delegate.testServerAnticheatTamper() }) {
                                        Text("Test Anti-Cheat", color = CrimsonStrike)
                                    }
                                }
                                if (auditLogs.isEmpty()) {
                                    Text("Authoritative checks active (movement clamp, damage calculation, reward validation).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    auditLogs.take(4).forEach { entry ->
                                        Text("[${entry.category}] ${entry.playerId}: ${entry.detail}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog && activeRoom != null) {
        val acceptedFriends = friends.filter { it.status == FriendStatus.ACCEPTED }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text(strings.inviteFriendsLabel) },
            text = {
                if (acceptedFriends.isEmpty()) {
                    Text("No accepted friends yet. Visit Friends & ID to accept requests or search players.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        acceptedFriends.forEach { f ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(f.username, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = {
                                    delegate.inviteFriendToActiveRoom(f.playerId, f.username)
                                    showInviteDialog = false
                                }) { Text("INVITE") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInviteDialog = false }) { Text(strings.closeLabel) } }
        )
    }

    val targetToReport = reportTarget
    if (targetToReport != null) {
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("${strings.reportLabel}: ${targetToReport.callsign}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportReason.entries.forEach { reason ->
                        OutlinedButton(
                            onClick = {
                                delegate.reportPlayer(targetToReport.playerId, targetToReport.callsign, reason, "Lobby report")
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

@Composable
private fun ActiveRoomHeaderCard(
    room: RoomSessionInfo,
    lang: AppLanguage,
    strings: GameStrings,
    connection: ConnectionState,
    onSimDrop: () -> Unit,
    onReconnect: () -> Unit,
    onLeave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, BengalEmeraldBright),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ROOM ${room.roomCode} • ${room.mode.title(lang)}", style = MaterialTheme.typography.titleMedium, color = BengalEmeraldBright)
                    Text("${room.districtId.uppercase()} • ${room.players.size}/${room.maxPlayers} • ${room.endpointLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLeave, modifier = Modifier.testTag("room_leave_button")) {
                    Text(strings.leaveRoomLabel, color = CrimsonStrike)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connection == ConnectionState.IN_ROOM) {
                    OutlinedButton(onClick = onSimDrop, modifier = Modifier.testTag("sim_drop_button")) {
                        Text(strings.simDropLabel)
                    }
                } else {
                    Button(onClick = onReconnect, colors = ButtonDefaults.buttonColors(containerColor = DhakaAmber, contentColor = ObsidianBg), modifier = Modifier.testTag("reconnect_button")) {
                        Text(strings.reconnectLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCommsCard(
    viewModel: GameViewModel,
    room: RoomSessionInfo,
    chatMessages: List<com.example.game.multiplayer.ChatMessage>,
    strings: GameStrings,
    lang: AppLanguage
) {
    val voice = viewModel.multiplayerCoordinator.voiceProvider
    val micMuted by voice.localMicMuted.collectAsStateWithLifecycle()
    val deafened by voice.localDeafened.collectAsStateWithLifecycle()
    val voiceParticipants by voice.participants.collectAsStateWithLifecycle()

    var channel by remember { mutableStateOf(ChatChannel.GLOBAL) }
    var chatInput by remember { mutableStateOf("") }
    var privateTargetId by remember { mutableStateOf(room.players.firstOrNull()?.playerId ?: "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, TacticalSurfaceBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.voiceChatLabel, style = MaterialTheme.typography.titleSmall, color = DhakaAmber)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !micMuted,
                        onClick = { voice.setLocalMicMuted(!micMuted) },
                        leadingIcon = { Icon(if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (micMuted) strings.micMuteLabel else "MIC ON", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = deafened,
                        onClick = { voice.setLocalDeafened(!deafened) },
                        leadingIcon = { Icon(Icons.Filled.HeadsetOff, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.deafenLabel, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Text(voice.providerName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (voiceParticipants.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(voiceParticipants) { vp ->
                        FilterChip(
                            selected = vp.isMutedByLocal,
                            onClick = { voice.togglePlayerMute(vp.playerId) },
                            label = { Text("${vp.callsign} ${if (vp.isMutedByLocal) "(Muted)" else ""}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ChatChannel.GLOBAL, ChatChannel.TEAM, ChatChannel.PRIVATE).forEach { ch ->
                    FilterChip(
                        selected = channel == ch,
                        onClick = { channel = ch },
                        label = { Text(ch.label(lang), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TacticalCyan, selectedLabelColor = ObsidianBg)
                    )
                }
            }

            if (channel == ChatChannel.PRIVATE) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(room.players) { p ->
                        FilterChip(
                            selected = privateTargetId == p.playerId,
                            onClick = { privateTargetId = p.playerId },
                            label = { Text(p.callsign, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 140.dp)
                    .background(ObsidianBg, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chatMessages.takeLast(6).forEach { msg ->
                    Text(
                        text = "[${msg.channel.id}] ${msg.senderName}: ${msg.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.channel == ChatChannel.SYSTEM) DhakaAmber else Color.White
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it.take(140) },
                    placeholder = { Text("Message (${channel.label(lang)})...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("lobby_chat_input")
                )
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.socialDelegate.sendChat(channel, if (channel == ChatChannel.PRIVATE) privateTargetId else "", chatInput)
                            chatInput = ""
                        }
                    },
                    modifier = Modifier.testTag("lobby_chat_send")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BengalEmeraldBright)
                }
            }
        }
    }
}
