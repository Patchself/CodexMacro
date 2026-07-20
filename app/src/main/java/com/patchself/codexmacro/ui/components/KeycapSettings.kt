package com.patchself.codexmacro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.ControllerSettings

@Composable
internal fun KeycapLayout(
    settings: ControllerSettings,
    onSettingsChange: (ControllerSettings) -> Unit,
    onEditSlot: (Int) -> Unit,
) {
    val activeLayer = settings.activeLayer.coerceIn(0, CommandKeycap.layerCount - 1)
    val layers = CommandKeycap.normalizeLayers(settings.layerKeycaps)
    val layout = layers[activeLayer]
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Layer ${activeLayer + 1} keycap layout",
                color = Color(0xFF24231F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Each layer keeps its own six legends. Control actions remain mapped by position on the host.",
                color = Color(0xFF77736B),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        TextButton(
            onClick = {
                val updatedLayers = layers.toMutableList()
                updatedLayers[activeLayer] = CommandKeycap.defaultLayout
                onSettingsChange(settings.copy(layerKeycaps = updatedLayers))
            },
        ) {
            Text("Reset")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(CommandKeycap.layerCount) { layer ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Edit layer ${layer + 1}"; role = Role.Button }
                    .clickable { onSettingsChange(settings.copy(activeLayer = layer)) },
                shape = RoundedCornerShape(9.dp),
                color = if (layer == activeLayer) Color(0xFFC8EBD9) else Color(0xFFE2E5E1),
            ) {
                Text(
                    text = "${layer + 1}",
                    modifier = Modifier.padding(vertical = 7.dp),
                    color = Color(0xFF24231F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    layout.chunked(2).forEachIndexed { rowIndex, rowKeycaps ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowKeycaps.forEachIndexed { columnIndex, keycap ->
                val slot = rowIndex * 2 + columnIndex
                KeycapSlot(slot, keycap, Modifier.weight(1f)) { onEditSlot(slot) }
            }
        }
    }
}

@Composable
private fun KeycapSlot(
    slot: Int,
    keycap: CommandKeycap,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .semantics { contentDescription = "Command key ${slot + 1}: ${keycap.label}"; role = Role.Button }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE2E5E1),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(keycap.glyph, color = Color(0xFF24231F), fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(
                text = "${slot + 1} · ${keycap.label}",
                modifier = Modifier.padding(start = 8.dp),
                color = Color(0xFF555149),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun KeycapPicker(
    layer: Int,
    slot: Int,
    selected: CommandKeycap,
    onSelect: (CommandKeycap) -> Unit,
    onBack: () -> Unit,
) {
    val commandId = listOf("ACT06", "ACT07", "ACT08", "ACT09", "ACT10", "ACT12")[slot]
    Column(
        modifier = Modifier.padding(22.dp).heightIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Layer ${layer + 1} · keycap ${slot + 1}",
            color = Color(0xFF181714),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "This changes the Android legend for $commandId on layer ${layer + 1}.",
            color = Color(0xFF656159),
            fontSize = 12.sp,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(CommandKeycap.entries, key = CommandKeycap::storageId) { keycap ->
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .semantics { contentDescription = "Keycap ${keycap.label}"; role = Role.Button }
                        .clickable { onSelect(keycap) },
                    shape = RoundedCornerShape(13.dp),
                    color = if (keycap == selected) Color(0xFFC8EBD9) else Color(0xFFE2E5E1),
                ) {
                    Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(keycap.glyph, color = Color(0xFF24231F), fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(
                                keycap.label,
                                color = Color(0xFF555149),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text("Back", fontWeight = FontWeight.Bold)
        }
    }
}
