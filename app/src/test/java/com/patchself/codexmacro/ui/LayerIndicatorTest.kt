package com.patchself.codexmacro.ui_test

import com.patchself.codexmacro.ui.components.layerIndicatorStates
import org.junit.Assert.assertEquals
import org.junit.Test

class LayerIndicatorTest {
    @Test
    fun indicatorStatesMatchCreatorMicroLayerCodes() {
        assertEquals(listOf(true, false, false), layerIndicatorStates(0))
        assertEquals(listOf(false, true, false), layerIndicatorStates(1))
        assertEquals(listOf(false, false, true), layerIndicatorStates(2))
        assertEquals(listOf(true, true, false), layerIndicatorStates(3))
        assertEquals(listOf(false, true, true), layerIndicatorStates(4))
        assertEquals(listOf(true, true, true), layerIndicatorStates(5))
    }
}
