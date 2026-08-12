package com.patchself.codexmacro.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.ControllerSettings
import com.patchself.codexmacro.bluetooth.CustomKeyBinding
import com.patchself.codexmacro.bluetooth.KeyboardKey
import com.patchself.codexmacro.bluetooth.KeyboardModifier
import com.patchself.codexmacro.ui.theme.CodexMacroTheme

@Composable
internal fun LayerKeyLayout(
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onEditSlot: (Int) -> Unit,
) {
    val activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1)
    val isCodexLayer = activeLayer == 0
    val customLayout = if (isCodexLayer) null else CustomKeyBinding.normalizeLayers(settings.customLayers)[activeLayer - 1]
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (isCodexLayer) {
                    stringResource(R.string.layer_codex_title)
                } else {
                    stringResource(R.string.layer_custom_title, activeLayer + 1)
                },
                color = Color(0xFF24231F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (isCodexLayer) {
                    stringResource(R.string.layer_codex_description)
                } else {
                    stringResource(R.string.layer_custom_description)
                },
                color = Color(0xFF77736B),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        TextButton(
            onClick = {
                if (isCodexLayer) {
                    onSettingsChange(settings.copy(codexKeycaps = CommandKeycap.defaultLayout))
                } else {
                    val layers = CustomKeyBinding.normalizeLayers(settings.customLayers).toMutableList()
                    layers[activeLayer - 1] = CustomKeyBinding.defaultLayout
                    onSettingsChange(settings.copy(customLayers = layers))
                }
            },
        ) { Text(stringResource(R.string.action_reset)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(CommandKeycap.layerCount) { layer ->
            val editLayerDescription = stringResource(R.string.layer_edit_description, layer + 1)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = editLayerDescription; role = Role.Button }
                    .clickable { onSettingsChange(settings.copy(activeLayer = layer)) },
                shape = RoundedCornerShape(9.dp),
                color = if (layer == activeLayer) Color(0xFFC8EBD9) else Color(0xFFE2E5E1),
            ) {
                Text(
                    text = if (layer == 0) "1 C" else "${layer + 1}",
                    modifier = Modifier.padding(vertical = 7.dp),
                    color = Color(0xFF24231F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    val slots = if (isCodexLayer) 6 else CustomKeyBinding.keyCount
    repeat((slots + 1) / 2) { rowIndex ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) { columnIndex ->
                val slot = rowIndex * 2 + columnIndex
                if (slot < slots) {
                    if (isCodexLayer) {
                        val keycap = settings.codexKeycaps[slot]
                        KeySlot(
                            slot = slot,
                            title = localizedKeycapLabel(keycap),
                            keycap = keycap,
                            modifier = Modifier.weight(1f),
                        ) { onEditSlot(slot) }
                    } else {
                        val binding = customLayout!![slot]
                        KeySlot(
                            slot = slot,
                            title = localizedShortcutLabel(binding),
                            keycap = binding.keycap,
                            customIconUri = binding.customIconUri,
                            modifier = Modifier.weight(1f),
                        ) { onEditSlot(slot) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeySlot(
    slot: Int,
    title: String,
    keycap: CommandKeycap,
    modifier: Modifier,
    customIconUri: String? = null,
    onClick: () -> Unit,
) {
    val keyDescription = stringResource(R.string.key_description, slot + 1, title)
    Surface(
        modifier = modifier
            .semantics { contentDescription = keyDescription; role = Role.Button }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE2E5E1),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            KeycapArtwork(keycap, customIconUri, Modifier.size(18.dp))
            Text(
                text = "${slot + 1} · $title",
                modifier = Modifier.padding(start = 8.dp),
                color = Color(0xFF555149),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun CodexKeycapPicker(
    slot: Int,
    selected: CommandKeycap,
    onSelect: (CommandKeycap) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(22.dp).heightIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.codex_key_title, slot + 1), color = Color(0xFF181714), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.codex_key_description), color = Color(0xFF656159), fontSize = 12.sp)
        IconGrid(selected, null, Modifier.fillMaxWidth().weight(1f)) { keycap -> onSelect(keycap) }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.action_back), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CustomKeyPicker(
    layer: Int,
    slot: Int,
    selected: CustomKeyBinding,
    onSave: (CustomKeyBinding) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var keycap by remember(selected) { mutableStateOf(selected.keycap) }
    var customIconUri by remember(selected) { mutableStateOf(selected.customIconUri) }
    var key by remember(selected) { mutableStateOf(selected.key) }
    var modifiers by remember(selected) { mutableIntStateOf(selected.modifiers) }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            customIconUri = uri.toString()
        }
    }
    Column(
        modifier = Modifier.padding(22.dp).heightIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.custom_key_title, layer + 1, slot + 1), color = Color(0xFF181714), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.icon_title), color = Color(0xFF24231F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        IconGrid(
            selected = keycap,
            customIconUri = customIconUri,
            modifier = Modifier.fillMaxWidth().height(178.dp),
        ) {
            keycap = it
            customIconUri = null
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { iconPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
            ) { Text(stringResource(R.string.action_upload_icon)) }
            if (customIconUri != null) {
                KeycapArtwork(keycap, customIconUri, Modifier.size(36.dp).align(Alignment.CenterVertically))
                TextButton(onClick = { customIconUri = null }) { Text(stringResource(R.string.action_remove_upload)) }
            }
        }
        Text(
            stringResource(
                R.string.shortcut_title,
                localizedShortcutLabel(CustomKeyBinding(keycap, customIconUri, key, modifiers)),
            ),
            color = Color(0xFF24231F),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KeyboardModifier.entries.forEach { modifier ->
                FilterChip(
                    selected = modifiers and modifier.mask != 0,
                    onClick = { modifiers = modifiers xor modifier.mask },
                    label = { Text(modifier.label) },
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(KeyboardKey.entries, key = KeyboardKey::storageId) { candidate ->
                Surface(
                    modifier = Modifier.height(38.dp).clickable { key = candidate },
                    shape = RoundedCornerShape(9.dp),
                    color = if (candidate == key) Color(0xFFC8EBD9) else Color(0xFFE2E5E1),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(candidate.label, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Button(onClick = { onSave(CustomKeyBinding(keycap, customIconUri, key, modifiers)) }) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun IconGrid(
    selected: CommandKeycap,
    customIconUri: String?,
    modifier: Modifier,
    onSelect: (CommandKeycap) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CommandKeycap.entries, key = CommandKeycap::storageId) { keycap ->
            val label = localizedKeycapLabel(keycap)
            val iconDescription = stringResource(R.string.icon_description, label)
            Surface(
                modifier = Modifier
                    .height(70.dp)
                    .semantics { contentDescription = iconDescription; role = Role.Button }
                    .clickable { onSelect(keycap) },
                shape = RoundedCornerShape(13.dp),
                color = if (keycap == selected && customIconUri == null) Color(0xFFC8EBD9) else Color(0xFFE2E5E1),
            ) {
                Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KeycapArtwork(keycap, modifier = Modifier.size(22.dp))
                        Text(label, color = Color(0xFF555149), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Preview(name = "Custom key picker", widthDp = 440, heightDp = 700, showBackground = true)
@Composable
private fun CustomKeyPickerPreview() {
    CodexMacroTheme {
        CustomKeyPicker(layer = 1, slot = 0, selected = CustomKeyBinding(), onSave = {}, onBack = {})
    }
}
