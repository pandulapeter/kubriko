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
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_entry_point_missing
import kubriko.creator.generated.resources.error_entry_point_not_loaded
import kubriko.creator.generated.resources.error_scene_view_failed
import kubriko.creator.generated.resources.scene_build_failed
import kubriko.creator.generated.resources.scene_unsupported
import kubriko.creator.generated.resources.scene_unsupported_library
import kubriko.creator.generated.resources.status_scene_newer_libraries
import kubriko.creator.generated.resources.status_scene_opened
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    private var projectLibraries = ""
    private var isBuildSuccessful = true
    private val tasks = mutableListOf<Pair<String, Map<String, String>>>()
    private val loadedClasspaths = mutableListOf<ProjectClasspath>()
    private var closedCount = 0
    private val reports = mutableListOf<ConsoleEntry>()
    private val sceneReads = mutableListOf<ProjectResource>()
    private var buildGate: CompletableDeferred<Unit>? = null
    private var cleanBuildRequests = 0
    private var sceneProblem: SceneFileProblem? = null
    private var warmUpCount = 0
    private val ballLook = ActorTypeLook(ActorShape.CIRCLE, color = 0xFFFFFFFF, width = 20f, height = 20f)
    private val brickLook = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFFE0605A, width = 100f, height = 36f)
    private var actorTypeLooks = mapOf("Ball" to ballLook, "Brick" to brickLook)
    private var writesGate: CompletableDeferred<Unit>? = null
    private val ball = SceneActorEntry("Ball", "{}", "a")
    private val brick = SceneActorEntry("Brick", "{}", "b")
    private var loader = EntryPointLoader { classpath ->
        loadedClasspaths += classpath
        LoadedEntryPoint(entryPoint = entryPoint, close = { closedCount++ })
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun TestScope.createController(
        scope: CoroutineScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        creatorLibraries: Map<String, String> = emptyMap(),
    ) = SceneEditingController(
        creatorKubrikoVersion = "1.0.0",
        creatorLibraries = creatorLibraries,
        runTask = { taskName, properties ->
            tasks += taskName to properties
            buildGate?.await()
            File(properties.getValue("kubrikoCreatorClasspathFile")).writeText("kubrikoVersion=$projectVersion\n/game/build/libs/game.jar$projectLibraries")
            isBuildSuccessful
        },
        entryPointLoader = { loader.load(it) },
        scope = scope,
        releaseScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        report = { reports += it },
        readScene = { resource ->
            sceneReads += resource
            if (resource.file.isFile) SceneDocument.parse(resource.file.readText()) else emptyList()
        },
        readSceneProblem = { sceneProblem },
        classpathFile = File(root, "classpath.txt"),
        currentTimeMillis = { 1_500L * tasks.size },
        requestCleanBuild = { cleanBuildRequests++ },
        warmUpCompiler = { warmUpCount++ },
        actorTypeLooks = { actorTypeLooks },
        awaitWrites = { writesGate?.await() },
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
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.status_scene_opened, arguments = listOf("1.5"))), reports)
    }

    @Test
    fun classesReleasedAsTheWorkspaceClosesAreClosedThoughItsScopeIsCancelled() = runTest {
        val workspaceScope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]) + UnconfinedTestDispatcher(testScheduler))
        val controller = createController(scope = workspaceScope)
        controller.open(scene)
        controller.dispose()
        workspaceScope.cancel()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
    }

    @Test
    fun classesLoadedWhileTheWorkspaceClosesAreClosedOnce() = runTest {
        val workspaceScope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]) + UnconfinedTestDispatcher(testScheduler))
        loader = EntryPointLoader {
            workspaceScope.cancel()
            LoadedEntryPoint(entryPoint = entryPoint, close = { closedCount++ })
        }
        val controller = createController(scope = workspaceScope)
        controller.open(scene)
        controller.dispose()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
    }

    @Test
    fun aSceneThatCantBeReadOpensSayingWhyAndFollowsItsFile() = runTest {
        sceneProblem = SceneFileProblem.Unparsable("Unexpected JSON token.")
        val controller = createController()
        controller.open(scene)
        assertEquals(sceneProblem, assertIs<SceneEditingState.Open>(controller.state.value).problem)
        sceneProblem = null
        scene.file.apply { parentFile.mkdirs() }.writeText("[]")
        controller.onScenesChanged(listOf("main"))
        assertNull(assertIs<SceneEditingState.Open>(controller.state.value).problem)
    }

    @Test
    fun aSceneWhoseViewTheProjectsCodeBreaksFailsWithTheProblem() = runTest {
        val controller = createController()
        controller.open(scene)
        val open = assertIs<SceneEditingState.Open>(controller.state.value)
        reports.clear()
        controller.onViewFailed(open.revision - 1, IllegalStateException("Stale."), isShowingOlderClasses = false)
        assertEquals(open, controller.state.value)
        controller.onViewFailed(open.revision, IllegalStateException("Broken."), isShowingOlderClasses = false)
        assertEquals(SceneEditingState.Failed(scene, Res.string.error_scene_view_failed, listOf("java.lang.IllegalStateException: Broken.")), controller.state.value)
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(Res.string.error_scene_view_failed, isError = true, arguments = listOf("java.lang.IllegalStateException: Broken."))), reports)
    }

    @Test
    fun newerClassesWhoseViewTheProjectsCodeBreaksLeaveTheOlderOnesShownAsOutdated() = runTest {
        val controller = createController()
        controller.open(scene)
        val open = assertIs<SceneEditingState.Open>(controller.state.value)
        controller.onViewFailed(open.revision, NoClassDefFoundError("com/example/Gone"), isShowingOlderClasses = true)
        assertEquals(open.copy(isOutdated = true), controller.state.value)
    }

    @Test
    fun preparingBuildsAndLoadsTheClassesSoTheFirstSceneOpensWithoutBuilding() = runTest {
        val controller = createController()
        controller.prepare()
        assertEquals(1, tasks.size)
        assertEquals(1, loadedClasspaths.size)
        assertEquals(SceneEditingState.Closed, controller.state.value)
        controller.prepare()
        controller.open(scene)
        assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(1, tasks.size)
    }

    @Test
    fun theCompilerIsWarmedUpOnceThePreparedClassesAreLoaded() = runTest {
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.prepare()
        assertEquals(0, warmUpCount)
        gate.complete(Unit)
        assertEquals(1, warmUpCount)
        controller.prepare()
        controller.open(scene)
        controller.markOutdated()
        controller.open(otherScene)
        assertEquals(1, warmUpCount, "only preparing warms up")
    }

    @Test
    fun theCompilerIsntWarmedUpAfterAFailedOrUnsupportedPreparation() = runTest {
        isBuildSuccessful = false
        createController().prepare()
        isBuildSuccessful = true
        projectVersion = "0.9.0"
        createController().prepare()
        assertEquals(0, warmUpCount)
    }

    @Test
    fun aSceneOpenedWhileTheClassesArePreparedWaitsForThatBuild() = runTest {
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.prepare()
        controller.open(scene)
        controller.open(otherScene)
        assertIs<SceneEditingState.Preparing>(controller.state.value)
        gate.complete(Unit)
        assertEquals(otherScene, assertIs<SceneEditingState.Open>(controller.state.value).scene)
        assertEquals(1, tasks.size)
    }

    @Test
    fun aChangeWhileTheClassesArePreparedBuildsAgainForTheFirstScene() = runTest {
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.prepare()
        controller.markOutdated()
        gate.complete(Unit)
        controller.open(scene)
        assertEquals(false, assertIs<SceneEditingState.Open>(controller.state.value).isOutdated)
        assertEquals(2, tasks.size)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount, "the prepared classes are released")
    }

    @Test
    fun aFailedPreparationIsShownByTheFirstSceneWithoutBuildingAgain() = runTest {
        isBuildSuccessful = false
        val controller = createController()
        controller.prepare()
        controller.open(scene)
        assertEquals(SceneEditingState.Failed(scene, Res.string.scene_build_failed), controller.state.value)
        assertEquals(1, tasks.size)
        controller.open(otherScene)
        assertEquals(2, tasks.size)
    }

    @Test
    fun disposingWhileTheClassesArePreparedStopsTheBuild() = runTest {
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.prepare()
        controller.dispose()
        gate.complete(Unit)
        runCurrent()
        assertTrue(loadedClasspaths.isEmpty())
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
        assertEquals(SceneEditingState.Unsupported(scene, Res.string.scene_unsupported, listOf("1.0.0", "0.9.0")), controller.state.value)
        assertTrue(reports.isEmpty())
        assertTrue(loadedClasspaths.isEmpty())
    }

    @Test
    fun aSceneOfAProjectOnANewerComposeCantBeEdited() = runTest {
        projectLibraries = "\nlibrary.org.jetbrains.compose.runtime:runtime-desktop=9.0.0"
        val controller = createController(creatorLibraries = mapOf("org.jetbrains.compose.runtime:runtime-desktop" to "1.12.2"))
        controller.open(scene)
        assertEquals(
            SceneEditingState.Unsupported(
                scene,
                Res.string.scene_unsupported_library,
                listOf("org.jetbrains.compose.runtime:runtime-desktop", "9.0.0", "1.12.2"),
            ),
            controller.state.value,
        )
        assertTrue(loadedClasspaths.isEmpty())
    }

    @Test
    fun newerOtherLibrariesAreReportedOnceAndTheSceneOpens() = runTest {
        projectLibraries = "\nlibrary.org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm=1.12.0"
        val controller = createController(creatorLibraries = mapOf("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm" to "1.11.0"))
        controller.open(scene)
        assertIs<SceneEditingState.Open>(controller.state.value)
        val warning = ConsoleEntry.Status(
            Res.string.status_scene_newer_libraries,
            arguments = listOf("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm 1.12.0 (Creator: 1.11.0)"),
        )
        assertEquals(1, reports.count { it == warning })
        controller.markOutdated()
        assertEquals(2, loadedClasspaths.size)
        assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(1, reports.count { it == warning }, "a rebuild doesn't repeat the warning")
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
    fun classesThatCantBeLoadedAreCompiledAgainFromScratch() = runTest {
        loader = EntryPointLoader { throw EntryPointLoadingException(Res.string.error_entry_point_not_loaded, NoSuchMethodError()) }
        createController().open(scene)
        assertEquals(1, cleanBuildRequests)
    }

    @Test
    fun aMissingEntryPointDoesntCompileEverythingAgain() = runTest {
        loader = EntryPointLoader { throw EntryPointLoadingException(Res.string.error_entry_point_missing) }
        createController().open(scene)
        assertEquals(0, cleanBuildRequests)
    }

    @Test
    fun reloadCompilesEverythingAgainButOpeningDoesnt() = runTest {
        val controller = createController()
        controller.reload()
        controller.open(scene)
        controller.markOutdated()
        controller.open(otherScene)
        assertEquals(0, cleanBuildRequests)
        controller.reload()
        assertEquals(1, cleanBuildRequests)
    }

    @Test
    fun aChangeBuildsTheOpenSceneAgainAndSwapsItsClassesOnceTheyAreReady() = runTest {
        val controller = createController()
        controller.markOutdated()
        controller.open(scene)
        val before = assertIs<SceneEditingState.Open>(controller.state.value)
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        controller.markOutdated()
        val updating = assertIs<SceneEditingState.Open>(controller.state.value)
        assertTrue(updating.isUpdating)
        assertEquals(before.revision, updating.revision, "the old classes stay until the new ones are ready")
        gate.complete(Unit)
        val after = assertIs<SceneEditingState.Open>(controller.state.value)
        assertTrue(after.revision > before.revision)
        assertEquals(false, after.isUpdating)
        assertEquals(false, after.isOutdated)
        assertEquals(2, tasks.size)
        assertEquals(0, cleanBuildRequests)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount, "the old classes are released")
        controller.close()
        controller.open(otherScene)
        assertEquals(2, tasks.size, "the new classes are kept for the next Scene")
    }

    @Test
    fun actorTypesChangedSinceTheClassesWereBuiltArePreviewedUntilNewerClassesAreSwappedIn() = runTest {
        val controller = createController()
        controller.open(scene)
        assertEquals(emptyMap(), assertIs<SceneEditingState.Open>(controller.state.value).previews)
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val circleBrick = brickLook.copy(shape = ActorShape.CIRCLE)
        val wall = brickLook.copy(width = 20f)
        actorTypeLooks = mapOf("Ball" to ballLook, "Brick" to circleBrick, "Wall" to wall)
        controller.onActorTypesChanged()
        assertEquals(mapOf("Brick" to circleBrick, "Wall" to wall), assertIs<SceneEditingState.Open>(controller.state.value).previews)
        controller.markOutdated()
        val greenBrick = circleBrick.copy(color = 0xFF00FF00)
        actorTypeLooks = mapOf("Ball" to ballLook, "Brick" to greenBrick, "Wall" to wall)
        controller.markOutdated()
        val second = CompletableDeferred<Unit>().also { buildGate = it }
        gate.complete(Unit)
        val swapped = assertIs<SceneEditingState.Open>(controller.state.value)
        assertTrue(swapped.isUpdating)
        assertEquals(mapOf("Brick" to greenBrick), swapped.previews, "what changed after the build started is still previewed")
        second.complete(Unit)
        assertEquals(emptyMap(), assertIs<SceneEditingState.Open>(controller.state.value).previews)
        assertEquals(3, tasks.size)
    }

    @Test
    fun aBuildKnowsTheLooksItCompilesOnlyOnceTheTypesAreWritten() = runTest {
        val gate = CompletableDeferred<Unit>().also { writesGate = it }
        actorTypeLooks = emptyMap()
        val controller = createController()
        controller.open(scene)
        actorTypeLooks = mapOf("Ball" to ballLook, "Brick" to brickLook)
        gate.complete(Unit)
        assertEquals(emptyMap(), assertIs<SceneEditingState.Open>(controller.state.value).previews)
    }

    @Test
    fun changesDuringABuildBuildAgainOnceItEnds() = runTest {
        val controller = createController()
        controller.open(scene)
        val first = CompletableDeferred<Unit>().also { buildGate = it }
        controller.markOutdated()
        controller.markOutdated()
        assertEquals(2, tasks.size, "the running build isn't cancelled")
        val second = CompletableDeferred<Unit>().also { buildGate = it }
        first.complete(Unit)
        assertEquals(3, tasks.size)
        assertTrue(assertIs<SceneEditingState.Open>(controller.state.value).isUpdating)
        second.complete(Unit)
        val after = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(false, after.isUpdating)
        assertEquals(3, tasks.size)
    }

    @Test
    fun aChangeWhileTheSceneOpensBuildsItAgainRightAfter() = runTest {
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.open(scene)
        controller.markOutdated()
        gate.complete(Unit)
        val open = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(false, open.isUpdating)
        assertEquals(2, tasks.size)
    }

    @Test
    fun aFailedBuildKeepsTheOldClassesAndSaysTheSceneIsOutdatedUntilABuildWorks() = runTest {
        val controller = createController()
        controller.open(scene)
        val before = assertIs<SceneEditingState.Open>(controller.state.value)
        isBuildSuccessful = false
        controller.markOutdated()
        val failed = assertIs<SceneEditingState.Open>(controller.state.value)
        assertTrue(failed.isOutdated)
        assertEquals(false, failed.isUpdating)
        assertEquals(before.revision, failed.revision)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(0, closedCount, "the shown classes stay")
        isBuildSuccessful = true
        controller.markOutdated()
        assertEquals(false, assertIs<SceneEditingState.Open>(controller.state.value).isOutdated)
        assertEquals(3, tasks.size)
    }

    @Test
    fun closingDuringABuildReleasesTheShownClassesAndKeepsTheNewOnesForTheNextScene() = runTest {
        val controller = createController()
        controller.open(scene)
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        controller.markOutdated()
        controller.close()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
        gate.complete(Unit)
        controller.open(scene)
        assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(2, tasks.size)
    }

    @Test
    fun reloadingDuringABuildBuildsFromScratch() = runTest {
        val controller = createController()
        controller.open(scene)
        CompletableDeferred<Unit>().also { buildGate = it }
        controller.markOutdated()
        buildGate = null
        controller.reload()
        assertEquals(1, cleanBuildRequests)
        assertEquals(3, tasks.size)
        assertEquals(false, assertIs<SceneEditingState.Open>(controller.state.value).isUpdating)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount, "the classes shown before Reload are released, and the cancelled build loaded none")
    }

    @Test
    fun theOpenSceneShowsItsActorsAgainWhenItChangesAndClosesWhenItIsDeleted() = runTest {
        val controller = createController()
        writeScene(ball)
        controller.open(scene)
        assertEquals(listOf(ball), assertIs<SceneEditingState.Open>(controller.state.value).actors)
        writeScene(ball, brick)
        controller.onScenesChanged(listOf("other"))
        assertEquals(listOf(ball), assertIs<SceneEditingState.Open>(controller.state.value).actors)
        controller.onScenesChanged(listOf("main"))
        val changed = assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(listOf(ball, brick), changed.actors)
        assertEquals(false, changed.isOutdated)
        scene.file.delete()
        controller.onScenesChanged(listOf("main"))
        assertEquals(SceneEditingState.Closed, controller.state.value)
    }

    @Test
    fun theOpenSceneIsReadAgainWhenScenesChangeOnDisk() = runTest {
        val controller = createController()
        writeScene(ball)
        controller.open(scene)
        writeScene(ball, brick)
        controller.onScenesChangedOnDisk()
        assertEquals(listOf(ball, brick), assertIs<SceneEditingState.Open>(controller.state.value).actors)
        scene.file.delete()
        controller.onScenesChangedOnDisk()
        assertEquals(SceneEditingState.Closed, controller.state.value)
    }

    @Test
    fun theSceneIsReadWhileTheProjectBuilds() = runTest {
        writeScene(ball)
        val gate = CompletableDeferred<Unit>().also { buildGate = it }
        val controller = createController()
        controller.open(scene)
        runCurrent()
        assertIs<SceneEditingState.Preparing>(controller.state.value)
        assertEquals(listOf(scene), sceneReads)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(ball), assertIs<SceneEditingState.Open>(controller.state.value).actors)
    }

    private fun writeScene(vararg actors: SceneActorEntry) {
        scene.file.parentFile.mkdirs()
        scene.file.writeText(SceneDocument.format(actors.toList()))
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
    fun disposingReleasesTheProjectsClassesOnceTheEditorIsGone() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.dispose()
        assertEquals(SceneEditingState.Closed, controller.state.value)
        assertEquals(0, closedCount)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
    }

    @Test
    fun closingKeepsTheProjectsClassesSoTheNextSceneOpensWithoutBuilding() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.close()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(0, closedCount)
        controller.open(scene)
        assertIs<SceneEditingState.Open>(controller.state.value)
        assertEquals(1, tasks.size)
        assertEquals(1, loadedClasspaths.size)
    }

    @Test
    fun aChangeSinceTheClassesWereLoadedBuildsAgainAndReleasesTheOldOnes() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.close()
        controller.markOutdated()
        controller.open(scene)
        assertEquals(2, tasks.size)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, closedCount)
        controller.reload()
        assertEquals(3, tasks.size, "reloading always builds again")
    }

    @Test
    fun openingAnotherSceneReplacesTheCurrentOne() = runTest {
        val controller = createController()
        controller.open(scene)
        controller.open(otherScene)
        assertEquals(otherScene, assertIs<SceneEditingState.Open>(controller.state.value).scene)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(0, closedCount, "both Scenes use the same classes")
        assertEquals(1, tasks.size)
    }
}
