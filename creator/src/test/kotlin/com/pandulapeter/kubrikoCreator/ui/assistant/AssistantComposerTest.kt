/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.agentclientprotocol.model.AvailableCommand
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AssistantComposerTest {

    @Test
    fun whileTheAgentWaitsForAnAnswerTheComposerSendsItAndCanStillStopTheTurn() = uiTest {
        val sent = mutableListOf<String>()
        var stops = 0
        showing {
            AssistantComposer(
                commands = emptyList(),
                isEnabled = true,
                isBusy = true,
                isAnswering = true,
                onSend = { sent += it; true },
                onStop = { stops++ },
            )
        }
        onNodeWithText("Answer the question above", substring = true).assertExists()
        onNode(hasSetTextAction()).performTextInput("Rides a dragon")
        saveSnapshot("composer-answering")
        onNodeWithContentDescription("Answer").performClick()
        onNodeWithContentDescription("Stop").performClick()
        assertEquals(listOf("Rides a dragon"), sent)
        assertEquals(1, stops)
    }

    @Test
    fun whileTheAgentWorksTheComposerOnlyStopsTheTurn() = uiTest {
        val sent = mutableListOf<String>()
        showing {
            AssistantComposer(
                commands = emptyList(),
                isEnabled = true,
                isBusy = true,
                onSend = { sent += it; true },
                onStop = {},
            )
        }
        onNode(hasSetTextAction()).performTextInput("Wait")
        onNodeWithContentDescription("Send").assertDoesNotExist()
        onNodeWithContentDescription("Stop").assertExists()
        assertEquals(emptyList(), sent)
    }

    @Test
    fun textNotTakenStaysAndNoSlashCommandIsOfferedAsAnAnswer() = uiTest {
        showing {
            AssistantComposer(
                commands = listOf(AvailableCommand("compact", "Compacts the conversation")),
                isEnabled = true,
                isBusy = true,
                isAnswering = true,
                onSend = { false },
                onStop = {},
            )
        }
        onNode(hasSetTextAction()).performTextInput("/comp")
        onNodeWithText("/compact").assertDoesNotExist()
        onNodeWithContentDescription("Answer").performClick()
        onNodeWithText("/comp").assertExists()
    }
}
