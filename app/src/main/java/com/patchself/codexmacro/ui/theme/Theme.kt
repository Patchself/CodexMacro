package com.patchself.codexmacro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ControllerColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Ink,
    primaryContainer = ElectricBlueDark,
    onPrimaryContainer = SoftWhite,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF164D3A),
    onSecondaryContainer = SoftWhite,
    tertiary = Amber,
    background = Ink,
    onBackground = SoftWhite,
    surface = Panel,
    onSurface = SoftWhite,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelRaised,
    onSurfaceVariant = Color(0xFFB8C0CC),
    outline = Color(0xFF697382),
    error = Color(0xFFFF7B83),
)

@Composable
fun CodexMacroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ControllerColorScheme,
        typography = Typography,
        content = content,
    )
}
