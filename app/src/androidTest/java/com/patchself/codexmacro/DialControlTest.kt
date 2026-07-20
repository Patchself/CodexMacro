package com.patchself.codexmacro

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.patchself.codexmacro.ui.components.DialControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DialControlTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun centerPressSendsPressAndRelease() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            DialControl(
                enabled = true,
                modifier = Modifier.size(200.dp),
                onKey = { key, action, _ -> events += "$key:$action" },
            )
        }

        composeRule.onNodeWithContentDescription("Dial").performTouchInput {
            down(center)
            advanceEventTime(120)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(listOf("ENC:1", "ENC:0"), events)
        }
    }

    @Test
    fun centerDragPastTouchSlopReleasesPressAndRotates() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            DialControl(
                enabled = true,
                modifier = Modifier.size(200.dp),
                onKey = { key, action, _ -> events += "$key:$action" },
            )
        }

        composeRule.onNodeWithContentDescription("Dial").performTouchInput {
            down(center)
            moveTo(Offset(width * 0.75f, height / 2f), 50)
            moveTo(Offset(width / 2f, height * 0.75f), 50)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(listOf("ENC:1", "ENC:0"), events.take(2))
            assertTrue(events.drop(2).isNotEmpty())
            assertTrue(events.drop(2).all { it == "ENC_CC:2" })
        }
    }

    @Test
    fun clockwiseOuterDragSendsRepeatedCounterClockwiseProtocolSteps() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            DialControl(
                enabled = true,
                modifier = Modifier.size(200.dp),
                onKey = { key, action, _ -> events += "$key:$action" },
            )
        }

        composeRule.onNodeWithContentDescription("Dial").performTouchInput {
            down(Offset(width * 0.9f, height / 2f))
            moveTo(Offset(width / 2f, height * 0.9f), 200)
            up()
        }

        composeRule.runOnIdle {
            assertTrue(events.size >= 5)
            assertTrue(events.all { it == "ENC_CC:2" })
        }
    }
}
