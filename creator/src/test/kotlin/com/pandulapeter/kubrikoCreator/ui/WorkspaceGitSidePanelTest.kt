/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WorkspaceGitSidePanelTest {
    private val state = MutableStateFlow<GitState>(GitState.Ready("main", listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf("Ball.kt"), isBlockType = true)), emptyList()))

    @Test
    fun aCommitIsDispatchedAsACommandThatClearsTheMessageOnceItWorks() = uiTest {
        val invocations = mutableListOf<Invocation<*>>()
        val width = PanelWidthState(Panel.VERSION_CONTROL, PanelLayout(emptyMap(), { _, _ -> }, {}))
        showing {
            Row {
                WorkspaceGitSidePanel(
                    gitState = state,
                    onCommand = { invocation, onSuccess ->
                        invocations += invocation
                        onSuccess()
                    },
                    width = width,
                    row = PanelRow(1200.dp, panelsInRow = 2),
                )
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("Make the Ball faster")
        onNodeWithText("Commit").performClick()
        waitForIdle()
        assertEquals(listOf<Invocation<*>>(Invocation(Commit, Commit.Input("Make the Ball faster"))), invocations)
        onNodeWithText("What did you change?").assertExists()
    }
}
