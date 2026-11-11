/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionControllerTest {
    private val rootDirectory = testDirectory()
    private val build = FakeProjectBuild()
    private val scene = ProjectResource(
        group = ResourceGroup.SCENES,
        file = File(rootDirectory, "src/commonMain/composeResources/files/scenes/level.json"),
        relativePath = "src/commonMain/composeResources/files/scenes/level.json",
    )

    private fun TestScope.createController(): WorkspaceController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return workspaceController(
            project = CreatorProject(rootDirectory = rootDirectory, name = "game"),
            build = build,
            sessionChannel = FakeSessionChannel(),
            parentScope = CoroutineScope(backgroundScope.coroutineContext + dispatcher),
            ioDispatcher = dispatcher,
            scanResources = { _, _ -> mapOf(ResourceGroup.SCENES to listOf(scene)) },
            blockTypeRepository = FakeBlockTypeRepository(listOf(ActorTypeDefinition(name = "Player"))),
            openGitRepository = { null },
        ).also { runCurrent() }
    }

    @Test
    fun selectingATypeClosesTheShownScene() = runTest {
        val controller = createController()
        controller.selection.openScene("level")
        assertTrue(controller.selection.selectType("Player"))
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
    }

    @Test
    fun openingASceneDeselectsTheType() = runTest {
        val controller = createController()
        controller.selection.selectType("Player")
        assertTrue(controller.selection.openScene("level"))
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
        assertNull(controller.blockTypes!!.selectedName.value)
    }

    @Test
    fun openingAPageClosesTheSceneAndDeselectsTheTypeAndEitherClosesThePage() = runTest {
        val controller = createController()
        controller.selection.openScene("level")
        controller.selection.openPage("hero")
        assertEquals(WorkspaceSelection.Page("hero"), controller.selection.current.value)
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
        assertTrue(controller.selection.selectType("Player"))
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
        controller.selection.openPage("hero")
        assertNull(controller.blockTypes!!.selectedName.value)
        assertTrue(controller.selection.openScene("level"))
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
        controller.selection.openPage("hero")
        controller.selection.clear()
        assertEquals(WorkspaceSelection.None, controller.selection.current.value)
    }

    @Test
    fun openingADocumentTakesThePagesPlaceAndClosesTheScene() = runTest {
        val controller = createController()
        controller.selection.openPage("hero")
        controller.selection.openDocument("docs/combat.md")
        assertEquals(WorkspaceSelection.Document("docs/combat.md"), controller.selection.current.value)
        controller.selection.openScene("level")
        controller.selection.openDocument("docs/combat.md")
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
        assertEquals(WorkspaceSelection.Document("docs/combat.md"), controller.selection.current.value)
        assertTrue(controller.selection.selectType("Player"))
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
    }

    @Test
    fun openingASpriteTakesThePagesPlaceAndClosesTheScene() = runTest {
        val controller = createController()
        controller.selection.openPage("hero")
        controller.selection.openSprite("drawable/ship.png")
        assertEquals(WorkspaceSelection.Sprite("drawable/ship.png"), controller.selection.current.value)
        controller.sceneEditing.open(scene)
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
        controller.selection.openSprite("drawable/ship.png")
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
        assertEquals(WorkspaceSelection.Sprite("drawable/ship.png"), controller.selection.current.value)
    }

    @Test
    fun anUnknownTypeOrSceneChangesNothing() = runTest {
        val controller = createController()
        controller.selection.selectType("Player")
        assertFalse(controller.selection.selectType("Missing"))
        assertFalse(controller.selection.openScene("missing"))
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
    }

    @Test
    fun openingTheSceneShownAlreadyDoesNotOpenItAgain() = runTest {
        val controller = createController()
        val states = mutableListOf<SceneEditingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.sceneEditing.state.collect { states += it } }
        controller.selection.openScene("level")
        assertTrue(controller.selection.openScene("level"))
        assertEquals(1, states.count { it is SceneEditingState.Preparing })
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
    }

    @Test
    fun aPreparingOrFailedSceneIsTheSelection() = runTest {
        val controller = createController()
        controller.selection.openScene("level")
        assertIs<SceneEditingState.Preparing>(controller.sceneEditing.state.value)
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
        build.runs.first().finish(isSuccessful = false)
        assertIs<SceneEditingState.Failed>(controller.sceneEditing.state.value)
        assertEquals(WorkspaceSelection.Scene("level"), controller.selection.current.value)
    }

    @Test
    fun deletingTheSelectedTypeSelectsNothing() = runTest {
        val controller = createController()
        controller.selection.selectType("Player")
        controller.commands.dispatch(Invocation(DeleteActorType, DeleteActorType.Input("Player")))
        assertEquals(WorkspaceSelection.None, controller.selection.current.value)
    }

    @Test
    fun renamingTheSelectedTypeFollowsIt() = runTest {
        val controller = createController()
        controller.selection.selectType("Player")
        controller.commands.dispatch(Invocation(RenameActorType, RenameActorType.Input("Player", "Hero")))
        assertEquals(WorkspaceSelection.BlockType("Hero"), controller.selection.current.value)
    }

    @Test
    fun clearSelectsNothing() = runTest {
        val controller = createController()
        controller.selection.openScene("level")
        controller.selection.clear()
        assertEquals(WorkspaceSelection.None, controller.selection.current.value)
        controller.selection.selectType("Player")
        controller.selection.clear()
        assertEquals(WorkspaceSelection.None, controller.selection.current.value)
        assertNull(controller.blockTypes!!.selectedName.value)
    }

    @Test
    fun aLeaveGuardDecidesWhetherTheCenterShowsSomethingElse() = runTest {
        val controller = createController()
        controller.selection.openSprite("drawable/hero.png")
        var asked: (() -> Unit)? = null
        controller.selection.leaveGuard = { proceed -> asked = proceed }
        assertTrue(controller.selection.isLeaveGuarded)
        controller.selection.openSprite("drawable/hero.png")
        assertNull(asked, "showing what is shown asks nothing")
        controller.selection.selectType("Player")
        assertEquals(WorkspaceSelection.Sprite("drawable/hero.png"), controller.selection.current.value, "nothing changes while it asks")
        asked!!()
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
        controller.selection.leaveGuard = null
        assertFalse(controller.selection.isLeaveGuarded)
        controller.selection.clear()
        assertEquals(WorkspaceSelection.None, controller.selection.current.value)
    }
}
