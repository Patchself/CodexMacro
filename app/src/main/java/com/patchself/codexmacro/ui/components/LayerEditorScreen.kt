package com.patchself.codexmacro.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.bluetooth.CustomKeyBinding

/** LayerEditorScreen previews and edits every complete controller layer. */
@Composable
internal fun LayerEditorScreen(
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onBack: () -> Unit,
) {
    var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = editingSlot != null) { editingSlot = null }
    val slot = editingSlot
    if (slot != null) {
        LayerKeyPicker(settings, slot, onSettingsChange) { editingSlot = null }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.layer_editor_title),
                    color = Color(0xFF181714),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(R.string.layer_editor_description),
                    color = Color(0xFF656159),
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.action_back), fontWeight = FontWeight.Bold)
            }
        }
        LayerKeyLayout(settings, onSettingsChange) { editingSlot = it }
    }
}

@Composable
private fun LayerKeyPicker(
    settings: ControllerSettings,
    slot: Int,
    onSettingsChange: (ControllerSettings) -> Unit,
    onBack: () -> Unit,
) {
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
                onBack()
            },
            onBack = onBack,
        )
        return
    }
    val layers = CustomKeyBinding.normalizeLayers(settings.customLayers)
    CustomKeyPicker(
        layer = activeLayer,
        slot = slot,
        selected = layers[activeLayer - 1][slot],
        onSave = { selected ->
            val updatedLayers = layers.map { it.toMutableList() }.toMutableList()
            updatedLayers[activeLayer - 1][slot] = selected
            onSettingsChange(settings.copy(customLayers = updatedLayers))
            onBack()
        },
        onBack = onBack,
    )
}
