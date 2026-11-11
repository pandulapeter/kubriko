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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastConversationTest {
    private val last = SavedConversation("/projects/breakout", "claude-acp", "1", "Make the Ball faster", 1, lastAnswer = "Done: the Ball moves at 360.")

    @Test
    fun aNewConversationHearsHowThePreviousOneEndedAfterTheProject() = assertEquals(
        "Digest\n\nThe previous conversation about this project, \"Make the Ball faster\", ended with this answer:\nDone: the Ball moves at 360.",
        LastConversation.withProject("Digest", last),
    )

    @Test
    fun whatThePreviousOneNotedIsToldInPlaceOfItsAnswer() = assertEquals(
        "Digest\n\nWhat the previous conversation about this project, \"Make the Ball faster\", noted for this one:\nThe Ball is faster; the Paddle is still slow.",
        LastConversation.withProject("Digest", last.copy(summary = "The Ball is faster; the Paddle is still slow.")),
    )

    @Test
    fun anEarlierSummaryIsToldWithTheAnswerThatCameAfterIt() = assertEquals(
        "What the previous conversation about this project, \"Make the Ball faster\", noted for this one:\nThe Ball is faster; the Paddle is still slow.\n" +
            "After that, it ended with this answer:\nDone: the Ball moves at 360.",
        LastConversation.withProject(null, last.copy(summary = "The Ball is faster; the Paddle is still slow.", isSummaryEarlier = true)),
    )

    @Test
    fun withoutAnAnswerOnlyTheProjectIsTold() {
        assertEquals("Digest", LastConversation.withProject("Digest", last.copy(lastAnswer = null)))
        assertNull(LastConversation.withProject(null, null))
    }
}
