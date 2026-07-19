package com.patchself.codexmacro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.ui.components.AgentKey
import com.patchself.codexmacro.ui.components.CommandKey
import com.patchself.codexmacro.ui.components.ControllerStatus
import com.patchself.codexmacro.ui.components.ControllerSettingsDialog
import com.patchself.codexmacro.ui.components.DialControl
import com.patchself.codexmacro.ui.components.JoystickControl
import com.patchself.codexmacro.ui.components.LayerControl
import com.patchself.codexmacro.R

private val appBackground = Brush.verticalGradient(
    listOf(Color(0xFFF4F1EB), Color(0xFFE1DDD4)),
)

@Composable
fun CodexMicroApp(
    state: ControllerState,
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onKey: (String, Int, Int?) -> Unit,
    onJoystick: (Double, Double) -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ControllerStatus(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onOpenSettings = { showSettings = true },
            )
            Spacer(Modifier.weight(1f))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(12f),
                contentAlignment = Alignment.Center,
            ) {
                val boardWidth = minOf(maxWidth, maxHeight * 0.94f, 560.dp)
                MicroBoard(
                    state = state,
                    onKey = onKey,
                    onJoystick = onJoystick,
                    modifier = Modifier.width(boardWidth).heightIn(max = 596.dp).aspectRatio(0.94f),
                )
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    color = if (state.phase == ControllerPhase.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF5E5A53)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 1,
                )
            }
        }
        if (showSettings) {
            ControllerSettingsDialog(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onDismiss = { showSettings = false },
            )
        }
    }
}

@Composable
private fun MicroBoard(
    state: ControllerState,
    onKey: (String, Int, Int?) -> Unit,
    onJoystick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFD6F4E3),
        shadowElevation = 18.dp,
        tonalElevation = 0.dp,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(11.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F2EF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8BFBB)),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    BoardMarkings()
                    Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    DialControl(state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    AgentKey(0, state.threads[0], state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    AgentKey(1, state.threads[1], state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    JoystickControl(state.isConnected, Modifier.weight(1f).fillMaxSize(), onJoystick)
                }
                Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    repeat(4) { offset ->
                        val index = offset + 2
                        AgentKey(index, state.threads[index], state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    }
                }
                Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    CommandKey(R.drawable.ic_key_fast, "Fast", "ACT06", state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    CommandKey(R.drawable.ic_key_approve, "Approve", "ACT07", state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    CommandKey(R.drawable.ic_key_decline, "Decline", "ACT08", state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                    CommandKey(R.drawable.ic_key_fork, "Fork", "ACT09", state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                }
                Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    LayerControl(Modifier.weight(1f).fillMaxSize())
                    CommandKey(R.drawable.ic_key_mic, "Mic", "ACT10", state.isConnected, Modifier.weight(2f).fillMaxSize(), onKey)
                    CommandKey(R.drawable.ic_key_codex, "Codex", "ACT12", state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                }
                    }
                }
                BoardScrew(Modifier.align(Alignment.TopStart))
                BoardScrew(Modifier.align(Alignment.TopEnd))
                BoardScrew(Modifier.align(Alignment.BottomStart))
                BoardScrew(Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

@Composable
private fun BoardScrew(modifier: Modifier) {
    Box(
        modifier = modifier.padding(5.dp).size(8.dp).background(Color(0xFF242522), CircleShape)
            .border(1.dp, Color.Black, CircleShape),
    )
}

@Composable
private fun BoardMarkings() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("WORK LOUDER × OPENAI", color = Color(0xFF8C8981), fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text("2026", color = Color(0xFF8C8981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}
