package com.patchself.codexmacro.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.protocol.ThreadLight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.roundToInt

private val keyShape = RoundedCornerShape(13.dp)

@Composable
fun AgentKey(
    index: Int,
    light: ThreadLight,
    enabled: Boolean,
    modifier: Modifier,
    onKey: (String, Int, Int?) -> Unit,
) {
    val statusColor = if (light.color == 0L) defaultAgentColor(index) else rgbColor(light.color)
    val transition = rememberInfiniteTransition(label = "agent-breath-$index")
    val breath by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "agent-breath-alpha-$index",
    )
    val lightAlpha = if (light.effect == "breath") breath else light.brightness.coerceIn(0.38f, 1f)
    HardwareKey(
        symbol = "+",
        description = "Agent ${index + 1}",
        enabled = enabled,
        glow = statusColor.copy(alpha = lightAlpha),
        modifier = modifier,
        onPress = { onKey("AG${index.toString().padStart(2, '0')}", 1, index) },
        onRelease = { onKey("AG${index.toString().padStart(2, '0')}", 0, index) },
    )
}

@Composable
fun CommandKey(
    keycap: CommandKeycap,
    id: String,
    enabled: Boolean,
    modifier: Modifier,
    onKey: (String, Int, Int?) -> Unit,
) {
    HardwareKey(
        symbol = if (keycap.iconRes() == null) keycap.glyph else null,
        iconRes = keycap.iconRes(),
        description = keycap.label,
        enabled = enabled,
        glow = Color(0x553ECFA4),
        modifier = modifier,
        onPress = { onKey(id, 1, null) },
        onRelease = { onKey(id, 0, null) },
    )
}

@DrawableRes
private fun CommandKeycap.iconRes(): Int? = when (this) {
    CommandKeycap.Fast -> R.drawable.ic_key_fast
    CommandKeycap.Approve -> R.drawable.ic_key_approve
    CommandKeycap.Decline -> R.drawable.ic_key_decline
    CommandKeycap.Fork -> R.drawable.ic_key_fork
    CommandKeycap.Mic -> R.drawable.ic_key_mic
    CommandKeycap.Codex -> R.drawable.ic_key_codex
    else -> null
}

@Composable
private fun HardwareKey(
    symbol: String? = null,
    @DrawableRes iconRes: Int? = null,
    description: String,
    enabled: Boolean,
    glow: Color,
    modifier: Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val face = if (pressed) Color(0xFFE6E4DE) else Color(0xFFF9F8F4)
    val content = if (glow.luminance() > 0.65f) Color(0xFF33302B) else Color(0xFF242320)
    Surface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.68f)
            .shadow(if (pressed) 2.dp else 7.dp, keyShape)
            .border(1.dp, glow, keyShape)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (!enabled) disabled()
            }
            .pointerInput(enabled, description) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onPress()
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                        onRelease()
                    }
                }
            },
        shape = keyShape,
        color = face,
        contentColor = content,
    ) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow, Color.Transparent))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(if (description == "Mic") 28.dp else 25.dp),
                    )
                } else {
                    Text(
                        text = symbol.orEmpty(),
                        fontSize = if (symbol.orEmpty().length > 3) 11.sp else 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(description, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DialControl(
    enabled: Boolean,
    modifier: Modifier,
    onKey: (String, Int, Int?) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var rotation by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.68f)
            .semantics { contentDescription = "Dial"; role = Role.Button; if (!enabled) disabled() }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val distance = hypot(
                        down.position.x - center.x,
                        down.position.y - center.y,
                    )
                    if (distance < minOf(size.width, size.height) * 0.28f) {
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onKey("ENC", 1, null)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            onKey("ENC", 0, null)
                        }
                    } else {
                        var lastAngle = atan2(
                            down.position.y - center.y,
                            down.position.x - center.x,
                        )
                        var accumulatedAngle = 0f
                        val stepAngle = (PI / 12).toFloat()
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.positionChanged()) {
                                val angle = atan2(
                                    change.position.y - center.y,
                                    change.position.x - center.x,
                                )
                                accumulatedAngle += normalizedAngle(angle - lastAngle)
                                lastAngle = angle
                                while (abs(accumulatedAngle) >= stepAngle) {
                                    val clockwise = accumulatedAngle > 0f
                                    onKey(if (clockwise) "ENC_CC" else "ENC_CW", 2, null)
                                    rotation += if (clockwise) 15f else -15f
                                    accumulatedAngle += if (clockwise) -stepAngle else stepAngle
                                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                }
                                change.consume()
                            }
                        } while (change.pressed)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize(0.78f).rotate(rotation).shadow(8.dp, CircleShape)
                .clip(CircleShape).background(Color(0xFFF2F3F0)).border(1.dp, Color(0xFFAAA8A2), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val middle = size.height / 2f
                clipRect(top = middle, bottom = size.height) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFAAA9A5), Color(0xFFFFFFFF)),
                            startY = middle,
                            endY = size.height,
                        ),
                    )
                }
                drawLine(
                    color = Color(0xFF868580),
                    start = Offset(0f, middle),
                    end = Offset(size.width, middle),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }
}

private fun normalizedAngle(angle: Float): Float = when {
    angle > PI -> (angle - 2 * PI).toFloat()
    angle < -PI -> (angle + 2 * PI).toFloat()
    else -> angle
}

@Composable
fun JoystickControl(
    enabled: Boolean,
    modifier: Modifier,
    onJoystick: (Double, Double) -> Unit,
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.68f)
            .semantics { contentDescription = "Analog stick"; role = Role.Button; if (!enabled) disabled() }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap) },
                    onDragEnd = { offset = Offset.Zero; onJoystick(0.0, 0.0) },
                    onDragCancel = { offset = Offset.Zero; onJoystick(0.0, 0.0) },
                ) { change, dragAmount ->
                    change.consume()
                    offset = (offset + dragAmount).let {
                        Offset(it.x.coerceIn(-24.dp.toPx(), 24.dp.toPx()), it.y.coerceIn(-24.dp.toPx(), 24.dp.toPx()))
                    }
                    if (abs(offset.x) + abs(offset.y) > 12.dp.toPx()) {
                        val angle = if (abs(offset.x) > abs(offset.y)) {
                            if (offset.x > 0) 0.0 else 0.5
                        } else {
                            if (offset.y > 0) 0.25 else 0.75
                        }
                        onJoystick(angle, 1.0)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize(0.8f).background(Color(0xFFD8D6D0), CircleShape), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize(0.72f).offset {
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                }.shadow(8.dp, CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFF383733), Color(0xFF090909))),
                    CircleShape,
                ),
            )
        }
    }
}

/** LayerControl cycles through six layers and renders the physical three-light code. */
@Composable
fun LayerControl(
    activeLayer: Int,
    onCycleLayer: () -> Unit,
    modifier: Modifier,
) {
    val layer = activeLayer.coerceIn(0, 5)
    val indicators = layerIndicatorStates(layer)
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Layer ${layer + 1}"
                stateDescription = "Layer ${layer + 1} of 6"
            }
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onCycleLayer()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(0.72f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                indicators.forEach { lit ->
                    Box(
                        Modifier
                            .width(10.dp)
                            .height(4.dp)
                            .background(
                                if (lit) Color(0xFF76E6BA) else Color(0xFFAAA9A3),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .shadow(5.dp, CircleShape)
                    .background(Color(0xFF11110F), CircleShape),
            )
        }
    }
}

internal fun layerIndicatorStates(layer: Int): List<Boolean> = when (layer.coerceIn(0, 5)) {
    0 -> listOf(true, false, false)
    1 -> listOf(false, true, false)
    2 -> listOf(false, false, true)
    3 -> listOf(true, true, false)
    4 -> listOf(false, true, true)
    else -> listOf(true, true, true)
}

internal fun rgbColor(value: Long): Color = Color((0xFF000000L or (value and 0xFFFFFF)).toInt())

private fun defaultAgentColor(index: Int): Color = listOf(
    Color(0xFF9D9AF4),
    Color(0xFF9DE7A6),
    Color(0xFFB8B6F8),
    Color(0xFFF5C7A4),
    Color(0xFFF3A5AA),
    Color(0xFFC7C7ED),
)[index]
