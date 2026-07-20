package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** LayerControl cycles through six layers and renders the physical three-light code. */
@Composable
fun LayerControl(
    activeLayer: Int,
    onCycleLayer: () -> Unit,
    modifier: Modifier = Modifier,
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
                        Modifier.width(10.dp).height(4.dp).background(
                            if (lit) Color(0xFF76E6BA) else Color(0xFFAAA9A3),
                            RoundedCornerShape(2.dp),
                        ),
                    )
                }
            }
            Box(
                Modifier.weight(1f).aspectRatio(1f).shadow(5.dp, CircleShape)
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
