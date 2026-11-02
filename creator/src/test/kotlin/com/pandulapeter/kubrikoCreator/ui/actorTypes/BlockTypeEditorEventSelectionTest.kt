/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.ui.saveSnapshot
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BlockTypeEditorEventSelectionTest {
    private val added = EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("added-blocks")))
    private val update = EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("update-blocks")))
    private val keyPressed = EventDefinition(EventType.KEY_PRESSED, KeyChoice.A, blocks = listOf(BlockDefinition.Log("key-blocks")))
    private var definition by mutableStateOf(ActorTypeDefinition(name = "Ball", events = listOf(added, update)))

    private fun showingEditor(onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit, checks: ComposeUiTest.() -> Unit) = uiTest(width = 1200, height = 700) {
        showing {
            BlockTypeEditor(
                definition = definition,
                scope = BlockScope.of(definition, listOf(definition)),
                sprites = emptyList(),
                inheritance = Inheritance(),
                isChangedByHand = false,
                onCommand = onCommand,
                onRename = { null },
                onTypeSelected = {},
            )
        }
        checks()
    }

    private fun ComposeUiTest.selectEveryFrame() {
        onAllNodesWithText("Every frame").onFirst().performClick()
        waitForIdle()
        onNodeWithText("update-blocks").assertExists()
    }

    private fun ComposeUiTest.addAnEvent() {
        onNodeWithContentDescription("Add Event").performClick()
        waitForIdle()
        onNodeWithText("When a key is pressed").performClick()
        waitForIdle()
    }

    @Test
    fun anEventInsertedBeforeTheSelectedOneLeavesTheSelectionOnIt() = showingEditor(onCommand = { _, _ -> }) {
        selectEveryFrame()
        definition = definition.copy(events = listOf(keyPressed, added, update))
        waitForIdle()
        onNodeWithText("update-blocks").assertExists()
        saveSnapshot("event-selection-after-insert")
    }

    @Test
    fun aRejectedAddSelectsNothingNew() = showingEditor(onCommand = { _, _ -> }) {
        selectEveryFrame()
        addAnEvent()
        onNodeWithText("update-blocks").assertExists()
    }

    @Test
    fun aSuccessfulAddSelectsTheNewEvent() = showingEditor(
        onCommand = { _, onSuccess ->
            definition = definition.copy(events = definition.events + keyPressed)
            onSuccess()
        },
    ) {
        selectEveryFrame()
        addAnEvent()
        onNodeWithText("key-blocks").assertExists()
    }

    @Test
    fun anAddSucceedingAfterTheNewEventIsShownSelectsIt() {
        var success: (() -> Unit)? = null
        showingEditor(
            onCommand = { _, onSuccess ->
                definition = definition.copy(events = definition.events + keyPressed)
                success = onSuccess
            },
        ) {
            selectEveryFrame()
            addAnEvent()
            onNodeWithText("update-blocks").assertExists()
            runOnIdle { success!!.invoke() }
            waitForIdle()
            onNodeWithText("key-blocks").assertExists()
        }
    }
}
