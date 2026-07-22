package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

/** ControllerSettingsDialog edits connection behavior and per-layer keycaps. */
@Composable
fun ControllerSettingsDialog(
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F2EF),
            shadowElevation = 20.dp,
        ) {
            val slot = editingSlot
            if (slot == null) {
                SettingsContent(settings, onSettingsChange, onDismiss, onEditSlot = { editingSlot = it })
            } else {
                val activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1)
                val layers = CommandKeycap.normalizeLayers(settings.layerKeycaps)
                KeycapPicker(
                    layer = activeLayer,
                    slot = slot,
                    selected = layers[activeLayer][slot],
                    onSelect = { selected ->
                        val updatedLayers = layers.map { it.toMutableList() }.toMutableList()
                        updatedLayers[activeLayer][slot] = selected
                        onSettingsChange(settings.copy(layerKeycaps = updatedLayers))
                        editingSlot = null
                    },
                    onBack = { editingSlot = null },
                )
            }
        }
    }
}

@Composable
internal fun SettingsContent(
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onDismiss: () -> Unit,
    onEditSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(22.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Controller settings", color = Color(0xFF181714), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "Connection compatibility and the keycaps shown on this controller.",
            color = Color(0xFF656159),
            fontSize = 12.sp,
        )
        SettingToggle(
            title = "Stable connection mode",
            description = "Keep the Codex Micro name, GATT services, and host connection active after Stop.",
            checked = settings.stableConnection,
            onCheckedChange = { onSettingsChange(settings.copy(stableConnection = it)) },
        )
        SettingToggle(
            title = "Auto resume",
            description = "Restore a running controller after process recovery or device reboot.",
            checked = settings.autoResume,
            onCheckedChange = { onSettingsChange(settings.copy(autoResume = it)) },
        )
        SettingToggle(
            title = "Show key labels",
            description = "Display text labels below the virtual controller keys.",
            checked = settings.showKeyLabels,
            onCheckedChange = { onSettingsChange(settings.copy(showKeyLabels = it)) },
        )
        KeycapLayout(settings, onSettingsChange, onEditSlot)
        Spacer(Modifier.padding(top = 2.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF24231F),
                contentColor = Color.White,
            ),
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF24231F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color(0xFF77736B), fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Preview(name = "Controller settings", widthDp = 440, heightDp = 720, showBackground = true)
@Composable
private fun ControllerSettingsPreview() {
    CodexMacroTheme {
        SettingsContent(
            settings = ControllerSettings(stableConnection = true, activeLayer = 2),
            onSettingsChange = {},
            onDismiss = {},
            onEditSlot = {},
        )
    }
}
