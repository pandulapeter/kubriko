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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.SearchHit
import com.pandulapeter.kubrikoCreator.context.SearchResults
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

    @Test
    fun theFieldSearchesTheTextsListingEachLineFoundUnderWhereItIs() = uiTest(width = 360, height = 400) {
        val hit = SearchHit(UsagePlace.Markdown("screenplay/story.md"), SearchHit.Kind.MARKDOWN, "The ball breaks every wall.", listOf(22..25), line = 3)
        val logHit = SearchHit(UsagePlace.InModel(RelationPlace.Type("Ball", 1, 0)), SearchHit.Kind.BLOCK_TEXT, "Hit a wall", listOf(6..9))
        var query by mutableStateOf("")
        val selected = mutableListOf<SearchHit>()
        showing {
            SearchPanel(
                usagesSubject = null,
                usages = UsagesState.Reading(null),
                onUsageSelected = {},
                query = query,
                onQueryChange = { query = it },
                texts = rememberTextSearch(query, revision = 0) { SearchResults(listOf(logHit, hit), total = 5) },
                onHitSelected = { selected += it },
            )
        }
        onNodeWithText("Type to find text in the project", substring = true).assertExists()
        onNode(hasSetTextAction()).performTextInput("wall")
        awaitText("Page story, line 3")
        onNodeWithText("Ball, Event 2, Block 1").assertExists()
        onNodeWithText("And 3 more: search for something longer to see them.").assertExists()
        val line = onNodeWithText("The ball breaks every wall.").fetchSemanticsNode().config[SemanticsProperties.Text].single { it.text == "The ball breaks every wall." }
        assertEquals(listOf(22 to 26), line.spanStyles.map { it.start to it.end })
        saveSnapshot("search-texts")
        onNodeWithText("The ball breaks every wall.").performClick()
        assertEquals(listOf(hit), selected)
        onNode(hasSetTextAction()).requestFocus()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        assertEquals("", query)
    }

    @Test
    fun nothingFoundIsSaidAndAnotherQuerysLinesAreNeverShown() = uiTest(width = 360, height = 300) {
        var query by mutableStateOf("wall")
        val slow = CompletableDeferred<SearchResults>()
        val wallHit = SearchHit(UsagePlace.Markdown("a.md"), SearchHit.Kind.MARKDOWN, "a wall", listOf(2..5), line = 1)
        showing {
            SearchPanel(
                usagesSubject = null,
                usages = UsagesState.Reading(null),
                onUsageSelected = {},
                query = query,
                texts = rememberTextSearch(query, revision = 0) { asked -> if (asked == "wall") SearchResults(listOf(wallHit), 1) else slow.await() },
            )
        }
        awaitText("a wall")
        query = "zebra"
        onNodeWithText("Searching for zebra…").assertExists()
        onNodeWithText("a wall").assertDoesNotExist()
        slow.complete(SearchResults(emptyList(), 0))
        awaitText("Nothing contains zebra.")
    }

    @Test
    fun theUsagesLeadBackToTheSearch() = uiTest(width = 360, height = 300) {
        var backs = 0
        showing { SearchPanel(usagesSubject = brick, usages = rememberUsages(brick, revision = 0) { emptyList() }, onUsageSelected = {}, onBackToSearch = { backs++ }) }
        onNodeWithContentDescription("Back to the search").performClick()
        assertEquals(1, backs)
    }

    private fun ComposeUiTest.awaitText(text: String) = waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
}
