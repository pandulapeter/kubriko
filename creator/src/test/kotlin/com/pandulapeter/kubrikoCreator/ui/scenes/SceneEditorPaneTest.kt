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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SceneEditorPaneTest {
    private val ball = SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "ball")
    private val ballBack = ActorsBroughtBack("level", listOf(ball))

    @Test
    fun undoingAnActorsRemovalSelectsItAgain() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val commands = mutableListOf<Invocation<*>>()
        showPane(broughtBack, onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ballBack.copy(scene = "other"))
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        broughtBack.tryEmit(ballBack)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription(REMOVE).performClick()
        assertEquals(listOf<Invocation<*>>(Invocation(RemoveActor, RemoveActor.Input("level", "ball"))), commands)
        onNodeWithText(INSPECTOR_HINT).assertExists()
        broughtBack.tryEmit(ballBack)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(INSPECTOR_HINT).assertDoesNotExist()
        saveSnapshot("scene-editor-removal-undone")
    }

    @Test
    fun onlyOneActorBroughtBackIntoThisSceneIsSelected() {
        assertEquals("ball", restoredSelectionOf(ballBack, "level"))
        assertNull(restoredSelectionOf(ballBack, "other"))
        assertNull(restoredSelectionOf(ballBack.copy(actors = listOf(ball, ball.copy(id = "wall"))), "level"))
    }

    @Test
    fun anActorBroughtBackIsSelectedOnlyOnceTheSceneShowsIt() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        var actors by mutableStateOf(emptyList<SceneActorEntry>())
        showPane(broughtBack, actors = { actors })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ballBack)
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        actors = listOf(ball)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theSceneSettingsBroughtBackAreNeverSelected() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val settings = SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, "{}", "settings")
        showPane(broughtBack, actors = { listOf(ball, settings) })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ActorsBroughtBack("level", listOf(settings)))
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        onAllNodes(hasContentDescription(REMOVE)).assertCountEquals(0)
    }

    @Test
    fun anActorBroughtBackWhileTheSceneIsStillPreparedIsSelectedOnceItShows() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val entryPoint = SlowEntryPoint()
        showPane(broughtBack, entryPoint = entryPoint)
        waitUntil(timeoutMillis = 5_000) { broughtBack.subscriptionCount.value > 0 }
        broughtBack.tryEmit(ballBack)
        entryPoint.isReady.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.showPane(
        broughtBack: Flow<ActorsBroughtBack>,
        entryPoint: CreatorEntryPoint = PlainEntryPoint,
        actors: () -> List<SceneActorEntry> = { listOf(ball) },
        onCommand: (Invocation<*>) -> Unit = {},
    ) = showing {
        SceneEditorPane(
            state = SceneEditingState.Open(
                scene = ProjectResource(ResourceGroup.SCENES, File("/game/src/commonMain/composeResources/files/scenes/level.json"), "src/commonMain/composeResources/files/scenes/level.json"),
                entryPoint = entryPoint,
                actors = actors(),
                revision = 1,
            ),
            onCommand = onCommand,
            actorTypeDragState = SidebarDragState(),
            panelLayout = PanelLayout(emptyMap(), onSizeChange = { _, _ -> }, onSizeReset = {}),
            actorTypes = emptyList(),
            music = emptyList(),
            description = null,
            broughtBack = broughtBack,
            onPlay = {},
            onReload = {},
            onClose = {},
            onViewFailed = { _, _ -> },
        )
    }

    private class SlowEntryPoint : CreatorEntryPoint {
        val isReady = CompletableDeferred<Unit>()

        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> {
            runBlocking { isReady.await() }
            return EditableMetadata.newSerializationManagerInstance()
        }
    }

    private object PlainEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()
    }

    private companion object {
        const val INSPECTOR_HINT = "Drag an Actor type from the sidebar into the Scene to place it there, and click an Actor to change it. Drag it to move it, or press Delete to remove it."
        const val REMOVE = "Remove from the Scene"
    }
}
