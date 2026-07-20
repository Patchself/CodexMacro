package com.patchself.codexmacro.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.components.ControllerStatus

internal data class ControllerCallbacks(
    val onStart: () -> Unit,
    val onStop: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onKey: (String, Int, Int?) -> Unit,
    val onJoystick: (Double, Double) -> Unit,
    val onCycleLayer: () -> Unit,
)

@Composable
internal fun PortraitController(
    state: ControllerState,
    settings: ControllerSettings,
    callbacks: ControllerCallbacks,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ControllerStatus(
            state = state,
            onStart = callbacks.onStart,
            onStop = callbacks.onStop,
            onOpenBluetoothSettings = callbacks.onOpenBluetoothSettings,
            onOpenSettings = callbacks.onOpenSettings,
        )
        Spacer(Modifier.weight(1f))
        ControllerBoard(state, settings, callbacks, Modifier.fillMaxWidth().weight(12f))
        ControllerMessage(state)
    }
}

@Composable
internal fun LandscapeController(
    state: ControllerState,
    settings: ControllerSettings,
    callbacks: ControllerCallbacks,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(190.dp).fillMaxHeight()) {
            ControllerStatus(
                state = state,
                onStart = callbacks.onStart,
                onStop = callbacks.onStop,
                onOpenBluetoothSettings = callbacks.onOpenBluetoothSettings,
                onOpenSettings = callbacks.onOpenSettings,
                vertical = true,
            )
            Spacer(Modifier.weight(1f))
            ControllerMessage(state)
        }
        ControllerBoard(
            state,
            settings,
            callbacks,
            Modifier.fillMaxHeight().weight(1f).padding(start = 12.dp),
        )
    }
}

@Composable
private fun ControllerBoard(
    state: ControllerState,
    settings: ControllerSettings,
    callbacks: ControllerCallbacks,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val boardWidth = minOf(maxWidth, maxHeight * 0.94f, 560.dp)
        MicroBoard(
            state = state,
            settings = settings,
            onKey = callbacks.onKey,
            onJoystick = callbacks.onJoystick,
            onCycleLayer = callbacks.onCycleLayer,
            modifier = Modifier.width(boardWidth).heightIn(max = 596.dp).aspectRatio(0.94f),
        )
    }
}

@Composable
private fun ControllerMessage(state: ControllerState) {
    state.message?.let { message ->
        Text(
            text = message,
            color = if (state.phase == ControllerPhase.ERROR) MaterialTheme.colorScheme.error else Color(0xFF5E5A53),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
        )
    }
}
