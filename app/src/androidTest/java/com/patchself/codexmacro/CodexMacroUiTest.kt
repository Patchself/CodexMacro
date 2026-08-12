package com.patchself.codexmacro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test

class CodexMacroUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun singleBoardExposesAllOfflineControls() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithContentDescription("Agent 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Fast").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Mic").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Dial").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Analog stick").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Layer", substring = true).assertIsDisplayed()
    }

    @Test
    fun settingsExposeConnectionCompatibilityOptions() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Stable connection mode").assertIsDisplayed()
        composeRule.onNodeWithText("Auto resume").assertIsDisplayed()
        composeRule.onNodeWithText("Customize layers").assertIsDisplayed()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithText("Layer 1 · Codex").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Done").performClick()
    }

    @Test
    fun settingsCustomizeCommandKeycapLegend() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Key 1: Fast").performScrollTo().performClick()
        composeRule.onNodeWithText("Layer 1 · Codex key 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Icon Bug").performClick()
        composeRule.onNodeWithContentDescription("Key 1: Bug").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").performClick()
    }

    @Test
    fun customLayerEditsAllKeysAndKeyboardShortcut() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 2").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Key 1: 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Layer 2 · custom key 1").assertIsDisplayed()
        composeRule.onNodeWithText("Upload icon").assertIsDisplayed()
        composeRule.onNodeWithText("Command").performClick()
        composeRule.onNodeWithText("Shortcut · Command + 1").assertIsDisplayed()
    }

    @Test
    fun layerPagerSwipesAndPersistsVisibleLayer() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            "Swipe to preview and edit every complete controller layer.",
        ).performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Layer 2 · Custom").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithContentDescription("Layer 2").assertIsDisplayed()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Customize layers").performClick()
        composeRule.onNodeWithText("Layer 2 · Custom").performScrollTo().assertIsDisplayed()
    }
}
