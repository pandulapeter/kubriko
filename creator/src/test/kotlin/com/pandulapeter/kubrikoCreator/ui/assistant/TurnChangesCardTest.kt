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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.ui.saveSnapshot
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TurnChangesCardTest {
    private val changes = TranscriptItem.TurnChanges(
        sinceRevision = 3,
        changes = listOf(
            ChangeRecord(4, "add_block", CommandTarget.BlockType("Ball", 0, 1)),
            ChangeRecord(5, "set_appearance", CommandTarget.BlockType("Paddle")),
        ),
    )

    @Test
    fun whatATurnChangedIsListedWithKeepAndUndo() = uiTest {
        val choices = mutableListOf<String>()
        showing { TurnChangesCard(changes = changes, onKeep = { choices += "keep" }, onUndo = { choices += "undo" }) }
        onNodeWithText("What changed").assertExists()
        onNodeWithText("Paddle", substring = true).assertExists()
        saveSnapshot("turn-changes-offered")
        onNodeWithText("Undo").performClick()
        onNodeWithText("Keep").performClick()
        assertEquals(listOf("undo", "keep"), choices)
    }

    @Test
    fun aChoiceMadeIsSaid() = uiTest {
        showing { TurnChangesCard(changes = changes.copy(state = TranscriptItem.TurnChanges.State.PARTLY_UNDONE), onKeep = {}, onUndo = {}) }
        onNodeWithText("Undone up to a later change of yours", substring = true).assertExists()
        onNodeWithText("Keep").assertDoesNotExist()
        saveSnapshot("turn-changes-partly-undone")
    }
}
