package com.patchself.codexmacro.bluetooth_test

import com.patchself.codexmacro.bluetooth.CommandKeycap
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandKeycapTest {
    @Test
    fun keycapCatalogContainsAllCodexIconChoices() {
        assertEquals(32, CommandKeycap.entries.size)
    }

    @Test
    fun encodedLayoutRoundTripsSixCommandPositions() {
        val layout = listOf(
            CommandKeycap.Yolo,
            CommandKeycap.Terminal,
            CommandKeycap.Review,
            CommandKeycap.BranchAdd,
            CommandKeycap.Mic,
            CommandKeycap.Yeet,
        )

        assertEquals(layout, CommandKeycap.decodeLayout(CommandKeycap.encodeLayout(layout)))
    }

    @Test
    fun incompleteStoredLayoutFallsBackToHardwareDefaults() {
        assertEquals(CommandKeycap.defaultLayout, CommandKeycap.decodeLayout("fast,approve"))
    }

    @Test
    fun encodedLayersRoundTripIndependentLayouts() {
        val layers = CommandKeycap.defaultLayers.map { it.toMutableList() }
        layers[1][0] = CommandKeycap.Bug
        layers[5][5] = CommandKeycap.Yeet

        assertEquals(layers, CommandKeycap.decodeLayers(CommandKeycap.encodeLayers(layers)))
    }
}
