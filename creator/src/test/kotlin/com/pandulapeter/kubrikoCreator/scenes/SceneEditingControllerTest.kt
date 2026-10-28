/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_entry_point_missing
import kubriko.creator.generated.resources.scene_build_failed
import kubriko.creator.generated.resources.status_scene_opened
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SceneEditingControllerTest {
    private val root = createTempDirectory().toFile()
    private val scene = ProjectResource(ResourceGroup.SCENES, File(root, "scenes/main.json"), "scenes/main.json")
    private val otherScene = ProjectResource(ResourceGroup.SCENES, File(root, "scenes/other.json"), "scenes/other.json")
    private val entryPoint = object : CreatorEntryPoint {
        override fun createSerializationManager() = throw NotImplementedError()
    }
    private var projectVersion = "1.0.0"
    private var isBuildSuccessful = true
    private val tasks = mutableListOf<Pair<String, Map<String, String>>>()
    private val loadedClasspaths = mutableListOf<ProjectClasspath>()
    private var closedCount = 0
    private val reports = mutableListOf<ConsoleEntry>()
    private var loader = EntryPointLoader { classpath ->
        loadedClasspaths += classpath
        LoadedEntryPoint(entryPoint = entryPoint, close = { closedCount++ })
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun TestScope.createController() = SceneEditingController(
        creatorKubrikoVersion = "1.0.0",
        runTask = { taskName, properties ->
            tasks += taskName to properties
            File(properties.getValue("kubrikoCreatorClasspathFile")).writeText("kubrikoVersion=$projectVersion\n/game/build/libs/game.jar")
            isBuildSuccessful
        },
        entryPointLoader = { loader.load(it) },
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        report = { reports += it },
        createClasspathFile = { File.createTempFile("classpath", ".txt", root) },
    )

    @Test
    fun aSceneOfAProjectOnCreatorsVersionOpensInCreatorsWindow() = runTest {
        val controller = createController()
        controller.open(scene)
        val state = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(scene, state.scene)
        // The project's entry point, with its sprites read by Creator.
        assertIs<SpriteLoadingEntryPoint>(state.entryPoint)
        assertFailsWith<NotImplementedError> { state.entryPoint.createSerializationManager() }
        assertEquals(SessionInitScript.CLASSPATH_TASK_NAME, tasks.single().first)
        assertEquals("classpath", tasks.single().second["kubrikoCreatorMode"])
        assertEquals(listOf(File("/game/build/libs/game.jar")), loadedClasspaths.single().files)
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.status_scene_opened)), reports)
    }

    @Test
    fun theClasspathFileIsDeletedOnceRead() = runTest {
        createController().open(scene)
        assertTrue(File(tasks.single().second.getValue("kubrikoCreatorClasspathFile")).exists().not())
    }

    @Test
    fun aSceneOfAProjectOnAnotherVersionCantBeEdited() = runTest {
        projectVersion = "0.9.0"
        val controller = createController()
        controller.open(scene)
        assertEquals(SceneEditingState.Unsupported(scene, "0.9.0"), controller.state.value)
        assertTrue(reports.isEmpty())
        assertTrue(loadedClasspaths.isEmpty())
    }

    @Test
    fun aFailedBuildSaysSoUntilItIsTriedAgain() = runTest {
        isBuildSuccessful = false
        val controller = createController()
        controller.open(scene)
        assertEquals(SceneEditingState.Failed(scene, Res.string.scene_build_failed), controller.state.value)
        assertTrue(loadedClasspaths.isEmpty())
        isBuildSuccessful = true
        controller.reload()
        assertIs<SceneEditingState.Open>(controller.state.value)
    }

    @Test
    fun anEntryPointThatCantBeLoadedIsReported() = runTest {
        loader = EntryPointLoader { throw EntryPointLoadingException(Res.string.error_entry_point_missing) }
        val controller = createController()
        controller.open(scene)
        assertEquals(SceneEditingState.Failed(scene, Res.string.error_entry_point_missing), controller.state.value)
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.error_entry_point_missing, isError = true)), reports)
    }

    @Test
    fun changedActorTypesMarkTheOpenSceneOutdatedUntilReloaded() = runTest {
        val controller = createController()
        controller.markOutdated()
        assertEquals(SceneEditingState.Closed, controller.state.value)
        controller.open(scene)
        controller.markOutdated()
        val outdated = assertIs<SceneEditingState.Open>(controller.state.value)
        assertTrue(outdated.isOutdated)
        controller.reload()
        val reloaded = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(false, reloaded.isOutdated)
        assertTrue(reloaded.revision > outdated.revision)
        assertEquals(2, tasks.size)
    }

    @Test
    fun theOpenSceneShowsItsFileAgainWhenItChangesAndClosesWhenItIsDeleted() = runTest {
        val controller = createController()
        scene.file.parentFile.mkdirs()
        scene.file.writeText("[]")
        controller.open(scene)
        assertEquals("[]", assertIs<SceneEditingState.Open>(controller.state.value).content)
        scene.file.writeText("[{}]")
        controller.onScenesChanged(listOf("other"))
        assertEquals("[]", assertIs<SceneEditingState.Open>(controller.state.value).content)
        controller.onScenesChanged(listOf("main"))
        val changed = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals("[{}]", changed.content)
        assertEquals(false, changed.isOutdated)
        scene.file.delete()
        controller.onScenesChanged(listOf("main"))
        assertEquals(SceneEditingState.Closed, controller.state.value)
    }

    @Test
    fun theOpenSceneFollowsItsRename() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.onSceneRenamed("other", otherScene)
        assertEquals(scene, assertIs<SceneEditingState.Open>(controller.state.value).scene)
        controller.onSceneRenamed("main", otherScene)
        assertEquals(otherScene, assertIs<SceneEditingState.Open>(controller.state.value).scene)
    }

    @Test
    fun closingReleasesTheProjectsClassesOnceTheEditorIsGone() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.close()
        assertEquals(SceneEditingState.Closed, controller.state.value)
        assertEquals(0, closedCount)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
    }

    @Test
    fun openingAnotherSceneReplacesTheCurrentOne() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.open(otherScene)
        assertEquals(otherScene, assertIs<SceneEditingState.Open>(controller.state.value).scene)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
    }
}
