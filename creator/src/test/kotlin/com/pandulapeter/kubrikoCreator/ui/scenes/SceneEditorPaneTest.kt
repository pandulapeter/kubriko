/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActors
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.SidebarDragState
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SceneEditorPaneTest {
    private val ball = SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "ball")
    private val restoreBall = Invocation(RestoreActor, RestoreActor.Input("level", 0, ball.typeId, ball.state, ball.id), isRestoring = true)

    @Test
    fun undoingAnActorsRemovalSelectsItAgain() = uiTest(width = 900, height = 600) {
        val steps = MutableSharedFlow<Invocation<*>>(extraBufferCapacity = 4)
        val commands = mutableListOf<Invocation<*>>()
        showing {
            SceneEditorPane(
                state = SceneEditingState.Open(
                    scene = ProjectResource(ResourceGroup.SCENES, File("/game/src/commonMain/composeResources/files/scenes/level.json"), "src/commonMain/composeResources/files/scenes/level.json"),
                    entryPoint = PlainEntryPoint,
                    actors = listOf(ball),
                    revision = 1,
                ),
                onCommand = { commands += it },
                actorTypeDragState = SidebarDragState(),
                panelLayout = PanelLayout(emptyMap(), onSizeChange = { _, _ -> }, onSizeReset = {}),
                actorTypes = emptyList(),
                music = emptyList(),
                description = null,
                historySteps = steps,
                onPlay = {},
                onReload = {},
                onClose = {},
                onViewFailed = { _, _ -> },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        steps.tryEmit(restoreBall.copy(input = restoreBall.input.copy(scene = "other")))
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        steps.tryEmit(restoreBall)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription(REMOVE).performClick()
        assertEquals(listOf<Invocation<*>>(Invocation(RemoveActor, RemoveActor.Input("level", "ball"))), commands)
        onNodeWithText(INSPECTOR_HINT).assertExists()
        steps.tryEmit(restoreBall)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(INSPECTOR_HINT).assertDoesNotExist()
        saveSnapshot("scene-editor-removal-undone")
    }

    @Test
    fun onlyAStepBringingOneActorBackIntoThisSceneNamesIt() {
        assertEquals("ball", actorBroughtBackBy(restoreBall, "level"))
        assertNull(actorBroughtBackBy(restoreBall, "other"))
        val one = RestoreActors.Restored(0, ball.typeId, ball.state, "ball")
        assertEquals("ball", actorBroughtBackBy(Invocation(RestoreActors, RestoreActors.Input("level", listOf(one))), "level"))
        assertNull(actorBroughtBackBy(Invocation(RestoreActors, RestoreActors.Input("level", listOf(one, one.copy(actorId = "wall")))), "level"))
        assertEquals("ball", actorBroughtBackBy(Invocation(PlaceActor, PlaceActor.Input("level", "Ball", 1f, 2f, "ball")), "level"))
        assertNull(actorBroughtBackBy(Invocation(RemoveActor, RemoveActor.Input("level", "ball")), "level"))
        assertNull(actorBroughtBackBy(Invocation(SetActorValues, SetActorValues.Input("level", "ball")), "level"))
    }

    private object PlainEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()
    }

    private companion object {
        const val INSPECTOR_HINT = "Drag an Actor type from the sidebar into the Scene to place it there, and click an Actor to change it. Drag it to move it, or press Delete to remove it."
        const val REMOVE = "Remove from the Scene"
    }
}
