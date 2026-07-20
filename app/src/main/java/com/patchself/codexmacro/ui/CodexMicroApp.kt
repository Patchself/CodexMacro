package com.patchself.codexmacro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.components.ControllerSettingsDialog

private val appBackground = Brush.verticalGradient(
    listOf(Color(0xFFF4F1EB), Color(0xFFE1DDD4)),
)

/** CodexMicroApp renders the adaptive controller screen and settings flow. */
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
    onCycleLayer: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        val callbacks = ControllerCallbacks(
            onStart = onStart,
            onStop = onStop,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
            onOpenSettings = { showSettings = true },
            onKey = onKey,
            onJoystick = onJoystick,
            onCycleLayer = onCycleLayer,
        )
        if (maxWidth > maxHeight) {
            LandscapeController(state, settings, callbacks)
        } else {
            PortraitController(state, settings, callbacks)
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
