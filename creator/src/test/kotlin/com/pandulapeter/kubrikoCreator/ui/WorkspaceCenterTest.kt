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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneProblem
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WorkspaceCenterTest {
    private val scene = ProjectResource(
        group = ResourceGroup.SCENES,
        file = File("/game/src/commonMain/composeResources/files/scenes/level.json"),
        relativePath = "src/commonMain/composeResources/files/scenes/level.json",
    )
    private var reloads = 0

    private fun showingCenter(selection: WorkspaceSelection, sceneEditing: SceneEditingState, checks: ComposeUiTest.() -> Unit) = uiTest(width = 900, height = 600) {
        showing {
            Row {
                WorkspaceCenter(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selection = selection,
                    sceneEditing = sceneEditing,
                    blockTypes = emptyList(),
                    resources = emptyMap(),
                    sceneNames = emptyList(),
                    sceneDescriptions = emptyMap(),
                    changedByHand = emptySet(),
                    shownAiStep = null,
                    actorTypeDragState = SidebarDragState(),
                    panelLayout = PanelLayout(emptyMap(), onSizeChange = { _, _ -> }, onSizeReset = {}),
                    onCommand = { _, _ -> },
                    currentBlockType = { null },
                    onCommandIfValid = { null },
                    onTypeSelected = {},
                    onPlayScene = {},
                    onReloadScene = { reloads++ },
                    onCloseScene = {},
                    onSceneViewFailed = { _, _, _ -> },
                )
            }
        }
        checks()
    }

    @Test
    fun nothingSelectedShowsTheWorkspaceHint() = showingCenter(WorkspaceSelection.None, SceneEditingState.Closed) {
        onNodeWithText("Lights, camera, action!").assertExists()
        saveSnapshot("workspace-center-hint")
    }

    @Test
    fun aFailedSceneShowsItsReasonAndTryAgain() = showingCenter(WorkspaceSelection.Scene("level"), SceneEditingState.Failed(scene, SceneProblem.ENTRY_POINT_MISSING)) {
        onNodeWithText("The project has no CreatorEntryPoint, so its Scenes can't be edited.").assertExists()
        onNodeWithText("Try again").performClick()
        waitForIdle()
        assertEquals(1, reloads)
        saveSnapshot("workspace-center-scene-failed")
    }

    @Test
    fun aPreparingSceneShowsProgress() = showingCenter(WorkspaceSelection.Scene("level"), SceneEditingState.Preparing(scene)) {
        onNodeWithText("Preparing level", substring = true).assertExists()
    }
}
