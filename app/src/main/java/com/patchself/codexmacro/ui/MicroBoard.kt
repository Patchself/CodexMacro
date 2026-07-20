package com.patchself.codexmacro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.components.AgentKey
import com.patchself.codexmacro.ui.components.CommandKey
import com.patchself.codexmacro.ui.components.DialControl
import com.patchself.codexmacro.ui.components.JoystickControl
import com.patchself.codexmacro.ui.components.LayerControl
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Composable
internal fun MicroBoard(
    state: ControllerState,
    settings: ControllerSettings,
    onKey: (String, Int, Int?) -> Unit,
    onJoystick: (Double, Double) -> Unit,
    onCycleLayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1)
    val commandKeycaps = CommandKeycap.normalizeLayers(settings.layerKeycaps)[activeLayer]
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
            border = BorderStroke(1.dp, Color(0xFFB8BFBB)),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    BoardMarkings()
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DialControl(state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                            AgentKey(0, state.threads[0], state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                            AgentKey(1, state.threads[1], state.isConnected, Modifier.weight(1f).fillMaxSize(), onKey)
                            JoystickControl(state.isConnected, Modifier.weight(1f).fillMaxSize(), onJoystick)
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { offset ->
                                val index = offset + 2
                                AgentKey(
                                    index,
                                    state.threads[index],
                                    state.isConnected,
                                    Modifier.weight(1f).fillMaxSize(),
                                    onKey,
                                )
                            }
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            commandKeycaps.take(4).forEachIndexed { index, keycap ->
                                CommandKey(
                                    keycap,
                                    "ACT${(index + 6).toString().padStart(2, '0')}",
                                    state.isConnected,
                                    Modifier.weight(1f).fillMaxSize(),
                                    onKey,
                                )
                            }
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LayerControl(activeLayer, onCycleLayer, Modifier.weight(1f).fillMaxSize())
                            CommandKey(
                                commandKeycaps[4],
                                "ACT10",
                                state.isConnected,
                                Modifier.weight(2f).fillMaxSize(),
                                onKey,
                            )
                            CommandKey(
                                commandKeycaps[5],
                                "ACT12",
                                state.isConnected,
                                Modifier.weight(1f).fillMaxSize(),
                                onKey,
                            )
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
