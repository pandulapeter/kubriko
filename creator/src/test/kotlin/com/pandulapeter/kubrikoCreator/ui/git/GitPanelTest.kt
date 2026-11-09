/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.git

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.CommitSummary
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.DescriptionWriter
import com.pandulapeter.kubrikoCreator.ui.LocalDescriptionWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GitPanelTest {
    private val state = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf("Ball.kt"), isBlockType = true)), emptyList()))

    @Test
    fun aCommitThatFailsKeepsTheMessage() = uiTest {
        val commits = mutableListOf<String>()
        showing { GitPanel(state = state, onStartVersionControl = {}, onCommit = { message, _ -> commits += message }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit the change").performClick()
        waitForIdle()
        assertEquals(listOf("Make the Ball faster"), commits)
        onNodeWithText("Make the Ball faster").assertExists()
    }

    @Test
    fun aCommitThatWorksClearsTheMessage() = uiTest {
        showing { GitPanel(state = state, onStartVersionControl = {}, onCommit = { _, onCommitted -> onCommitted() }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit the change").performClick()
        waitForIdle()
        onNodeWithText("Make the Ball faster").assertDoesNotExist()
        onNodeWithText("What did you change?").assertExists()
    }

    @Test
    fun theCommitFormStaysAboveTheChangesWhileABigChangeSetIsScrolled() = uiTest {
        showing { GitPanel(state = MutableStateFlow(manyChanges()), onStartVersionControl = {}, onCommit = { _, _ -> }, onDiscard = {}) }
        val commitButton = onNodeWithText("Commit the 60 changes").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(commitButton.bottom <= onNodeWithText("Changes (60)").assertIsDisplayed().getUnclippedBoundsInRoot().top)
        onNodeWithText("Type1").assertIsDisplayed()
        saveSnapshot("git-many-changes")
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Type60"))
        onNodeWithText("Type60").assertIsDisplayed()
        onNodeWithText("What did you change?").assertIsDisplayed()
        onNodeWithText("Commit the 60 changes").assertIsDisplayed()
        onNodeWithText("Changes (60)").assertIsDisplayed()
        saveSnapshot("git-many-changes-scrolled")
    }

    @Test
    fun aLongMessageScrollsWithinItsFieldAndLeavesTheChangesTheirRoom() = uiTest {
        showing { GitPanel(state = MutableStateFlow(manyChanges()), onStartVersionControl = {}, onCommit = { _, _ -> }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement((1..30).joinToString("\n") { "Line $it of the message" })
        onNodeWithText("Commit the 60 changes").assertIsDisplayed()
        onNodeWithText("Type1").assertIsDisplayed()
        saveSnapshot("git-long-message")
    }

    @Test
    fun inAShortWindowTheFormTakesAtMostHalfOfTheHeightAndScrollsWithinIt() = uiTest(height = 260) {
        val changes = (1..60).map { ChangeEntry("Type$it", ChangeKind.MODIFIED, listOf(".kubriko/actors/Type$it.json"), isBlockType = true) }
        showing { GitPanel(state = MutableStateFlow(GitState.Ready("main", changes, emptyList())), onStartVersionControl = {}, onCommit = { _, _ -> }, onDiscard = {}, areScreenplayRemindersShown = true) }
        saveSnapshot("git-short-window")
        onNodeWithText("Changes (60)").assertIsDisplayed()
        onNodeWithText("Type1").assertIsDisplayed()
        val form = onNode(hasScrollAction() and !hasScrollToNodeAction()).getUnclippedBoundsInRoot()
        val list = onNode(hasScrollToNodeAction()).getUnclippedBoundsInRoot()
        assertTrue(form.bottom - form.top <= (list.bottom - form.top) / 2)
        onNodeWithText("Commit the 60 changes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theRowLookedAtStaysInPlaceWhenAChangeAboveItGoes() = uiTest {
        val state = MutableStateFlow(manyChanges())
        showing { GitPanel(state = state, onStartVersionControl = {}, onCommit = { _, _ -> }, onDiscard = {}) }
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Type40"))
        val before = onNodeWithText("Type40").getUnclippedBoundsInRoot().top
        state.value = state.value.copy(changes = state.value.changes.drop(1))
        waitForIdle()
        assertEquals(before, onNodeWithText("Type40").assertIsDisplayed().getUnclippedBoundsInRoot().top)
    }

    @Test
    fun aChangesRowKeepsItsKeyWhileItsPathsChange() {
        val definition = ChangeEntry("Ball", ChangeKind.MODIFIED, listOf(".kubriko/actors/Ball.json"), isBlockType = true)
        assertEquals(changeKeyOf(definition), changeKeyOf(definition.copy(paths = definition.paths + "src/commonMain/kotlin/generated/Ball.kt")))
        assertNotEquals(changeKeyOf(definition), changeKeyOf(ChangeEntry("Ball", ChangeKind.ADDED, listOf("Ball"), isBlockType = false)))
    }

    @Test
    fun theHistorysHeadingTakesTheCountsPlaceOnceTheHistoryIsScrolledTo() = uiTest {
        val history = (1..30).map { CommitSummary("abc$it", "Commit $it", "Péter", it.toLong()) }
        showing { GitPanel(state = MutableStateFlow(manyChanges().copy(history = history)), onStartVersionControl = {}, onCommit = { _, _ -> }, onDiscard = {}) }
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Commit 30"))
        onNodeWithText("History").assertIsDisplayed()
        onNodeWithText("Changes (60)").assertDoesNotExist()
        saveSnapshot("git-history-scrolled")
    }

    @Test
    fun commitsWithTheSameAbbreviatedIdAreToldApart() {
        assertNotEquals(commitKeyOf(CommitSummary("abc1234", "One", "Péter", 1)), commitKeyOf(CommitSummary("abc1234", "Two", "Péter", 2)))
    }

    private fun manyChanges() = GitState.Ready("main", (1..60).map { ChangeEntry("Type$it", ChangeKind.MODIFIED, listOf("Type$it.kt"), isBlockType = true) }, emptyList())

    @Test
    fun aChangeToTheGameWithoutAPageRemindsAboutTheScreenplayWithoutStandingInTheWay() = uiTest {
        val prompts = mutableListOf<String>()
        val commits = mutableListOf<String>()
        val changed = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf(".kubriko/actors/Ball.json"), isBlockType = true)), emptyList()))
        showing {
            CompositionLocalProvider(LocalDescriptionWriter provides DescriptionWriter(isBusy = false) { prompts += it }) {
                GitPanel(state = changed, onStartVersionControl = {}, onCommit = { message, _ -> commits += message }, onDiscard = {}, areScreenplayRemindersShown = true)
            }
        }
        onNodeWithText("The game changed, but no page of the screenplay did.").assertExists()
        saveSnapshot("git-screenplay-reminder")
        onNodeWithText("Ask the assistant to update the screenplay").performClick()
        assertEquals(1, prompts.size)
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit the change").performClick()
        assertEquals(listOf("Make the Ball faster"), commits)
    }

    @Test
    fun theReminderIsDueOnlyForChangesToTheGameThatLeaveEveryPageAlone() {
        fun changed(vararg paths: String) = listOf(ChangeEntry("x", ChangeKind.MODIFIED, paths.toList(), isBlockType = false))
        assertEquals(true, isScreenplayReminderDue(changed(".kubriko/actors/Ball.json")))
        assertEquals(true, isScreenplayReminderDue(changed("src/commonMain/composeResources/files/scenes/main.json")))
        assertEquals(true, isScreenplayReminderDue(changed("src/commonMain/kotlin/Game.kt")))
        assertEquals(false, isScreenplayReminderDue(changed(".kubriko/actors/Ball.json", "screenplay/README.md")))
        assertEquals(false, isScreenplayReminderDue(changed("build.gradle.kts", "README.md", "gradle.properties")))
        assertEquals(false, isScreenplayReminderDue(changed("gradle/build-logic/src/main/kotlin/Plugin.kt", "src/desktopTest/kotlin/GameTest.kt")))
        assertEquals(true, isScreenplayReminderDue(changed("app/src/desktopMain/kotlin/Main.kt")))
        assertEquals(true, isScreenplayReminderDue(changed("src/commonMain/composeResources/drawable/ball.png")))
    }
}
