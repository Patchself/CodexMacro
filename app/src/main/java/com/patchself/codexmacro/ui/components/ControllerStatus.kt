package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState

@Composable
fun ControllerStatus(
    state: ControllerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    if (vertical) {
        Column(modifier.fillMaxWidth()) {
            StatusIdentity(state)
            BatteryStatus(state, Modifier.padding(top = 12.dp))
            StatusActions(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onOpenSettings = onOpenSettings,
                vertical = true,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIdentity(state, Modifier.weight(1f))
                    BatteryStatus(state, Modifier.padding(start = 8.dp))
                }
                StatusActions(
                    state = state,
                    onStart = onStart,
                    onStop = onStop,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIdentity(state)
                Spacer(Modifier.weight(1f))
                BatteryStatus(state, Modifier.padding(horizontal = 10.dp))
                StatusActions(
                    state = state,
                    onStart = onStart,
                    onStop = onStop,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun StatusIdentity(state: ControllerState, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Codex Micro", color = Color(0xFF181714), fontWeight = FontWeight.Black, fontSize = 20.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).background(phaseColor(state.phase), CircleShape))
            Text(phaseLabel(state.phase), color = Color(0xFF656159), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "· ${state.hostName ?: "ChatGPT Desktop"}",
                color = Color(0xFF77736B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BatteryStatus(state: ControllerState, modifier: Modifier = Modifier) {
    Text(
        text = if (state.isCharging) "ϟ ${state.battery}%" else "${state.battery}%",
        color = Color(0xFF625E56),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier,
    )
}

@Composable
private fun StatusActions(
    state: ControllerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        if (state.phase == ControllerPhase.ADVERTISING) {
            StatusButton("Pair", onOpenBluetoothSettings, vertical)
        }
        StatusButton("Settings", onOpenSettings, vertical)
        StatusButton(if (state.isRunning) "Stop" else "Start", if (state.isRunning) onStop else onStart, vertical)
    }
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    } else {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun StatusButton(label: String, onClick: () -> Unit, fillWidth: Boolean) {
    Button(
        onClick = onClick,
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF24231F),
            contentColor = Color.White,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun phaseColor(phase: ControllerPhase): Color = when (phase) {
    ControllerPhase.CONNECTED -> Color(0xFF38B779)
    ControllerPhase.ADVERTISING, ControllerPhase.STARTING -> Color(0xFFE6A23C)
    ControllerPhase.ERROR, ControllerPhase.UNSUPPORTED -> Color(0xFFD64B4B)
    ControllerPhase.STOPPED -> Color(0xFF99958E)
}

private fun phaseLabel(phase: ControllerPhase): String = when (phase) {
    ControllerPhase.STOPPED -> "OFFLINE"
    ControllerPhase.STARTING -> "STARTING"
    ControllerPhase.ADVERTISING -> "PAIRING"
    ControllerPhase.CONNECTED -> "CONNECTED"
    ControllerPhase.ERROR -> "ERROR"
    ControllerPhase.UNSUPPORTED -> "UNSUPPORTED"
}
