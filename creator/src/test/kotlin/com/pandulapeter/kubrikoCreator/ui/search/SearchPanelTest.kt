/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchPanelTest {
    private val brick = RelationSubject.Type("Brick")
    private val collision = Usage(UsagePlace.InModel(RelationPlace.Type("Ball", 2)), RelationKind.COLLIDES_WITH, brick, UsageEffect.EVENT_REMOVED)
    private val mention = Usage(UsagePlace.Markdown("screenplay/story.md"), RelationKind.MENTIONS, brick, UsageEffect.MENTION_TO_TEXT, count = 2)

    @Test
    fun theUsagesAreListedWithWhereTheyAreAndAClickGoesThere() = uiTest(width = 360, height = 400) {
        val selected = mutableListOf<Usage>()
        showing {
            SearchPanel(usagesSubject = brick, usages = rememberUsages(brick, revision = 0) { listOf(collision, mention) }, onUsageSelected = { selected += it })
        }
        onNodeWithText("Usages of Brick").assertExists()
        onNodeWithText("Ball, Event 3").assertExists()
        onNodeWithText("Page story").assertExists()
        saveSnapshot("search-usages")
        onNodeWithText("Ball, Event 3").performClick()
        assertEquals(listOf(collision), selected)
    }

    @Test
    fun nothingReferringAndAFailedReadAreSaid() = uiTest(width = 360, height = 300) {
        var subject by mutableStateOf<RelationSubject>(brick)
        showing {
            SearchPanel(
                usagesSubject = subject,
                usages = rememberUsages(subject, revision = 0) { asked -> if (asked == brick) emptyList() else error("The disk is gone.") },
                onUsageSelected = {},
            )
        }
        onNodeWithText("Nothing refers to Brick.").assertExists()
        subject = RelationSubject.Scene("level")
        onNodeWithText("The usages couldn't be read: The disk is gone.").assertExists()
    }

    @Test
    fun anotherSubjectsUsagesAreNeverShownWhileItsOwnAreRead() = uiTest(width = 360, height = 300) {
        var subject by mutableStateOf<RelationSubject>(brick)
        val paddleUsages = CompletableDeferred<List<Usage>>()
        showing {
            SearchPanel(
                usagesSubject = subject,
                usages = rememberUsages(subject, revision = 0) { asked -> if (asked == brick) listOf(collision) else paddleUsages.await() },
                onUsageSelected = {},
            )
        }
        onNodeWithText("Ball, Event 3").assertExists()
        subject = RelationSubject.Type("Paddle")
        onNodeWithText("Looking for what refers to Paddle…").assertExists()
        onNodeWithText("Ball, Event 3").assertDoesNotExist()
        paddleUsages.complete(emptyList())
        onNodeWithText("Nothing refers to Paddle.").assertExists()
    }

    @Test
    fun theUsagesAreReadAgainWhenTheProjectChanges() = uiTest(width = 360, height = 300) {
        var revision by mutableStateOf(0)
        var reads = 0
        showing {
            SearchPanel(usagesSubject = brick, usages = rememberUsages(brick, revision) { reads++; if (reads == 1) emptyList() else listOf(collision) }, onUsageSelected = {})
        }
        onNodeWithText("Nothing refers to Brick.").assertExists()
        revision++
        onNodeWithText("Ball, Event 3").assertExists()
        assertEquals(2, reads)
    }

    @Test
    fun aSubjectRenamedOrDeletedSinceIsSaidToBeGone() = uiTest(width = 360, height = 300) {
        showing { SearchPanel(usagesSubject = brick, usages = rememberUsages(brick, revision = 0) { null }, onUsageSelected = {}) }
        onNodeWithText("Brick is no longer in the project: it was renamed or deleted.").assertExists()
    }
}
