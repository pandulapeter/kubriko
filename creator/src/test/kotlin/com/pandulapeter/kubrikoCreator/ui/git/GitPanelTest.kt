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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.DescriptionWriter
import com.pandulapeter.kubrikoCreator.ui.LocalDescriptionWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitPanelTest {
    private val state = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf("Ball.kt"), isBlockType = true)), emptyList()))

    @Test
    fun aCommitThatFailsKeepsTheMessage() = uiTest {
        val commits = mutableListOf<String>()
        showing { GitPanel(state = state, onStartVersionControl = {}, onCommit = { message, _ -> commits += message }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit").performClick()
        waitForIdle()
        assertEquals(listOf("Make the Ball faster"), commits)
        onNodeWithText("Make the Ball faster").assertExists()
    }

    @Test
    fun aCommitThatWorksClearsTheMessage() = uiTest {
        showing { GitPanel(state = state, onStartVersionControl = {}, onCommit = { _, onCommitted -> onCommitted() }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit").performClick()
        waitForIdle()
        onNodeWithText("Make the Ball faster").assertDoesNotExist()
        onNodeWithText("What did you change?").assertExists()
    }

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
        onNodeWithText("Commit").performClick()
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
