package com.patchself.codexmacro

import androidx.compose.ui.graphics.toArgb
import com.patchself.codexmacro.ui.components.rgbColor
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentColorTest {
    @Test
    fun rgbColorUsesSrgbArgbEncoding() {
        val color = rgbColor(0x123456)
        val translucent = color.copy(alpha = 0.5f)

        assertEquals(0xFF123456.toInt(), color.toArgb())
        assertEquals(0x80123456.toInt(), translucent.toArgb())
    }
}
