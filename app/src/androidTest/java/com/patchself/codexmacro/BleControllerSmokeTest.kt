package com.patchself.codexmacro

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BleControllerSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startPauseAndResumeKeepBluetoothIdentity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        val originalName = adapter.name
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Stable connection mode").performClick()
        composeRule.onNodeWithText("Done").performClick()

        try {
            composeRule.onNodeWithText("Start").performClick()
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText("PAIRING").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithText("CONNECTED").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("Codex Micro", adapter.name)

            composeRule.onNodeWithText("Stop").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("OFFLINE").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("OFFLINE").assertIsDisplayed()
            assertEquals("Codex Micro", adapter.name)

            composeRule.onNodeWithText("Start").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("PAIRING").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithText("CONNECTED").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("Codex Micro", adapter.name)
        } finally {
            val stopNodes = composeRule.onAllNodesWithText("Stop").fetchSemanticsNodes()
            if (stopNodes.isNotEmpty()) composeRule.onNodeWithText("Stop").performClick()
            composeRule.onNodeWithText("Settings").performClick()
            composeRule.onNodeWithText("Stable connection mode").performClick()
            composeRule.onNodeWithText("Done").performClick()
        }

        composeRule.waitUntil(timeoutMillis = 10_000) { adapter.name == originalName }
        assertEquals(originalName, adapter.name)
    }
}
