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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

/** ControllerSettingsDialog edits connection behavior, Codex icons, and custom layer shortcuts. */
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
                if (activeLayer == 0) {
                    val keycaps = CommandKeycap.normalizeLayout(settings.codexKeycaps)
                    CodexKeycapPicker(
                        slot = slot,
                        selected = keycaps[slot],
                        onSelect = { selected ->
                            val updated = keycaps.toMutableList()
                            updated[slot] = selected
                            onSettingsChange(settings.copy(codexKeycaps = updated))
                            editingSlot = null
                        },
                        onBack = { editingSlot = null },
                    )
                } else {
                    val layers = com.patchself.codexmacro.bluetooth.CustomKeyBinding.normalizeLayers(settings.customLayers)
                    CustomKeyPicker(
                        layer = activeLayer,
                        slot = slot,
                        selected = layers[activeLayer - 1][slot],
                        onSave = { selected ->
                            val updatedLayers = layers.map { it.toMutableList() }.toMutableList()
                            updatedLayers[activeLayer - 1][slot] = selected
                            onSettingsChange(settings.copy(customLayers = updatedLayers))
                            editingSlot = null
                        },
                        onBack = { editingSlot = null },
                    )
                }
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
        Text(stringResource(R.string.settings_title), color = Color(0xFF181714), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            stringResource(R.string.settings_description),
            color = Color(0xFF656159),
            fontSize = 12.sp,
        )
        SettingToggle(
            title = stringResource(R.string.settings_stable_connection),
            description = stringResource(R.string.settings_stable_connection_description),
            checked = settings.stableConnection,
            onCheckedChange = { onSettingsChange(settings.copy(stableConnection = it)) },
        )
        SettingToggle(
            title = stringResource(R.string.settings_auto_resume),
            description = stringResource(R.string.settings_auto_resume_description),
            checked = settings.autoResume,
            onCheckedChange = { onSettingsChange(settings.copy(autoResume = it)) },
        )
        SettingToggle(
            title = stringResource(R.string.settings_show_key_labels),
            description = stringResource(R.string.settings_show_key_labels_description),
            checked = settings.showKeyLabels,
            onCheckedChange = { onSettingsChange(settings.copy(showKeyLabels = it)) },
        )
        SettingToggle(
            title = stringResource(R.string.settings_bluetooth_logging),
            description = stringResource(R.string.settings_bluetooth_logging_description),
            checked = settings.bluetoothDataLogging,
            onCheckedChange = { onSettingsChange(settings.copy(bluetoothDataLogging = it)) },
        )
        LayerKeyLayout(settings, onSettingsChange, onEditSlot)
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
            Text(stringResource(R.string.action_done), fontWeight = FontWeight.Bold)
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
