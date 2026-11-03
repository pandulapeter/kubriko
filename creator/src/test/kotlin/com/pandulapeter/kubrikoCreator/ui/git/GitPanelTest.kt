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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GitPanelTest {
    private val state = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf("Ball.kt"), isBlockType = true)), emptyList()))

    @Test
    fun aCommitThatFailsKeepsTheMessage() = uiTest {
        val commits = mutableListOf<String>()
        showing { GitPanel(state = state, onRefresh = {}, onStartVersionControl = {}, onCommit = { message, _ -> commits += message }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit").performClick()
        waitForIdle()
        assertEquals(listOf("Make the Ball faster"), commits)
        onNodeWithText("Make the Ball faster").assertExists()
    }

    @Test
    fun aCommitThatWorksClearsTheMessage() = uiTest {
        showing { GitPanel(state = state, onRefresh = {}, onStartVersionControl = {}, onCommit = { _, onCommitted -> onCommitted() }, onDiscard = {}) }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit").performClick()
        waitForIdle()
        onNodeWithText("Make the Ball faster").assertDoesNotExist()
        onNodeWithText("What did you change?").assertExists()
    }
}
