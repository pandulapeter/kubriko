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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WorkspaceToolbarTest {

    @Test
    fun theButtonsAtTheEndStayInPlaceWhileTheGameRuns() = uiTest(width = 1200, height = 80) {
        var sessionState by mutableStateOf<SessionState>(SessionState.Idle)
        showing {
            WorkspaceToolbar(
                sessionState = sessionState,
                history = MutableStateFlow(CommandHistory()),
                onUndo = {},
                onRedo = {},
                gitState = MutableStateFlow(GitState.NoRepository),
                isAiConnected = false,
                openPanel = null,
                onPanelToggled = {},
                playTargets = PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP),
                onPlayTargetsOpened = {},
                onPlayTargetChosen = {},
                onPlay = {},
                onStop = {},
                onOpenProjectSettings = {},
                onOpenTargets = {},
                onSetUpMoreDevices = {},
                onOpenSettings = {},
                onCloseProject = {},
            )
        }
        val idle = onNodeWithContentDescription("Close project").fetchSemanticsNode().boundsInRoot
        sessionState = SessionState.Running(SessionMode.PLAY)
        waitForIdle()
        saveSnapshot("workspace-toolbar-playing")
        assertEquals(idle, onNodeWithContentDescription("Close project").fetchSemanticsNode().boundsInRoot)
    }
}
