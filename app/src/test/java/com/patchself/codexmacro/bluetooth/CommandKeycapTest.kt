package com.patchself.codexmacro.bluetooth_test

import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.CustomKeyBinding
import com.patchself.codexmacro.bluetooth.KeyboardKey
import com.patchself.codexmacro.bluetooth.KeyboardModifier
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
    fun customLayersRoundTripIconsUploadsAndShortcuts() {
        val layers = CustomKeyBinding.defaultLayers.map { it.toMutableList() }
        layers[0][0] = CustomKeyBinding(
            keycap = CommandKeycap.Bug,
            customIconUri = "content://icons/bug",
            key = KeyboardKey.K,
            modifiers = KeyboardModifier.Command.mask or KeyboardModifier.Shift.mask,
        )
        layers[4][11] = CustomKeyBinding(keycap = CommandKeycap.Yeet, key = KeyboardKey.F12)

        assertEquals(layers, CustomKeyBinding.decodeLayers(CustomKeyBinding.encodeLayers(layers)))
    }

    @Test
    fun malformedCustomLayersFallBackToDefaults() {
        assertEquals(CustomKeyBinding.defaultLayers, CustomKeyBinding.decodeLayers("[]"))
    }
}
