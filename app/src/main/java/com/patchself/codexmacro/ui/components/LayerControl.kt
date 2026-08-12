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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.ui.theme.CodexMacroTheme
import com.patchself.codexmacro.R

/** LayerControl cycles through six layers and renders the physical three-light code. */
@Composable
fun LayerControl(
    activeLayer: Int,
    enabled: Boolean,
    onCycleLayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layer = activeLayer.coerceIn(0, 5)
    val indicators = layerIndicatorStates(layer)
    val haptics = LocalHapticFeedback.current
    val layerDescription = stringResource(R.string.layer_description, layer + 1)
    val layerStateDescription = stringResource(R.string.layer_state_description, layer + 1, 6)
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.68f)
            .semantics {
                contentDescription = layerDescription
                stateDescription = layerStateDescription
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, role = Role.Button) {
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
                Modifier.weight(1f).aspectRatio(1f).shadow(if (enabled) 5.dp else 0.dp, CircleShape)
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

@Preview(name = "Layer control", widthDp = 112, heightDp = 112, showBackground = true)
@Composable
private fun LayerControlPreview() {
    CodexMacroTheme {
        LayerControl(
            activeLayer = 3,
            enabled = true,
            onCycleLayer = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
