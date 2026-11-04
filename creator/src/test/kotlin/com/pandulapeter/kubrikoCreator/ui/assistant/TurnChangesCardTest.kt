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
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kubriko.creator.generated.resources.Res
import kotlin.test.Test

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
    fun whatATurnChangedIsListedWithoutKeepAndUndo() = uiTest {
        showing { TurnChangesCard(changes = changes) }
        onNodeWithText("What changed").assertExists()
        onNodeWithText("Paddle", substring = true).assertExists()
        onNodeWithText("Keep").assertDoesNotExist()
        saveSnapshot("turn-changes")
    }
}
