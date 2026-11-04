/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.mcp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AiActivityBarTest {
    private fun step(clientName: String?) = AiStep(0, "add_block", null, isRunning = false, clientName = clientName)

    @Test
    fun clientsAreNamedOnlyWhileStepsComeFromMoreThanOne() {
        assertFalse(showsClientNames(listOf(step("claude-code"), step("claude-code"), step(null))))
        assertTrue(showsClientNames(listOf(step("claude-code"), step("cursor"))))
    }

    @Test
    fun followingIsASwitchFillingTheBarsEnd() = uiTest(width = 800, height = 120) {
        val changes = mutableListOf<Boolean>()
        showing {
            AiActivityBar(
                steps = listOf(AiStep(0, "add_block", null, isRunning = true, changesProject = true)),
                isFollowing = true,
                onFollowingChange = { changes += it },
                onClick = {},
            )
        }
        onNodeWithText("Follow").assertIsOn()
        val switchBounds = onNodeWithText("Follow").getUnclippedBoundsInRoot()
        val barBounds = onNodeWithText("Step 1").getUnclippedBoundsInRoot()
        assertEquals(barBounds.top, switchBounds.top)
        assertEquals(barBounds.bottom, switchBounds.bottom)
        assertEquals(barBounds.right, switchBounds.right)
        saveSnapshot("ai-activity-bar")
        onNodeWithText("Follow").performClick()
        assertEquals(listOf(false), changes)
    }
}
