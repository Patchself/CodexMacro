package com.patchself.codexmacro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Preview(name = "Controller settings", widthDp = 440, heightDp = 720, showBackground = true)
@Composable
private fun SettingsContentPreview() {
    CodexMacroTheme {
        SettingsContent(
            settings = ControllerSettings(stableConnection = true, activeLayer = 2),
            onSettingsChange = {},
            onDismiss = {},
            onEditSlot = {},
        )
    }
}

@Preview(name = "Keycap picker", widthDp = 440, heightDp = 620, showBackground = true)
@Composable
private fun KeycapPickerPreview() {
    CodexMacroTheme {
        KeycapPicker(
            layer = 0,
            slot = 1,
            selected = CommandKeycap.Approve,
            onSelect = {},
            onBack = {},
        )
    }
}
