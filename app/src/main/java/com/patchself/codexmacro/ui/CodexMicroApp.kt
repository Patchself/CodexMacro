package com.patchself.codexmacro.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.tooling.preview.Preview
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.bluetooth.CustomKeyBinding
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.components.ControllerSettingsDialog
import com.patchself.codexmacro.ui.components.LayerEditorScreen
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

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
    onShortcut: (CustomKeyBinding, Boolean) -> Unit,
    onJoystick: (Double, Double) -> Unit,
    onCycleLayer: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showLayerEditor by rememberSaveable { mutableStateOf(false) }
    val closeLayerEditor = {
        showLayerEditor = false
        showSettings = true
    }
    BackHandler(enabled = showLayerEditor, onBack = closeLayerEditor)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        if (showLayerEditor) {
            LayerEditorScreen(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onBack = closeLayerEditor,
            )
        } else {
            val callbacks = ControllerCallbacks(
                onStart = onStart,
                onStop = onStop,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onOpenSettings = { showSettings = true },
                onKey = onKey,
                onShortcut = onShortcut,
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
                    onOpenLayerEditor = {
                        showSettings = false
                        showLayerEditor = true
                    },
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}

@Preview(name = "Phone portrait", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Phone landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun CodexMicroAppPreview() {
    CodexMacroTheme {
        CodexMicroApp(
            state = ControllerState(),
            settings = ControllerSettings(),
            onSettingsChange = {},
            onStart = {},
            onStop = {},
            onOpenBluetoothSettings = {},
            onKey = { _, _, _ -> },
            onShortcut = { _, _ -> },
            onJoystick = { _, _ -> },
            onCycleLayer = {},
        )
    }
}
