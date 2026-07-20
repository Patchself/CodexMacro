package com.patchself.codexmacro.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

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
            onJoystick = { _, _ -> },
            onCycleLayer = {},
        )
    }
}
