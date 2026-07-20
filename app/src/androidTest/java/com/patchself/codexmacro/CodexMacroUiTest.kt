package com.patchself.codexmacro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class CodexMacroUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun singleBoardExposesAllOfflineControls() {
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
        composeRule.onNodeWithText("Layer 1 keycap layout").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()
    }

    @Test
    fun settingsCustomizeCommandKeycapLegend() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Command key 1: Fast").performScrollTo().performClick()
        composeRule.onNodeWithText("Layer 1 · keycap 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Keycap Bug").performClick()
        composeRule.onNodeWithText("1 · Bug").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset").performClick()
    }

    @Test
    fun layerTouchCyclesAndPersistsVisibleLayer() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        composeRule.onNodeWithContentDescription("Layer 1").performClick()
        composeRule.onNodeWithContentDescription("Layer 2").assertIsDisplayed()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithContentDescription("Edit layer 1").performScrollTo().performClick()
    }
}
