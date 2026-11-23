package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.localization.AppLanguage
import com.example.game.localization.GameStrings
import com.example.game.multiplayer.ChatChannel
import com.example.game.multiplayer.ConnectionState
import com.example.game.multiplayer.TeamSide
import com.example.game.ui.GameViewModel
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder

@Composable
fun BoxScope.MultiplayerInGameOverlays(
    viewModel: GameViewModel,
    strings: GameStrings,
    lang: AppLanguage
) {
    val coord = viewModel.multiplayerCoordinator
    val connection by coord.connectionFlow.collectAsStateWithLifecycle()
    val worldState by coord.worldStateFlow.collectAsStateWithLifecycle()
    val chatMessages by coord.chatMessagesFlow.collectAsStateWithLifecycle()
    val micMuted by coord.voiceProvider.localMicMuted.collectAsStateWithLifecycle()

    var showChatBox by remember { mutableStateOf(false) }
    var chatChannel by remember { mutableStateOf(ChatChannel.GLOBAL) }
    var chatInput by remember { mutableStateOf("") }

    // Top-Center: Synchronized Squad Roster & Match Timer + Comms Toggles
    Column(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val ws = worldState
        if (ws != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("T-${ws.remainingSec}s", style = MaterialTheme.typography.labelMedium, color = DhakaAmber)
                    ws.players.take(4).forEach { p ->
                        val c = when {
                            !p.isAlive -> Color.Gray
                            p.team == TeamSide.ALPHA -> BengalEmeraldBright
                            p.team == TeamSide.BRAVO -> CrimsonStrike
                            else -> TacticalCyan
                        }
                        Text(
                            text = "${p.callsign.take(7)}:${p.health}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c
                        )
                    }
                }
            }
        }

        // Quick Comms + Reconnect Test Strip
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                onClick = { showChatBox = !showChatBox },
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("ingame_chat_toggle")
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Chat, contentDescription = strings.chatLabel, tint = TacticalCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.chatLabel, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
            }

            Surface(
                onClick = { coord.voiceProvider.setLocalMicMuted(!micMuted) },
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DhakaAmber.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("ingame_mic_toggle")
            ) {
                Icon(
                    imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = strings.micMuteLabel,
                    tint = if (micMuted) CrimsonStrike else BengalEmeraldBright,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp).size(14.dp)
                )
            }

            if (connection == ConnectionState.IN_ROOM) {
                Surface(
                    onClick = { viewModel.socialDelegate.simulateDrop() },
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TacticalSurfaceBorder),
                    modifier = Modifier.testTag("ingame_sim_drop")
                ) {
                    Text(strings.simDropLabel, style = MaterialTheme.typography.labelSmall, color = DhakaAmber, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
        }
    }

    // Disconnect / Reconnect Recovery Banner
    if (connection == ConnectionState.DISCONNECTED || connection == ConnectionState.RECONNECTING) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.94f)),
            border = BorderStroke(1.5.dp, CrimsonStrike),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .testTag("reconnect_banner")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null, tint = CrimsonStrike)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.connectionLostMsg, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Button(
                    onClick = { viewModel.socialDelegate.reconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = BengalEmeraldBright, contentColor = ObsidianBg),
                    modifier = Modifier.testTag("ingame_reconnect_button")
                ) { Text(strings.reconnectLabel) }
            }
        }
    }

    // Collapsible In-Game Text/Team Chat Drawer
    if (showChatBox) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, TacticalCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                .padding(start = 8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ChatChannel.GLOBAL, ChatChannel.TEAM).forEach { ch ->
                        FilterChip(
                            selected = chatChannel == ch,
                            onClick = { chatChannel = ch },
                            label = { Text(ch.label(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 110.dp)
                        .background(ObsidianBg, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    chatMessages.takeLast(5).forEach { msg ->
                        Text("${msg.senderName}: ${msg.text}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it.take(120) },
                        placeholder = { Text("Chat...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.socialDelegate.sendChat(chatChannel, "", chatInput)
                            chatInput = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BengalEmeraldBright)
                    }
                }
            }
        }
    }
}
