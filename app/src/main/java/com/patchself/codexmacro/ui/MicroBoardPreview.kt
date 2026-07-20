package com.patchself.codexmacro.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Preview(name = "Connected controller board", widthDp = 420, heightDp = 470, showBackground = true)
@Composable
private fun MicroBoardPreview() {
    CodexMacroTheme {
        MicroBoard(
            state = ControllerState(
                phase = ControllerPhase.CONNECTED,
                threads = List(6) { index ->
                    ThreadLight(color = 0x8ADDBBL + index * 0x080808L, brightness = 1f)
                },
            ),
            settings = ControllerSettings(),
            onKey = { _, _, _ -> },
            onJoystick = { _, _ -> },
            onCycleLayer = {},
            modifier = Modifier.fillMaxWidth().padding(12.dp).aspectRatio(0.94f),
        )
    }
}
