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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BlockTypeEditorBlockDropTest {
    private val definition = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("added-blocks"))),
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop)),
            EventDefinition(EventType.COLLISION, otherActorType = "Ball", blocks = listOf(BlockDefinition.RemoveOther)),
        ),
    )
    private val dispatched = mutableListOf<Invocation<*>>()

    private fun showingEditor(checks: ComposeUiTest.() -> Unit) = uiTest(width = 1200, height = 700) {
        showing {
            BlockTypeEditor(
                definition = definition,
                scope = BlockScope.of(definition, listOf(definition)),
                sprites = SpriteChoices(),
                inheritance = Inheritance(),
                isChangedByHand = false,
                onCommand = { invocation, _ -> dispatched += invocation },
                currentDefinition = { definition },
                onCommandIfValid = { null },
                onTypeSelected = {},
            )
        }
        checks()
    }

    /**
     * Holds the Block's handle over the row of [event], with the clock advanced by hand, since the Block list asks for
     * every frame while a Block is held near its edges, so it is never idle.
     */
    private fun ComposeUiTest.holdBlockOver(event: String) {
        val from = onNodeWithContentDescription(DRAG_HANDLE).fetchSemanticsNode().boundsInRoot
        val to = onAllNodesWithText(event).onFirst().fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onNodeWithContentDescription(DRAG_HANDLE).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, 30f))
            moveTo(to - from.topLeft)
        }
        repeat(2) { mainClock.advanceTimeByFrame() }
    }

    private fun ComposeUiTest.release() {
        onNodeWithContentDescription(DRAG_HANDLE).performMouseInput { release() }
        mainClock.autoAdvance = true
        waitForIdle()
    }

    @Test
    fun aBlockDraggedOntoAnotherEventMovesToItsEnd() = showingEditor {
        holdBlockOver("Every frame")
        saveSnapshot("block-dragged-over-another-event")
        release()
        assertEquals<List<Invocation<*>>>(listOf(Invocation(MoveBlock, MoveBlock.Input("Ball", 0, 0, 1, targetEventIndex = 1))), dispatched)
    }

    @Test
    fun aBlockDroppedOnItsOwnEventMovesNowhere() = showingEditor {
        holdBlockOver("When added to the Scene")
        release()
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun aBlockDroppedOnAnEventThatCantTakeItMovesNowhereAndThatEventIsFadedMeanwhile() = showingEditor {
        onNodeWithText("When it collides with").performClick()
        waitForIdle()
        holdBlockOver("Every frame")
        saveSnapshot("block-dragged-over-an-event-that-cant-take-it")
        release()
        assertTrue(dispatched.isEmpty())
    }

    private companion object {
        const val DRAG_HANDLE = "Drag to move"
    }
}
