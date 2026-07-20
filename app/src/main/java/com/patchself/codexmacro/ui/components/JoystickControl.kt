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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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
                    if (abs(dragOffset.x) + abs(dragOffset.y) > 12.dp.toPx()) {
                        val angle = if (abs(dragOffset.x) > abs(dragOffset.y)) {
                            if (dragOffset.x > 0) 0.0 else 0.5
                        } else {
                            if (dragOffset.y > 0) 0.25 else 0.75
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
