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
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TurnReviewCardTest {
    @Test
    fun keepAndUndoAreOffered() = uiTest {
        val choices = mutableListOf<String>()
        showing { TurnReviewCard(review = TranscriptItem.TurnReview(3), onKeep = { choices += "keep" }, onUndo = { choices += "undo" }) }
        saveSnapshot("turn-review-offered")
        onNodeWithText("Undo").performClick()
        onNodeWithText("Keep").performClick()
        assertEquals(listOf("undo", "keep"), choices)
    }

    @Test
    fun aChoiceMadeIsSaid() = uiTest {
        showing { TurnReviewCard(review = TranscriptItem.TurnReview(3, TranscriptItem.TurnReview.State.PARTLY_UNDONE), onKeep = {}, onUndo = {}) }
        onNodeWithText("Undone up to a later change of yours", substring = true).assertExists()
        onNodeWithText("Keep").assertDoesNotExist()
        saveSnapshot("turn-review-partly-undone")
    }
}
