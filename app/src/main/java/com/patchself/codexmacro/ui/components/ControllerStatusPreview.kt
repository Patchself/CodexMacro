package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Preview(name = "Connected status", widthDp = 520, showBackground = true)
@Preview(name = "Compact status", widthDp = 360, showBackground = true)
@Composable
private fun ControllerStatusPreview() {
    CodexMacroTheme {
        ControllerStatus(
            state = ControllerState(
                phase = ControllerPhase.CONNECTED,
                hostName = "MacBook Pro",
                battery = 78,
                isCharging = true,
            ),
            onStart = {},
            onStop = {},
            onOpenBluetoothSettings = {},
            onOpenSettings = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
