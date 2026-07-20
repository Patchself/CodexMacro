package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.patchself.codexmacro.ui.theme.CodexMacroTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** JoystickControl converts drag gestures into radial controller input. */
@Composable
fun JoystickControl(
    enabled: Boolean,
    modifier: Modifier = Modifier,
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
                var dragOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        dragOffset = Offset.Zero
                        offset = Offset.Zero
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        offset = Offset.Zero
                        onJoystick(0.0, 0.0)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        offset = Offset.Zero
                        onJoystick(0.0, 0.0)
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    val maxOffset = 24.dp.toPx()
                    val dragDistance = hypot(dragOffset.x, dragOffset.y)
                    offset = if (dragDistance > maxOffset) {
                        dragOffset * (maxOffset / dragDistance)
                    } else {
                        dragOffset
                    }
                    val deadZone = 12.dp.toPx()
                    if (dragDistance > deadZone) {
                        val angle = (atan2(dragOffset.y, dragOffset.x) / (2 * PI) + 1.0) % 1.0
                        val distance = ((dragDistance - deadZone) / (maxOffset - deadZone))
                            .coerceIn(0f, 1f)
                        onJoystick(angle, distance.toDouble())
                    } else {
                        onJoystick(0.0, 0.0)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize(0.8f)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.14f),
                )
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFFDEDDD8),
                            0.82f to Color(0xFFD8D6D0),
                            1f to Color(0xFFC9C7C1),
                        ),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .fillMaxSize(0.576f)
                .zIndex(1f)
                .offset {
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                }
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.24f),
                    spotColor = Color.Black.copy(alpha = 0.32f),
                )
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF3B3A36),
                            0.78f to Color(0xFF171716),
                            1f to Color(0xFF0B0B0B),
                        ),
                    ),
                    CircleShape,
                ),
        )
    }
}

@Preview(name = "Joystick control", widthDp = 112, heightDp = 112, showBackground = true)
@Composable
private fun JoystickControlPreview() {
    CodexMacroTheme {
        JoystickControl(
            enabled = true,
            modifier = Modifier.fillMaxSize(),
            onJoystick = { _, _ -> },
        )
    }
}
