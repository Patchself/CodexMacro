package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Preview(name = "Micro controls", widthDp = 420, heightDp = 120, showBackground = true)
@Composable
private fun MicroControlsPreview() {
    CodexMacroTheme {
        Row(
            Modifier.fillMaxSize().background(Color(0xFFF0F2EF)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialControl(true, Modifier.weight(1f).fillMaxSize()) { _, _, _ -> }
            AgentKey(
                index = 0,
                light = ThreadLight(color = 0x9D9AF4, brightness = 1f),
                enabled = true,
                modifier = Modifier.weight(1f).fillMaxSize(),
            ) { _, _, _ -> }
            CommandKey(
                keycap = CommandKeycap.Approve,
                id = "ACT07",
                enabled = true,
                modifier = Modifier.weight(1f).fillMaxSize(),
            ) { _, _, _ -> }
            JoystickControl(true, Modifier.weight(1f).fillMaxSize()) { _, _ -> }
        }
    }
}

@Preview(name = "Layer control", widthDp = 110, heightDp = 100, showBackground = true)
@Composable
private fun LayerControlPreview() {
    CodexMacroTheme {
        LayerControl(
            activeLayer = 3,
            onCycleLayer = {},
            modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2EF)),
        )
    }
}
