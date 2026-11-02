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

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.EntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.LoadedEntryPoint
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.JavaArgumentFile
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.FakeGitRepository
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_changed_outside
import kubriko.creator.generated.resources.error_no_desktop_module
import kubriko.creator.generated.resources.error_project_newer_format
import kubriko.creator.generated.resources.error_project_newer_format_unknown_version
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.status_block_type_not_saved
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_building_with_unsaved_types
import kubriko.creator.generated.resources.status_built
import kubriko.creator.generated.resources.status_game_failed
import kubriko.creator.generated.resources.status_game_not_started
import kubriko.creator.generated.resources.status_game_reloaded
import kubriko.creator.generated.resources.status_kotlin_block_scope_changed
import kubriko.creator.generated.resources.status_play_from_scene_needs_upgrade
import kubriko.creator.generated.resources.status_unexpected_problem
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceControllerTest {
    private val rootDirectory = File("/projects/game")
    private val scene = ProjectResource(
        group = ResourceGroup.SCENES,
        file = File(rootDirectory, "src/commonMain/composeResources/files/scenes/level.json"),
        relativePath = "src/commonMain/composeResources/files/scenes/level.json",
    )
    private val build = FakeProjectBuild()
    private val channel = FakeSessionChannel()

    private fun TestScope.createController(
        desktopModulePath: String = ":",
        findDesktopModulePath: suspend () -> String = { desktopModulePath },
        ioDispatcher: CoroutineDispatcher? = null,
        scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { _, _ -> emptyMap() },
        blockTypeRepository: FakeBlockTypeRepository? = null,
        startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
        rootDirectory: File = this@WorkspaceControllerTest.rootDirectory,
        entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader(rootDirectory),
        runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = { _, _ -> GameProcess.Exit(0, emptyList()) },
        initialFormatStatus: ProjectFormatStatus = ProjectFormatStatus.Writable,
        writeDelayInMilliseconds: Long = 0,
        gitRepository: FakeGitRepository? = null,
        ownWrites: OwnWrites = OwnWrites(),
    ): WorkspaceController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkspaceController(
            project = CreatorProject(rootDirectory = rootDirectory, name = "game"),
            build = build,
            sessionChannel = channel,
            parentScope = CoroutineScope(backgroundScope.coroutineContext + dispatcher),
            ioDispatcher = ioDispatcher ?: dispatcher,
            findDesktopModulePath = findDesktopModulePath,
            scanResources = scanResources,
            blockTypeRepository = blockTypeRepository,
            startWatching = startWatching,
            creatorKubrikoVersion = CREATOR_KUBRIKO_VERSION,
            entryPointLoader = entryPointLoader,
            runGame = runGame,
            initialFormatStatus = initialFormatStatus,
            writeDelayInMilliseconds = writeDelayInMilliseconds,
            openGitRepository = { gitRepository },
            ownWrites = ownWrites,
        )
    }

    /**
     * Plays the game through the init script's session task: the classpath build before it says nothing about how to
     * launch the game.
     */
    private fun WorkspaceController.playThroughGradle() {
        play()
        build.currentRun.finish(isSuccessful = true)
    }

    private val WorkspaceController.statuses get() = console.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    @Test
    fun resourcesAreScannedFromTheProjectRootWhenOpened() = runTest {
        val scannedDirectories = mutableListOf<File>()
        val controller = createController(scanResources = { root, _ -> scannedDirectories += root; mapOf(ResourceGroup.SCENES to listOf(scene)) })
        assertEquals(listOf(rootDirectory), scannedDirectories)
        assertEquals(mapOf(ResourceGroup.SCENES to listOf(scene)), controller.resources.value)
    }

    @Test
    fun anUnexpectedFailureInTheBackgroundIsReportedInTheConsoleAndTheRestKeepsWorking() = runTest {
        var isFailing = false
        val controller = createController(scanResources = { _, _ -> if (isFailing) throw IllegalStateException("Broken.") else mapOf(ResourceGroup.SCENES to listOf(scene)) })
        isFailing = true
        controller.refreshResources()
        assertContains(controller.statuses, Res.string.status_unexpected_problem)
        isFailing = false
        controller.refreshResources()
        assertEquals(mapOf(ResourceGroup.SCENES to listOf(scene)), controller.resources.value)
    }

    @Test
    fun nothingReadsAPartBeforeItExists() = runTest {
        val controller = createController(
            blockTypeRepository = FakeBlockTypeRepository(listOf(ActorTypeDefinition(name = "Ball"))),
            findDesktopModulePath = { ":desktop" },
            gitRepository = FakeGitRepository(),
        )
        assertEquals(emptyList(), controller.console.value.filterIsInstance<ConsoleEntry.Status>().filter { it.text == Res.string.status_unexpected_problem })
        assertEquals(listOf("Ball"), controller.blockTypes?.types?.value?.map { it.name })
    }

    @Test
    fun aWatcherChangeIsScannedFromTheWatchersSnapshot() = runTest {
        val snapshots = mutableListOf<ProjectSnapshot?>()
        val controller = createController(scanResources = { _, snapshot -> snapshots += snapshot; emptyMap() })
        val snapshot = ProjectSnapshot(emptyMap())
        controller.onProjectChanged(setOf(ProjectChange.SCENES), snapshot)
        assertEquals(listOf(null, snapshot), snapshots)
    }

    @Test
    fun aWatcherSnapshotTakenBeforeOneOfCreatorsOwnWritesIsScannedByWalking() = runTest {
        val snapshots = mutableListOf<ProjectSnapshot?>()
        val ownWrites = OwnWrites()
        val controller = createController(scanResources = { _, snapshot -> snapshots += snapshot; emptyMap() }, ownWrites = ownWrites)
        val snapshot = ProjectSnapshot(emptyMap(), ownWriteGeneration = ownWrites.generation)
        ownWrites.record(File(rootDirectory, "scenes/main.json"))
        controller.onProjectChanged(setOf(ProjectChange.SCENES), snapshot)
        assertEquals(listOf<ProjectSnapshot?>(null, null), snapshots)
    }

    @Test
    fun refreshingRescansTheResources() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { _, _ -> scanCount++; emptyMap() })
        controller.refreshResources()
        assertEquals(2, scanCount)
    }

    @Test
    fun playRunsTheSessionTaskOfTheRootModuleInPlayMode() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        assertEquals(":${SessionInitScript.SESSION_TASK_NAME}", build.currentRun.taskPath)
        val arguments = build.currentRun.arguments
        assertEquals(SessionInitScript.file.absolutePath, arguments[arguments.indexOf("--init-script") + 1])
        assertTrue(File(arguments[arguments.indexOf("--init-script") + 1]).isFile)
        assertContains(arguments, "-PkubrikoCreatorPort=${channel.port}")
        assertContains(arguments, "-PkubrikoCreatorMode=play")
        assertEquals(SessionState.Starting(SessionMode.PLAY), controller.sessionState.value)
        assertEquals(listOf(Res.string.status_building, Res.string.status_built, Res.string.status_building), controller.statuses)
    }

    @Test
    fun everyLaunchWaitsForASessionOfItsOwn() = runTest {
        val controller = createController()
        controller.play()
        assertEquals(1, channel.expectedSessions)
        controller.play()
        assertEquals(2, channel.expectedSessions, "restarting the game goes through play() as well")
    }

    @Test
    fun theDesktopModuleIsAskedForAgainAfterALookupThatFailed() = runTest {
        var lookups = 0
        val controller = createController(
            findDesktopModulePath = {
                lookups++
                if (lookups == 1) throw ProjectInspectionException(Res.string.error_no_desktop_module) else ":"
            },
        )
        controller.play()
        assertTrue(build.runs.isEmpty())
        assertEquals(1, lookups, "the next lookup waits until it is needed, so it reads the files as they are then")
        controller.play()
        assertEquals(1, build.runs.size)
    }

    @Test
    fun aBuildWaitsForTheBlockTypeChangesThatAreStillBeingWritten() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository, writeDelayInMilliseconds = 100)
        advanceTimeBy(101)
        repository.writes.clear()
        assertNull(controller.createActorType("Player"))
        controller.play()
        assertTrue(build.runs.isEmpty())
        advanceTimeBy(101)
        assertEquals(listOf("save Player", "regenerate"), repository.writes)
        assertEquals(1, build.runs.size)
    }

    @Test
    fun creatorsOwnSaveRefreshesGit() = runTest {
        val gitRepository = FakeGitRepository()
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository(), writeDelayInMilliseconds = 100, gitRepository = gitRepository)
        advanceTimeBy(101)
        val statusCalls = gitRepository.statusCalls
        assertNull(controller.createActorType("Player"))
        assertEquals(statusCalls, gitRepository.statusCalls)
        advanceTimeBy(101)
        assertTrue(gitRepository.statusCalls > statusCalls)
    }

    @Test
    fun aGameWaitsForTheSceneEditorsClasspathBuildAndThenBuildsItsOwn() = runTest {
        val controller = createController()
        controller.sceneEditing.prepare()
        controller.play()
        assertEquals(1, build.runs.size, "the game's build waits for the Scene editor's")
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(":${SessionInitScript.CLASSPATH_TASK_NAME}", ":${SessionInitScript.CLASSPATH_TASK_NAME}"), build.runs.map { it.taskPath })
        assertContains(build.currentRun.arguments, "-PkubrikoCreatorClasspathFile=${SessionInitScript.playClasspathFileOf(rootDirectory).absolutePath}")
        build.currentRun.finish(isSuccessful = true)
        assertEquals(":${SessionInitScript.SESSION_TASK_NAME}", build.currentRun.taskPath)
    }

    @Test
    fun closingWritesWhatIsPendingBeforeTheBuildIsReleased() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        controller.close()
        assertFalse(build.isClosed)
        runCurrent()
        assertEquals(listOf("regenerate"), repository.writes)
        assertTrue(build.isClosed)
    }

    @Test
    fun closeWaitsForARetryThatSucceeds() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository)
        repository.failingNames += "Player"
        assertNull(controller.createActorType("Player"))
        val closing = controller.close()
        repository.failingNames.clear()
        advanceTimeBy(2_001)
        assertContains(repository.writes, "save Player")
        assertTrue(closing.isCompleted)
        assertTrue(build.isClosed)
    }

    @Test
    fun closeGivesUpOnAWriteThatKeepsFailingAfterTenSeconds() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository)
        repository.failingNames += "Player"
        assertNull(controller.createActorType("Player"))
        val closing = controller.close()
        advanceTimeBy(9_999)
        assertFalse(closing.isCompleted)
        advanceTimeBy(2)
        assertTrue(closing.isCompleted)
        assertTrue(build.isClosed)
    }

    @Test
    fun aBuildWhileATypeCannotBeSavedWarnsAndStillRuns() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository)
        repository.failingNames += "Player"
        assertNull(controller.createActorType("Player"))
        controller.play()
        assertContains(controller.console.value, ConsoleEntry.Status(Res.string.status_building_with_unsaved_types, isError = true, arguments = listOf("Player", "Locked.")))
        assertEquals(1, build.runs.size)
    }

    @Test
    fun theCompilerIsWarmedUpSilentlyOnceThePreparedClassesAreLoadedAndASessionWaitsForIt() = runTest {
        val entryPoint = object : CreatorEntryPoint {
            override fun createSerializationManager() = throw NotImplementedError()
        }
        val controller = createController(entryPointLoader = { LoadedEntryPoint(entryPoint = entryPoint, close = {}) })
        controller.sceneEditing.prepare()
        SessionInitScript.classpathFileOf(rootDirectory).writeText("kubrikoVersion=$CREATOR_KUBRIKO_VERSION\n")
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(":${SessionInitScript.CLASSPATH_TASK_NAME}", ":${SessionInitScript.WARM_UP_TASK_NAME}"), build.runs.map { it.taskPath })
        assertContains(build.currentRun.arguments, "-PkubrikoCreatorMode=warmUp")
        val console = controller.console.value
        controller.play()
        assertEquals(2, build.runs.size)
        build.currentRun.finish(isSuccessful = false)
        assertEquals(":${SessionInitScript.CLASSPATH_TASK_NAME}", build.currentRun.taskPath)
        assertFalse("--rerun-tasks" in build.currentRun.arguments, "a failed warm-up doesn't make the next build a clean one")
        assertEquals(console + ConsoleEntry.Status(Res.string.status_building), controller.console.value)
    }

    @Test
    fun aGameTheBuildSaysHowToLaunchIsPlayedWithoutGradleFromTheChosenScene() = runTest {
        val playClasspathFile = SessionInitScript.playClasspathFileOf(rootDirectory)
        try {
            val launches = mutableListOf<Pair<GameLaunch, Map<String, String>>>()
            val exit = CompletableDeferred<GameProcess.Exit>()
            val controller = createController(runGame = { launch, properties -> launches += launch to properties; exit.await() })
            controller.play(firstScene = "levels/1-1")
            playClasspathFile.writeText("kubrikoVersion=1.0.0\n/game/classes\nlaunch.java=/jdk/bin/java\nlaunch.mainClass=game.MainKt\nlaunch.workingDir=/game\nlaunch.classpath=/game/classes\n")
            build.currentRun.finish(isSuccessful = true)
            assertEquals(1, build.runs.size, "Gradle doesn't start the game")
            val (launch, properties) = launches.single()
            assertEquals(GameLaunch(File("/jdk/bin/java"), "game.MainKt", File("/game"), emptyList(), listOf(File("/game/classes"))), launch)
            assertEquals(mapOf("kubriko.creator.port" to channel.port.toString(), "kubriko.creator.logging" to "true", "kubriko.creator.scene" to "levels/1-1"), properties)
            exit.complete(GameProcess.Exit(1, listOf("Exception in thread \"main\"")))
            assertEquals(Res.string.status_game_failed, controller.statuses.last())
            assertEquals(ConsoleEntry.BuildOutput("Exception in thread \"main\""), controller.console.value.last())
            assertEquals(SessionState.Idle, controller.sessionState.value)
        } finally {
            playClasspathFile.delete()
        }
    }

    @Test
    fun closingWaitsUntilTheGameHasStopped() = runTest {
        val playClasspathFile = SessionInitScript.playClasspathFileOf(rootDirectory)
        try {
            val stopped = CompletableDeferred<Unit>()
            val controller = createController(
                runGame = { _, _ ->
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) { stopped.await() }
                    }
                },
            )
            controller.play()
            playClasspathFile.writeText("kubrikoVersion=1.0.0\nlaunch.java=/java\nlaunch.mainClass=M\nlaunch.workingDir=/game\nlaunch.classpath=/game/classes\n")
            build.currentRun.finish(isSuccessful = true)
            val closing = controller.close()
            runCurrent()
            assertFalse(closing.isCompleted)
            stopped.complete(Unit)
            runCurrent()
            assertTrue(closing.isCompleted)
        } finally {
            playClasspathFile.delete()
        }
    }

    @Test
    fun aGameThatCantBeStartedSaysWhyInTheConsole() = runTest {
        val playClasspathFile = SessionInitScript.playClasspathFileOf(rootDirectory)
        try {
            val controller = createController(runGame = { _, _ -> throw IOException("Cannot run program \"/java\"") })
            controller.play()
            playClasspathFile.writeText("kubrikoVersion=1.0.0\nlaunch.java=/java\nlaunch.mainClass=M\nlaunch.workingDir=/game\nlaunch.classpath=/game/classes\n")
            build.currentRun.finish(isSuccessful = true)
            assertEquals(Res.string.status_game_not_started, controller.statuses.last())
            assertEquals(SessionState.Idle, controller.sessionState.value)
        } finally {
            playClasspathFile.delete()
        }
    }

    @Test
    fun stoppingAGamePlayedWithoutGradleStopsItsProcess() = runTest {
        val playClasspathFile = SessionInitScript.playClasspathFileOf(rootDirectory)
        try {
            var isStopped = false
            val controller = createController(
                runGame = { _, _ ->
                    try {
                        awaitCancellation()
                    } finally {
                        isStopped = true
                    }
                },
            )
            controller.play()
            playClasspathFile.writeText("kubrikoVersion=1.0.0\nlaunch.java=/java\nlaunch.mainClass=M\nlaunch.workingDir=/game\nlaunch.classpath=/game/classes\n")
            build.currentRun.finish(isSuccessful = true)
            controller.stopSession()
            assertTrue(isStopped)
            assertEquals(SessionState.Idle, controller.sessionState.value)
        } finally {
            playClasspathFile.delete()
        }
    }

    @Test
    fun aGameStartedWithHotReloadGetsTheChangesMadeWhileItPlays() = runTest {
        val root = createTempDirectory().toFile()
        val playClasspathFile = SessionInitScript.playClasspathFileOf(root)
        val classpathFile = SessionInitScript.classpathFileOf(root)
        try {
            val classes = File(root, "build/classes").apply { mkdirs() }
            val argumentFile = File(root, "build/run/game.argfile").apply { parentFile.mkdirs() }
            argumentFile.writeText(JavaArgumentFile.format(listOf("-javaagent:/libs/agent.jar", "-cp", File(root, "build/run/classes").path, "game.MainKt")))
            val launches = mutableListOf<GameLaunch>()
            val controller = createController(rootDirectory = root, runGame = { launch, _ -> launches += launch; awaitCancellation() })
            controller.play()
            assertContains(build.currentRun.arguments, "-PkubrikoCreatorHotReload=true")
            playClasspathFile.writeText(
                "kubrikoVersion=1.0.0\n${classes.path}\nlaunch.java=/jdk/bin/java\nlaunch.mainClass=game.MainKt\nlaunch.workingDir=${root.path}\n" +
                    "launch.classpath=${classes.path}\nlaunch.hotJava=/jbr/bin/java\nlaunch.hotArgumentFile=${argumentFile.path}\n" +
                    "launch.hotReloadTask=hotReloadDesktopMain\nlaunch.hotLibrary=/libs/session.jar\n",
            )
            build.currentRun.finish(isSuccessful = true)
            val launch = launches.single()
            assertEquals(File("/jbr/bin/java"), launch.java)
            assertEquals(listOf(File(root, "build/run/classes"), File("/libs/session.jar")), launch.classpath)
            assertEquals("hotReloadDesktopMain", launch.reloadTaskName)
            channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY))
            controller.onProjectChanged(setOf(ProjectChange.SOURCES))
            assertEquals(":${SessionInitScript.CLASSPATH_TASK_NAME}", build.currentRun.taskPath)
            classpathFile.writeText("kubrikoVersion=1.0.0\n${classes.path}\n")
            build.currentRun.finish(isSuccessful = true)
            assertEquals(":hotReloadDesktopMain", build.currentRun.taskPath)
            build.currentRun.finish(isSuccessful = true)
            assertContains(controller.statuses, Res.string.status_game_reloaded)
            assertEquals(1, launches.size)
        } finally {
            playClasspathFile.delete()
            classpathFile.delete()
            root.deleteRecursively()
        }
    }

    @Test
    fun aFailedBuildStartsNoGame() = runTest {
        val controller = createController()
        controller.play()
        build.currentRun.finish(isSuccessful = false)
        assertEquals(1, build.runs.size)
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun playingFromASceneInAProjectThatNeedsAnUpgradeSaysItMayStartElsewhere() = runTest {
        val root = createTempDirectory().toFile()
        try {
            File(root, ".kubriko").mkdirs()
            File(root, ".kubriko/project.json").writeText("""{ "packageName": "com.example.game", "templateVersion": 6 }""")
            File(root, "gradle.properties").writeText("org.gradle.caching=true\norg.gradle.configuration-cache=true\n")
            File(root, "src/desktopMain/kotlin/com/example/game/Main.kt").apply { parentFile.mkdirs() }.writeText("fun main() {\n    KubrikoWiring.isLoggingEnabled = true\n}\n")
            val controller = createController(rootDirectory = root)
            controller.play()
            assertTrue(Res.string.status_play_from_scene_needs_upgrade !in controller.statuses)
            controller.play(firstScene = "main")
            assertContains(controller.statuses, Res.string.status_play_from_scene_needs_upgrade)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun theConsoleKeepsOnlyTheMostRecentEntries() = runTest {
        val controller = createController()
        repeat(2_100) { channel.receive(SessionMessage.Failure(message = "$it")) }
        assertEquals(2_000, controller.console.value.size)
        assertEquals(ConsoleEntry.SessionFailure("2099"), controller.console.value.last())
    }

    @Test
    fun clearingEmptiesTheConsole() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Failure(message = "x"))
        controller.clearConsole()
        assertTrue(controller.console.value.isEmpty())
    }

    @Test
    fun closingStopsTheSessionAndReleasesTheBuildAndTheChannel() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        controller.close()
        assertTrue(build.currentRun.isCancelled)
        assertTrue(build.isClosed)
        assertTrue(channel.isClosed)
    }

    @Test
    fun actorTypesAreOnlyAvailableToProjectsThatOptedIn() = runTest {
        assertNull(createController().blockTypes)
        assertNotNull(createController(blockTypeRepository = FakeBlockTypeRepository()).blockTypes)
    }

    @Test
    fun savingAnActorTypeDoesntRescanTheResourcesWhichLeaveItsGeneratedCodeOut() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { _, _ -> scanCount++; emptyMap() }, blockTypeRepository = FakeBlockTypeRepository())
        assertEquals(1, scanCount)
        assertNull(controller.createActorType("Player"))
        assertEquals(1, scanCount)
        assertEquals("Player", controller.blockTypes!!.selectedName.value)
    }

    @Test
    fun creatingAnActorOrManagerTypeClosesTheSceneShownAndSelectsTheType() = runTest {
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository())
        controller.sceneEditing.open(scene)
        assertNull(controller.createActorType("Player"))
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
        assertEquals("Player", controller.blockTypes!!.selectedName.value)
        assertEquals(WorkspaceSelection.BlockType("Player"), controller.selection.current.value)
        controller.sceneEditing.open(scene)
        assertNull(controller.createManagerType("Game"))
        assertEquals(SceneEditingState.Closed, controller.sceneEditing.state.value)
        assertEquals("Game", controller.blockTypes!!.selectedName.value)
        assertEquals(WorkspaceSelection.BlockType("Game"), controller.selection.current.value)
    }

    @Test
    fun aFailedActorTypeSaveIsReportedInTheConsole() = runTest {
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository().apply { isFailing = true })
        controller.clearConsole()
        controller.createActorType("Player")
        assertEquals(listOf(ConsoleEntry.Status(Res.string.status_block_type_not_saved, isError = true, arguments = listOf("Player", "Disk full."))), controller.console.value)
    }

    @Test
    fun openingAProjectNamesEachTypeWhoseKotlinBlocksRelyOnWhatTheEventFunctionsChanged() = runTest {
        val kotlin = { code: String -> EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin(code))) }
        val repository = FakeBlockTypeRepository(
            listOf(
                ActorTypeDefinition(name = "Ball", events = listOf(kotlin("return"), kotlin("if (score > 0f) return"))),
                ActorTypeDefinition(name = "Paddle", events = listOf(kotlin("velocity = SceneOffset.Zero"))),
            ),
        )
        val controller = createController(blockTypeRepository = repository)
        runCurrent()
        assertEquals(
            listOf(ConsoleEntry.Status(Res.string.status_kotlin_block_scope_changed, isError = true, arguments = listOf("Ball"))),
            controller.console.value.filter { it is ConsoleEntry.Status && it.text == Res.string.status_kotlin_block_scope_changed },
        )
    }

    @Test
    fun anActorTypeNameThatCantBeUsedIsExplainedAndCreatesNothing() = runTest {
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository())
        assertEquals(Res.string.error_type_name_invalid, controller.createActorType("ball game"))
        assertTrue(controller.blockTypes!!.actorTypes.value.isEmpty())
        assertNull(controller.commands.history.value.nextUndoCommandName)
    }

    @Test
    fun aSceneNameThatCantBeUsedIsExplainedAndCreatesNothing() = runTest {
        val controller = createController()
        assertEquals(Res.string.error_scene_name_invalid, controller.createScene("my level"))
        assertNull(controller.commands.history.value.nextUndoCommandName)
    }

    @Test
    fun rejectedUndoStepsAreReportedInTheConsole() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository)
        controller.createActorType("Player")
        repository.stored.clear()
        controller.onProjectChanged(setOf(ProjectChange.BLOCK_TYPE_DEFINITIONS))
        controller.clearConsole()
        controller.commands.undo()
        assertEquals(listOf(ConsoleEntry.Status(Res.string.error_command_changed_outside, isError = true, arguments = listOf("Player"))), controller.console.value)
    }

    @Test
    fun theProjectIsWatchedUntilTheWorkspaceCloses() = runTest {
        var isWatching = false
        val controller = createController(startWatching = { isWatching = true; AutoCloseable { isWatching = false } })
        assertTrue(isWatching)
        controller.close()
        assertFalse(isWatching)
    }

    @Test
    fun changedSourcesRescanTheResources() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { _, _ -> scanCount++; emptyMap() })
        controller.onProjectChanged(setOf(ProjectChange.SOURCES))
        controller.onProjectChanged(setOf(ProjectChange.SCENES))
        assertEquals(3, scanCount)
    }

    @Test
    fun changedScenesRegenerateTheCodeSinceTheirMusicDecidesWhetherTheGamePlaysAudio() = runTest {
        val root = createTempDirectory().toFile()
        try {
            val repository = FakeBlockTypeRepository()
            val controller = createController(blockTypeRepository = repository, rootDirectory = root)
            repository.writes.clear()
            controller.onProjectChanged(setOf(ProjectChange.SCENES))
            assertEquals(listOf("regenerate"), repository.writes)
            assertNull(controller.createScene("level2"))
            assertEquals(listOf("regenerate", "regenerate"), repository.writes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun changedDefinitionsAreReloaded() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository)
        repository.stored["Ball"] = com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition(name = "Ball")
        controller.onProjectChanged(setOf(ProjectChange.BLOCK_TYPE_DEFINITIONS))
        assertEquals(listOf("Ball"), controller.blockTypes!!.actorTypes.value.map { it.name })
    }

    @Test
    fun theTemplateUpgradeIsCheckedWhenOpenedAndWhenSourcesChange() = runTest {
        val root = createTempDirectory().toFile()
        try {
            File(root, ".kubriko").mkdirs()
            File(root, ".kubriko/project.json").writeText("""{ "packageName": "com.example.game" }""")
            File(root, "gradle.properties").writeText(
                "org.gradle.caching=true\norg.gradle.configuration-cache=true\n" +
                    "game.name=Game\ngame.versionName=1.0.0\ngame.versionNumber=1\ngame.applicationId=com.example.game\ngame.vendor=\ngame.description=\n",
            )
            File(root, "build.gradle.kts").writeText("// Reads game.name.\n")
            val game = File(root, "src/commonMain/kotlin/com/example/game/Game.kt").apply { parentFile.mkdirs() }
            game.writeText("package com.example.game\n\nval kubriko = Kubriko.newInstance(\n    SceneLoader(scenePath = \"files/scenes/main.json\"),\n)\n")
            File(root, "src/desktopMain/kotlin/com/example/game/Main.kt").apply { parentFile.mkdirs() }.writeText("// Sets KubrikoWiring.isLoggingEnabled and KubrikoWiring.firstSceneOverride.\n")
            val controller = createController(rootDirectory = root)
            val upgrade = assertIs<TemplateUpgrade.Needed>(controller.projectUpgrade.value)
            assertEquals(listOf("src/commonMain/kotlin/com/example/game/GameEntryPoint.kt"), upgrade.byHand)
            game.writeText(checkNotNull(upgrade.changes.single().after))
            File(root, "src/commonMain/kotlin/com/example/game/GameEntryPoint.kt").writeText("// Uses KubrikoWiring.\n")
            controller.onProjectChanged(setOf(ProjectChange.SOURCES))
            assertEquals(TemplateUpgrade.UpToDate, controller.projectUpgrade.value)
            assertTrue("\"templateVersion\": ${ProjectUpgrader.CURRENT_VERSION}" in File(root, ".kubriko/project.json").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun aProjectSavedByANewerCreatorIsOnlyBrowsedUntilItsFormatIsOneThisCreatorWrites() = runTest {
        val root = createTempDirectory().toFile()
        try {
            val settingsFile = File(root, ".kubriko/project.json").apply { parentFile.mkdirs() }
            settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": 99, "minimumCreatorVersion": "9.0.0" }""")
            val repository = FakeBlockTypeRepository()
            val controller = createController(blockTypeRepository = repository, rootDirectory = root, initialFormatStatus = ProjectFormatStatus.ReadOnly(99, "9.0.0"))
            controller.onProjectChanged(setOf(ProjectChange.SCENES))
            assertEquals(emptyList(), repository.writes)
            assertEquals(TemplateUpgrade.UpToDate, controller.projectUpgrade.value)
            assertEquals(Res.string.error_project_newer_format, controller.createActorType("Ball"))
            settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": ${ProjectFormat.CURRENT_VERSION} }""")
            controller.onProjectChanged(setOf(ProjectChange.SETTINGS))
            assertEquals(ProjectFormatStatus.Writable, controller.formatStatus.value)
            assertEquals(listOf("regenerate"), repository.writes)
            assertIs<TemplateUpgrade.Needed>(controller.projectUpgrade.value)
            assertNull(controller.createActorType("Ball"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun aProjectCheckedOutAtANewerFormatWhileOpenStopsBeingChanged() = runTest {
        val root = createTempDirectory().toFile()
        try {
            val settingsFile = File(root, ".kubriko/project.json").apply { parentFile.mkdirs() }
            settingsFile.writeText("""{ "packageName": "com.example.game" }""")
            val controller = createController(blockTypeRepository = FakeBlockTypeRepository(), rootDirectory = root)
            settingsFile.writeText("""{ "packageName": "com.example.game", "formatVersion": 99 }""")
            controller.onProjectChanged(setOf(ProjectChange.SETTINGS))
            assertEquals(ProjectFormatStatus.ReadOnly(99, null), controller.formatStatus.value)
            assertEquals(Res.string.error_project_newer_format_unknown_version, controller.createScene("level"))
        } finally {
            root.deleteRecursively()
        }
    }

    private companion object {
        const val CREATOR_KUBRIKO_VERSION = "1.0.0"
    }
}
