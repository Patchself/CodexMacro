package com.patchself.codexmacro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.protocol.ThreadLight
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

private val keyShape = RoundedCornerShape(13.dp)

/** AgentKey renders a thread-aware hardware key and emits press events. */
@Composable
fun AgentKey(
    index: Int,
    light: ThreadLight,
    enabled: Boolean,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
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
        showLabel = showLabel,
        enabled = enabled,
        glow = statusColor.copy(alpha = lightAlpha),
        modifier = modifier,
        onPress = { onKey("AG${index.toString().padStart(2, '0')}", 1, index) },
        onRelease = { onKey("AG${index.toString().padStart(2, '0')}", 0, index) },
    )
}

/** CommandKey renders a configurable command keycap and emits press events. */
@Composable
fun CommandKey(
    keycap: CommandKeycap,
    id: String,
    enabled: Boolean,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    onKey: (String, Int, Int?) -> Unit,
) {
    HardwareKey(
        symbol = if (keycap.iconRes() == null) keycap.glyph else null,
        iconRes = keycap.iconRes(),
        description = keycap.label,
        showLabel = showLabel,
        enabled = enabled,
        glow = Color(0x553ECFA4),
        modifier = modifier,
        onPress = { onKey(id, 1, null) },
        onRelease = { onKey(id, 0, null) },
    )
}

@DrawableRes
internal fun CommandKeycap.iconRes(): Int? = when (this) {
    CommandKeycap.Bug -> R.drawable.ic_key_bug
    CommandKeycap.Codex -> R.drawable.ic_key_codex
    CommandKeycap.Terminal -> R.drawable.ic_key_terminal
    CommandKeycap.Download -> R.drawable.ic_key_download
    CommandKeycap.Trash -> R.drawable.ic_key_trash
    CommandKeycap.Edit -> R.drawable.ic_key_edit
    CommandKeycap.Send -> R.drawable.ic_key_send
    CommandKeycap.Spark -> R.drawable.ic_key_spark
    CommandKeycap.NewChat -> R.drawable.ic_key_new_chat
    CommandKeycap.Run -> R.drawable.ic_key_run
    CommandKeycap.Branches -> R.drawable.ic_key_branches
    CommandKeycap.Fork -> R.drawable.ic_key_fork
    CommandKeycap.BranchAdd -> R.drawable.ic_key_branch_add
    CommandKeycap.Merge -> R.drawable.ic_key_merge
    CommandKeycap.Plug -> R.drawable.ic_key_plug
    CommandKeycap.Experiment -> R.drawable.ic_key_experiment
    CommandKeycap.Review -> R.drawable.ic_key_review
    CommandKeycap.History -> R.drawable.ic_key_history
    CommandKeycap.Think -> R.drawable.ic_key_think
    CommandKeycap.Link -> R.drawable.ic_key_link
    CommandKeycap.Fast -> R.drawable.ic_key_fast
    CommandKeycap.Approve -> R.drawable.ic_key_approve
    CommandKeycap.Decline -> R.drawable.ic_key_decline
    CommandKeycap.Redirect -> R.drawable.ic_key_redirect
    CommandKeycap.Settings -> R.drawable.ic_key_settings
    CommandKeycap.FolderAdd -> R.drawable.ic_key_folder_add
    CommandKeycap.Upload -> R.drawable.ic_key_upload
    CommandKeycap.Apps -> R.drawable.ic_key_apps
    CommandKeycap.Mic -> R.drawable.ic_key_mic
    CommandKeycap.Assistant -> R.drawable.ic_key_assistant
    CommandKeycap.Yolo,
    CommandKeycap.Yeet,
    -> null
}

@Composable
internal fun KeycapArtwork(
    keycap: CommandKeycap,
    modifier: Modifier = Modifier,
) {
    val iconRes = keycap.iconRes()
    if (iconRes != null) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Text(
            text = keycap.glyph,
            color = Color(0xFF24231F),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HardwareKey(
    symbol: String? = null,
    @DrawableRes iconRes: Int? = null,
    description: String,
    showLabel: Boolean,
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
    val visibleGlow = if (enabled) glow else Color.Transparent
    Surface(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.68f)
            .shadow(if (!enabled) 0.dp else if (pressed) 2.dp else 7.dp, keyShape)
            .border(1.dp, if (enabled) glow else Color(0xFFB8BFBB), keyShape)
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
        Box(
            Modifier.fillMaxSize().background(Brush.radialGradient(listOf(visibleGlow, Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) {
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
                if (showLabel) {
                    Text(description, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
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

@Preview(name = "Agent key", widthDp = 112, heightDp = 112, showBackground = true)
@Composable
private fun AgentKeyPreview() {
    CodexMacroTheme {
        AgentKey(
            index = 0,
            light = ThreadLight(color = 0x9D9AF4, brightness = 1f),
            enabled = true,
            showLabel = true,
            modifier = Modifier.size(96.dp),
        ) { _, _, _ -> }
    }
}

@Preview(name = "Command key", widthDp = 112, heightDp = 112, showBackground = true)
@Composable
private fun CommandKeyPreview() {
    CodexMacroTheme {
        CommandKey(
            keycap = CommandKeycap.Approve,
            id = "ACT07",
            enabled = true,
            showLabel = true,
            modifier = Modifier.size(96.dp),
        ) { _, _, _ -> }
    }
}
