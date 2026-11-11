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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.GameModules
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WorkspaceToolbarTest {

    @Test
    fun theButtonsAtTheEndStayInPlaceWhileTheGameRuns() = uiTest(width = 1200, height = 80) {
        var sessionState by mutableStateOf<SessionState>(SessionState.Idle)
        showing { Toolbar(sessionState) }
        val idle = onNodeWithContentDescription("Close project").fetchSemanticsNode().boundsInRoot
        sessionState = SessionState.Running(SessionMode.PLAY)
        waitForIdle()
        saveSnapshot("workspace-toolbar-playing")
        assertEquals(idle, onNodeWithContentDescription("Close project").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun theGameModuleIsOfferedOnlyWhileThereIsAChoiceAndNothingRuns() = uiTest(width = 1200, height = 80) {
        var sessionState by mutableStateOf<SessionState>(SessionState.Idle)
        var gameModules by mutableStateOf<GameModules?>(null)
        showing { Toolbar(sessionState, gameModules) }
        gameModules = GameModules(listOf(":"), ":")
        waitForIdle()
        onNodeWithText("Root module").assertDoesNotExist()
        gameModules = GameModules(listOf(":app:desktop", ":editor:model"), ":app:desktop")
        waitForIdle()
        val play = onNode(hasText("Play") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val module = onNodeWithText("app:desktop").fetchSemanticsNode().boundsInRoot
        val target = onNodeWithText("Desktop").fetchSemanticsNode().boundsInRoot
        assertTrue(play.right <= module.left && module.right <= target.left, "Play, then the module, then the target: $play, $module, $target")
        saveSnapshot("workspace-toolbar-game-module")
        sessionState = SessionState.Running(SessionMode.PLAY)
        waitForIdle()
        onNodeWithText("app:desktop").assertDoesNotExist()
    }

    @Test
    fun playWaitsForTheBuildThatRuns() = uiTest(width = 1200, height = 80) {
        var isBuilding by mutableStateOf(true)
        var plays = 0
        showing { Toolbar(SessionState.Idle, isBuilding = isBuilding, onPlay = { plays++ }) }
        onNode(hasText("Play") and hasClickAction()).assertIsNotEnabled()
        saveSnapshot("workspace-toolbar-building")
        isBuilding = false
        waitForIdle()
        onNode(hasText("Play") and hasClickAction()).assertIsEnabled().performClick()
        assertEquals(1, plays)
    }

    @Test
    fun undoIsOfferedForAnEditorsOwnStepsWhenTheWorkspaceHasNone() = uiTest(width = 1200, height = 80) {
        val editor = object : EditorHistory {
            override var canUndo by mutableStateOf(false)
            override val canRedo = false

            override fun undo() = canUndo

            override fun redo() = false
        }
        var undos = 0
        showing { Toolbar(SessionState.Idle, editorHistory = editor, onUndo = { undos++ }) }
        onNodeWithContentDescription("Undo").assertIsNotEnabled()
        editor.canUndo = true
        waitForIdle()
        onNodeWithContentDescription("Undo").assertIsEnabled().performClick()
        assertEquals(1, undos)
    }

    @Composable
    private fun Toolbar(
        sessionState: SessionState,
        gameModules: GameModules? = null,
        isBuilding: Boolean = false,
        onPlay: () -> Unit = {},
        editorHistory: EditorHistory? = null,
        onUndo: () -> Unit = {},
    ) = WorkspaceToolbar(
        sessionState = sessionState,
        isBuilding = isBuilding,
        history = MutableStateFlow(CommandHistory()),
        onUndo = onUndo,
        onRedo = {},
        editorHistory = editorHistory,
        gitState = MutableStateFlow(GitState.NoRepository),
        isAiConnected = false,
        openPanel = null,
        onPanelToggled = {},
        playTargets = PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP),
        onPlayTargetsOpened = {},
        onPlayTargetChosen = {},
        gameModules = gameModules,
        onGameModuleChosen = {},
        onPlay = onPlay,
        onStop = {},
        onOpenProjectSettings = {},
        onOpenTargets = {},
        onSetUpMoreDevices = {},
        onOpenSettings = {},
        onCloseProject = {},
    )
}
