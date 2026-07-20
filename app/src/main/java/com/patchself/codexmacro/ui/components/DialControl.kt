package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.ui.theme.CodexMacroTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** DialControl handles encoder rotation and center-button gestures. */
@Composable
fun DialControl(
    enabled: Boolean,
    modifier: Modifier = Modifier,
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
                    val distance = hypot(down.position.x - center.x, down.position.y - center.y)
                    if (distance < minOf(size.width, size.height) * 0.28f) {
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onKey("ENC", 1, null)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            onKey("ENC", 0, null)
                        }
                    } else {
                        var lastAngle = atan2(down.position.y - center.y, down.position.x - center.x)
                        var accumulatedAngle = 0f
                        val stepAngle = (PI / 12).toFloat()
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.positionChanged()) {
                                val angle = atan2(change.position.y - center.y, change.position.x - center.x)
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
                            colors = listOf(Color(0xFFAAA9A5), Color.White),
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

@Preview(name = "Dial control", widthDp = 112, heightDp = 112, showBackground = true)
@Composable
private fun DialControlPreview() {
    CodexMacroTheme {
        DialControl(
            enabled = true,
            modifier = Modifier.fillMaxSize(),
            onKey = { _, _, _ -> },
        )
    }
}
