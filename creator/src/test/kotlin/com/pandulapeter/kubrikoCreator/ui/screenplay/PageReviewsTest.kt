/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantTranscript
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.HistoryEntry
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageReviewsTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val chat = AssistantChat(parentScope = scope, start = { _, _ -> awaitCancellation() }, end = {})

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun aTurnIsOfferedWhileTheConversationsLastReviewIsAndNoLaterTurnRuns() {
        val transcript = AssistantTranscript(listOf(TranscriptItem.TurnReview(3, TranscriptItem.TurnReview.State.KEPT), TranscriptItem.AgentMessage("Done"), TranscriptItem.TurnReview(7)))
        val last = lastReviewOf(transcript)
        assertEquals(OfferedTurn(chat, "tools-1", 7), offeredTurnOf(chat, last, "tools-1", isBusy = false))
        assertNull(offeredTurnOf(chat, last, "tools-1", isBusy = true))
        assertNull(offeredTurnOf(chat, last, conversationId = null, isBusy = false))
        assertNull(offeredTurnOf(chat, TranscriptItem.TurnReview(7, TranscriptItem.TurnReview.State.UNDONE), "tools-1", isBusy = false))
        assertNull(lastReviewOf(AssistantTranscript(listOf(TranscriptItem.AgentMessage("Hi")))))
    }

    @Test
    fun aPageIsReviewedByTheOfferedTurnThatChangedItAndOnlyWhileReviewsAreShown() {
        val edit = Invocation(EditPage, EditPage.Input("hero", "# The hero\n\nA knight who rolls.\n"))
        val history = CommandHistory(undoable = listOf(HistoryEntry(revert = Invocation(EditPage, EditPage.Input("hero", "# The hero\n")), reapply = edit, conversationId = "tools-1", revision = 8)))
        val offered = listOf(OfferedTurn(chat, "tools-2", 7), OfferedTurn(chat, "tools-1", 7))
        assertEquals("# The hero\n", pageReviewOf("hero", offered, history, isShown = true)?.before)
        assertNull(pageReviewOf("villain", offered, history, isShown = true))
        assertNull(pageReviewOf("hero", offered, history, isShown = false))
        assertNull(pageReviewOf("hero", listOf(OfferedTurn(chat, "tools-1", 8)), history, isShown = true))
    }
}
