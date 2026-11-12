/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.foundation.TextSnippet
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationSearchTest {
    private val newer = SavedConversation("/game", "claude-acp", "b", "Faster ball", 2)
    private val older = SavedConversation("/game", "other-acp", "a", "Bricks", 1)
    private val conversations = listOf(
        newer to listOf(KeptMessage(true, "Make the BALL faster"), KeptMessage(false, "Done.\nThe ball now moves at 400."), KeptMessage(true, "Thanks")),
        older to listOf(KeptMessage(true, "Add gold bricks"), KeptMessage(false, "The ball breaks them in two hits.")),
    )

    @Test
    fun everyMessageHoldingTheQueryIsFoundNewestConversationFirstWithTheWordsAround() = assertEquals(
        listOf(
            ConversationMatch(newer, "ball", 0, 1, isFromUser = true, isHeldByAnotherAgent = false, snippet = TextSnippet("Make the BALL faster", listOf(9..12))),
            ConversationMatch(newer, "ball", 1, 0, isFromUser = false, isHeldByAnotherAgent = false, snippet = TextSnippet("The ball now moves at 400.", listOf(4..7))),
            ConversationMatch(older, "ball", 1, 0, isFromUser = false, isHeldByAnotherAgent = true, snippet = TextSnippet("The ball breaks them in two hits.", listOf(4..7))),
        ),
        ConversationSearch.search(" ball ", conversations, agentId = "claude-acp", limit = 10),
    )

    @Test
    fun theMatchesStopAtTheLimitAndABlankQueryFindsNothing() {
        assertEquals(2, ConversationSearch.search("ball", conversations, agentId = "claude-acp", limit = 2).size)
        assertEquals(emptyList(), ConversationSearch.search("  ", conversations, agentId = "claude-acp", limit = 10))
    }
}
