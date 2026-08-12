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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Matrix
import android.graphics.SweepGradient as AndroidSweepGradient
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.bluetooth.CustomKeyBinding
import com.patchself.codexmacro.protocol.CodexProtocol
import com.patchself.codexmacro.protocol.ControllerPhase
import com.patchself.codexmacro.protocol.ControllerState
import com.patchself.codexmacro.protocol.LightingSide
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.components.AgentKey
import com.patchself.codexmacro.ui.components.CommandKey
import com.patchself.codexmacro.ui.components.CustomKey
import com.patchself.codexmacro.ui.components.DialControl
import com.patchself.codexmacro.ui.components.JoystickControl
import com.patchself.codexmacro.ui.components.LayerControl
import com.patchself.codexmacro.ui.components.localizedKeycapLabel
import com.patchself.codexmacro.ui.components.localizedShortcutLabel
import com.patchself.codexmacro.ui.components.rgbColor
import com.patchself.codexmacro.ui.theme.CodexMacroTheme
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun MicroBoard(
    state: ControllerState,
    settings: ControllerSettings,
    onKey: (String, Int, Int?) -> Unit,
    onShortcut: (CustomKeyBinding, Boolean) -> Unit,
    onJoystick: (Double, Double) -> Unit,
    onCycleLayer: () -> Unit,
    modifier: Modifier = Modifier,
    onEditSlot: ((Int) -> Unit)? = null,
) {
    val activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1)
    val isCodexLayer = activeLayer == 0
    val commandKeycaps = CommandKeycap.normalizeLayout(settings.codexKeycaps)
    val customKeys = if (isCodexLayer) null else CustomKeyBinding.normalizeLayers(settings.customLayers)[activeLayer - 1]
    val ambientNeedsAnimation = state.ambient.effect == CodexProtocol.effectSnake ||
        state.ambient.effect == CodexProtocol.effectRainbow ||
        state.ambient.effect == CodexProtocol.effectBreath ||
        state.ambient.effect == CodexProtocol.effectShallowBreath ||
        state.ambient.effect == CodexProtocol.effectGradient
    val keysNeedsAnimation = state.keys.effect == CodexProtocol.effectSnake ||
        state.keys.effect == CodexProtocol.effectRainbow ||
        state.keys.effect == CodexProtocol.effectBreath ||
        state.keys.effect == CodexProtocol.effectShallowBreath ||
        state.keys.effect == CodexProtocol.effectGradient
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
                                DialControl(state.isConnected && isCodexLayer, Modifier.weight(1f).fillMaxSize(), onKey)
                                repeat(2) { index ->
                                    if (isCodexLayer) {
                                        AgentKey(
                                            index,
                                            state.threads[index],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            Modifier.weight(1f).fillMaxSize(),
                                            onKey,
                                        )
                                    } else {
                                        CustomKey(
                                            customKeys!![index],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 30f, lightingPulse, true),
                                            Modifier.weight(1f).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 1,
                                                    localizedShortcutLabel(customKeys[index]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index) } },
                                            onShortcut = onShortcut,
                                        )
                                    }
                                }
                                JoystickControl(state.isConnected && isCodexLayer, Modifier.weight(1f).fillMaxSize(), onJoystick)
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(4) { offset ->
                                    val index = offset + 2
                                    if (isCodexLayer) {
                                        AgentKey(
                                            index,
                                            state.threads[index],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            Modifier.weight(1f).fillMaxSize(),
                                            onKey,
                                        )
                                    } else {
                                        CustomKey(
                                            customKeys!![index],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 30f, lightingPulse, true),
                                            Modifier.weight(1f).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 1,
                                                    localizedShortcutLabel(customKeys[index]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index) } },
                                            onShortcut = onShortcut,
                                        )
                                    }
                                }
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(4) { index ->
                                    if (isCodexLayer) {
                                        CommandKey(
                                            commandKeycaps[index],
                                            "ACT${(index + 6).toString().padStart(2, '0')}",
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 60f, lightingPulse, true),
                                            Modifier.weight(1f).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 1,
                                                    localizedKeycapLabel(commandKeycaps[index]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index) } },
                                            onKey = onKey,
                                        )
                                    } else {
                                        CustomKey(
                                            customKeys!![index + 6],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 60f, lightingPulse, true),
                                            Modifier.weight(1f).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 7,
                                                    localizedShortcutLabel(customKeys[index + 6]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index + 6) } },
                                            onShortcut = onShortcut,
                                        )
                                    }
                                }
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LayerControl(
                                    activeLayer,
                                    state.isConnected,
                                    onCycleLayer,
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                                listOf(4 to 2f, 5 to 1f).forEach { (index, weight) ->
                                    if (isCodexLayer) {
                                        CommandKey(
                                            commandKeycaps[index],
                                            if (index == 4) "ACT10" else "ACT12",
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 60f, lightingPulse, true),
                                            Modifier.weight(weight).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 1,
                                                    localizedKeycapLabel(commandKeycaps[index]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index) } },
                                            onKey = onKey,
                                        )
                                    } else {
                                        CustomKey(
                                            customKeys!![index + 6],
                                            state.isConnected,
                                            settings.showKeyLabels,
                                            lightingColor(state.keys, lightingPhase - index * 60f, lightingPulse, true),
                                            Modifier.weight(weight).fillMaxSize(),
                                            description = onEditSlot?.let {
                                                stringResource(
                                                    R.string.key_description,
                                                    index + 7,
                                                    localizedShortcutLabel(customKeys[index + 6]),
                                                )
                                            },
                                            onClick = onEditSlot?.let { edit -> { edit(index + 6) } },
                                            onShortcut = onShortcut,
                                        )
                                    }
                                }
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
        val glowStroke = Stroke(width = 14.dp.toPx())
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())
        val brightness = light.brightness.coerceIn(0f, 1f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (light.effect) {
            CodexProtocol.effectSnake -> {
                val baseArgb = rgbColor(light.color).copy(alpha = brightness).toArgb()
                val transparent = Color.Transparent.toArgb()
                val angleDeg = phase
                val glowShader = AndroidSweepGradient(
                    cx, cy,
                    intArrayOf(
                        transparent, transparent, transparent,
                        android.graphics.Color.argb((0.15f * brightness * 255).toInt(),
                            android.graphics.Color.red(baseArgb),
                            android.graphics.Color.green(baseArgb),
                            android.graphics.Color.blue(baseArgb)),
                        android.graphics.Color.argb((0.85f * brightness * 255).toInt(),
                            android.graphics.Color.red(baseArgb),
                            android.graphics.Color.green(baseArgb),
                            android.graphics.Color.blue(baseArgb)),
                        baseArgb,
                        android.graphics.Color.argb((0.6f * brightness * 255).toInt(),
                            android.graphics.Color.red(baseArgb),
                            android.graphics.Color.green(baseArgb),
                            android.graphics.Color.blue(baseArgb)),
                        transparent,
                    ),
                    floatArrayOf(0f, 0.45f, 0.55f, 0.7f, 0.82f, 0.9f, 0.96f, 1f),
                ).also {
                    it.setLocalMatrix(Matrix().apply { setRotate(angleDeg, cx, cy) })
                }
                val coreShader = AndroidSweepGradient(
                    cx, cy,
                    intArrayOf(
                        transparent, transparent, transparent,
                        android.graphics.Color.argb((0.4f * brightness * 255).toInt(),
                            android.graphics.Color.red(baseArgb),
                            android.graphics.Color.green(baseArgb),
                            android.graphics.Color.blue(baseArgb)),
                        baseArgb,
                        android.graphics.Color.argb((0.5f * brightness * 255).toInt(),
                            android.graphics.Color.red(baseArgb),
                            android.graphics.Color.green(baseArgb),
                            android.graphics.Color.blue(baseArgb)),
                        transparent,
                    ),
                    floatArrayOf(0f, 0.5f, 0.6f, 0.78f, 0.88f, 0.95f, 1f),
                ).also {
                    it.setLocalMatrix(Matrix().apply { setRotate(angleDeg, cx, cy) })
                }
                drawRoundRect(
                    brush = ShaderBrush(glowShader),
                    cornerRadius = cornerRadius,
                    style = glowStroke,
                )
                drawRoundRect(
                    brush = ShaderBrush(coreShader),
                    cornerRadius = cornerRadius,
                    style = stroke,
                )
            }
            CodexProtocol.effectRainbow -> {
                val angleDeg = phase
                val rainbowColors = IntArray(37) { i ->
                    android.graphics.Color.HSVToColor(floatArrayOf(i * 10f, 0.85f, 1f))
                }
                val rainbowStops = FloatArray(37) { i -> i / 36f }
                val glowShader = AndroidSweepGradient(cx, cy, rainbowColors, rainbowStops).also {
                    it.setLocalMatrix(Matrix().apply { setRotate(angleDeg, cx, cy) })
                }
                val coreColors = IntArray(37) { i ->
                    android.graphics.Color.HSVToColor(floatArrayOf(i * 10f, 0.72f, 1f))
                }
                val coreShader = AndroidSweepGradient(cx, cy, coreColors, rainbowStops).also {
                    it.setLocalMatrix(Matrix().apply { setRotate(angleDeg, cx, cy) })
                }
                drawRoundRect(
                    brush = ShaderBrush(glowShader),
                    cornerRadius = cornerRadius,
                    style = glowStroke,
                    alpha = 0.35f * brightness,
                )
                drawRoundRect(
                    brush = ShaderBrush(coreShader),
                    cornerRadius = cornerRadius,
                    style = stroke,
                    alpha = brightness,
                )
            }
            CodexProtocol.effectBreath, CodexProtocol.effectShallowBreath -> {
                val glowAlpha = color.alpha * 0.35f
                drawRoundRect(
                    color = color.copy(alpha = glowAlpha),
                    cornerRadius = cornerRadius,
                    style = glowStroke,
                )
                drawRoundRect(color = color, cornerRadius = cornerRadius, style = stroke)
            }
            CodexProtocol.effectGradient -> {
                val baseColor = rgbColor(light.color)
                val shimmer = (sin(Math.toRadians(phase.toDouble())) * 0.5 + 0.5).toFloat()
                val highlight = baseColor.copy(alpha = (0.6f + 0.4f * shimmer) * brightness)
                drawRoundRect(
                    color = baseColor.copy(alpha = 0.2f * brightness),
                    cornerRadius = cornerRadius,
                    style = glowStroke,
                )
                drawRoundRect(color = highlight, cornerRadius = cornerRadius, style = stroke)
            }
            CodexProtocol.effectSolid -> {
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * 0.3f),
                    cornerRadius = cornerRadius,
                    style = glowStroke,
                )
                drawRoundRect(color = color, cornerRadius = cornerRadius, style = stroke)
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
    val brightness = light.brightness.coerceIn(0f, 1f)
    val alpha = when (light.effect) {
        CodexProtocol.effectBreath -> {
            val smooth = pulse * pulse * (3f - 2f * pulse)
            smooth * brightness
        }
        CodexProtocol.effectShallowBreath -> {
            val smooth = pulse * pulse * (3f - 2f * pulse)
            (0.55f + 0.45f * smooth) * brightness
        }
        CodexProtocol.effectSnake -> if (animateSnake) {
            val t = ((cos(Math.toRadians(phase.toDouble())) + 1.0) / 2.0)
            val smooth = t * t * (3.0 - 2.0 * t)
            (0.2f + 0.8f * smooth.pow(3.0)).toFloat() * brightness
        } else {
            brightness
        }
        else -> brightness
    }
    val color = if (light.effect == CodexProtocol.effectRainbow) {
        Color.hsv(phase, 0.8f, 1f)
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
            onShortcut = { _, _ -> },
            onJoystick = { _, _ -> },
            onCycleLayer = {},
            modifier = Modifier.fillMaxWidth().padding(12.dp).aspectRatio(0.94f),
        )
    }
}
