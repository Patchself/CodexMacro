package com.patchself.codexmacro.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.protocol.CodexProtocol
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.protocol.LightingSide
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.components.AgentKey
import com.patchself.codexmacro.ui.components.CommandKey
import com.patchself.codexmacro.ui.components.DialControl
import com.patchself.codexmacro.ui.components.JoystickControl
import com.patchself.codexmacro.ui.components.LayerControl
import com.patchself.codexmacro.ui.components.rgbColor
import com.patchself.codexmacro.ui.theme.CodexMacroTheme
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

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
    val ambientNeedsAnimation = state.ambient.effect == CodexProtocol.effectSnake ||
        state.ambient.effect == CodexProtocol.effectRainbow ||
        state.ambient.effect == CodexProtocol.effectBreath ||
        state.ambient.effect == CodexProtocol.effectShallowBreath
    val keysNeedsAnimation = state.keys.effect == CodexProtocol.effectSnake ||
        state.keys.effect == CodexProtocol.effectRainbow ||
        state.keys.effect == CodexProtocol.effectBreath ||
        state.keys.effect == CodexProtocol.effectShallowBreath
    val needsLightingAnimation = ambientNeedsAnimation || keysNeedsAnimation
    val lightingSpeed = maxOf(state.ambient.speed, state.keys.speed).coerceIn(0f, 1f)
    val lightingPhase: Float
    val lightingPulse: Float
    if (needsLightingAnimation) {
        val lightingTransition = rememberInfiniteTransition(label = "global-lighting")
        lightingPhase = lightingTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                tween((4000f - lightingSpeed * 3200f).roundToInt(), easing = LinearEasing),
            ),
            label = "global-lighting-phase",
        ).value
        lightingPulse = lightingTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween((1800f - lightingSpeed * 1200f).roundToInt()),
                RepeatMode.Reverse,
            ),
            label = "global-lighting-pulse",
        ).value
    } else {
        lightingPhase = 0f
        lightingPulse = 1f
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFD6F4E3),
        shadowElevation = if (state.isConnected) 18.dp else 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
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
                                AgentKey(
                                    0,
                                    state.threads[0],
                                    state.isConnected,
                                    settings.showKeyLabels,
                                    Modifier.weight(1f).fillMaxSize(),
                                    onKey,
                                )
                                AgentKey(
                                    1,
                                    state.threads[1],
                                    state.isConnected,
                                    settings.showKeyLabels,
                                    Modifier.weight(1f).fillMaxSize(),
                                    onKey,
                                )
                                JoystickControl(state.isConnected, Modifier.weight(1f).fillMaxSize(), onJoystick)
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(4) { offset ->
                                    val index = offset + 2
                                    AgentKey(
                                        index,
                                        state.threads[index],
                                        state.isConnected,
                                        settings.showKeyLabels,
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
                                        settings.showKeyLabels,
                                        lightingColor(state.keys, lightingPhase - index * 60f, lightingPulse, true),
                                        Modifier.weight(1f).fillMaxSize(),
                                        onKey,
                                    )
                                }
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LayerControl(
                                    activeLayer,
                                    state.isConnected,
                                    onCycleLayer,
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                                CommandKey(
                                    commandKeycaps[4],
                                    "ACT10",
                                    state.isConnected,
                                    settings.showKeyLabels,
                                    lightingColor(state.keys, lightingPhase - 240f, lightingPulse, true),
                                    Modifier.weight(2f).fillMaxSize(),
                                    onKey,
                                )
                                CommandKey(
                                    commandKeycaps[5],
                                    "ACT12",
                                    state.isConnected,
                                    settings.showKeyLabels,
                                    lightingColor(state.keys, lightingPhase - 300f, lightingPulse, true),
                                    Modifier.weight(1f).fillMaxSize(),
                                    onKey,
                                )
                            }
                        }
                    }
                }
            }
            AmbientLighting(
                light = state.ambient,
                phase = lightingPhase,
                pulse = lightingPulse,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AmbientLighting(
    light: LightingSide,
    phase: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val color = lightingColor(light, phase, pulse)
    if (color.alpha == 0f) return
    Canvas(modifier) {
        val stroke = Stroke(width = 5.dp.toPx())
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())
        when (light.effect) {
            CodexProtocol.effectSnake -> rotate(phase) {
                drawRoundRect(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.9f to color,
                        1f to Color.Transparent,
                    ),
                    cornerRadius = cornerRadius,
                    style = stroke,
                )
            }
            CodexProtocol.effectRainbow -> rotate(phase) {
                drawRoundRect(
                    brush = Brush.sweepGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    ),
                    cornerRadius = cornerRadius,
                    style = stroke,
                    alpha = light.brightness.coerceIn(0f, 1f),
                )
            }
            else -> drawRoundRect(color = color, cornerRadius = cornerRadius, style = stroke)
        }
    }
}

private fun lightingColor(
    light: LightingSide,
    phase: Float,
    pulse: Float,
    animateSnake: Boolean = false,
): Color {
    if (light.effect == CodexProtocol.effectOff || light.brightness <= 0f) return Color.Transparent
    val alpha = when (light.effect) {
        CodexProtocol.effectBreath -> pulse
        CodexProtocol.effectShallowBreath -> 0.5f + pulse * 0.5f
        CodexProtocol.effectSnake -> if (animateSnake) {
            0.15f + 0.85f * ((cos(Math.toRadians(phase.toDouble())) + 1.0) / 2.0).pow(4.0).toFloat()
        } else {
            1f
        }
        else -> 1f
    } * light.brightness.coerceIn(0f, 1f)
    val color = if (light.effect == CodexProtocol.effectRainbow) {
        Color.hsv(phase, 0.72f, 1f)
    } else {
        rgbColor(light.color)
    }
    return color.copy(alpha = alpha)
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
                ambient = LightingSide(
                    color = 0x304FFE,
                    brightness = 1f,
                    effect = CodexProtocol.effectSnake,
                    speed = 0.4f,
                ),
                keys = LightingSide(
                    color = 0x304FFE,
                    brightness = 0.8f,
                    effect = CodexProtocol.effectSnake,
                    speed = 0.4f,
                ),
            ),
            settings = ControllerSettings(),
            onKey = { _, _, _ -> },
            onJoystick = { _, _ -> },
            onCycleLayer = {},
            modifier = Modifier.fillMaxWidth().padding(12.dp).aspectRatio(0.94f),
        )
    }
}
