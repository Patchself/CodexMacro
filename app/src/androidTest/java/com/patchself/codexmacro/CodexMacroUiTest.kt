package com.patchself.codexmacro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithContentDescription("Layer").assertIsDisplayed()
    }

    @Test
    fun settingsExposeConnectionCompatibilityOptions() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Stable connection mode").assertIsDisplayed()
        composeRule.onNodeWithText("Auto resume").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
    }
}
