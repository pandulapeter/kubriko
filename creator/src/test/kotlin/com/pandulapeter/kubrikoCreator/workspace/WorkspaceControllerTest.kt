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
import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.EntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.LoadedEntryPoint
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_game_command_failed
import kubriko.creator.generated.resources.error_game_no_answer
import kubriko.creator.generated.resources.error_game_not_running
import kubriko.creator.generated.resources.error_game_unsupported
import kubriko.creator.generated.resources.error_no_desktop_module
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.status_actor_type_not_saved
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_built
import kubriko.creator.generated.resources.status_game_failed
import kubriko.creator.generated.resources.status_game_not_started
import kubriko.creator.generated.resources.status_game_reloaded
import kubriko.creator.generated.resources.status_play_from_scene_needs_upgrade
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_unsupported_protocol
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
        findDesktopModulePath: () -> String = { desktopModulePath },
        ioDispatcher: CoroutineDispatcher? = null,
        scanResources: (File) -> Map<ResourceGroup, List<ProjectResource>> = { emptyMap() },
        blockTypeRepository: FakeBlockTypeRepository? = null,
        startWatching: (onChange: (Set<ProjectChange>) -> Unit) -> AutoCloseable = { AutoCloseable {} },
        rootDirectory: File = this@WorkspaceControllerTest.rootDirectory,
        entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader,
        runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = { _, _ -> GameProcess.Exit(0, emptyList()) },
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
        val controller = createController(scanResources = { scannedDirectories += it; mapOf(ResourceGroup.SCENES to listOf(scene)) })
        assertEquals(listOf(rootDirectory), scannedDirectories)
        assertEquals(mapOf(ResourceGroup.SCENES to listOf(scene)), controller.resources.value)
    }

    @Test
    fun refreshingRescansTheResources() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { scanCount++; emptyMap() })
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
    fun commonCodeCompilesIncrementallyUnlessTheProjectDeclaresExpectsAndAFailedBuildIsFollowedByACleanOne() = runTest {
        val root = createTempDirectory().toFile()
        try {
            val controller = createController(rootDirectory = root)
            val property = "-P${SessionInitScript.INCREMENTAL_COMMON_CODE_PROPERTY}"
            controller.play()
            assertContains(build.currentRun.arguments, "$property=true")
            assertFalse("--rerun-tasks" in build.currentRun.arguments)
            build.currentRun.finish(isSuccessful = false)
            controller.play()
            assertContains(build.currentRun.arguments, "$property=true")
            assertContains(build.currentRun.arguments, "--rerun-tasks")
            build.currentRun.finish(isSuccessful = true)
            controller.play()
            assertFalse("--rerun-tasks" in build.currentRun.arguments)
            build.currentRun.finish(isSuccessful = true)
            root.resolve("src/commonMain/kotlin/Platform.kt").apply { parentFile.mkdirs() }.writeText("expect fun name(): String\n")
            controller.play()
            assertContains(build.currentRun.arguments, "$property=false")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun aProjectWithoutADesktopModuleSaysWhyItCantBuild() = runTest {
        val controller = createController(findDesktopModulePath = { throw ProjectInspectionException(Res.string.error_no_desktop_module) })
        controller.play()
        assertEquals(listOf(Res.string.status_building, Res.string.error_no_desktop_module), controller.statuses)
        assertTrue(build.runs.isEmpty())
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
        controller.play()
        assertEquals(1, build.runs.size)
    }

    @Test
    fun aBuildWaitsForTheChangesThatAreStillBeingWritten() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createController(blockTypeRepository = repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        controller.play()
        assertTrue(build.runs.isEmpty())
        runCurrent()
        assertEquals(listOf("regenerate"), repository.writes)
        assertEquals(1, build.runs.size)
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
    fun aSessionWaitsForTheClasspathBuildBeforeIt() = runTest {
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
            argumentFile.writeText("\"-javaagent:/libs/agent.jar\"\n\"-cp\"\n\"${File(root, "build/run/classes").path}\"\n\"game.MainKt\"\n")
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
    fun theSessionTaskOfANestedModuleIsAddressedByItsPath() = runTest {
        createController(desktopModulePath = ":app:desktop").playThroughGradle()
        assertEquals(":app:desktop:${SessionInitScript.SESSION_TASK_NAME}", build.currentRun.taskPath)
    }

    @Test
    fun aHelloInTheCurrentProtocolMarksTheSessionAsRunning() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY))
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.sessionState.value)
        assertEquals(Res.string.status_session_started, controller.statuses.last())
    }

    @Test
    fun aHelloInANewerProtocolStillRunsTheSession() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION + 1, mode = SessionMode.PLAY))
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.sessionState.value)
    }

    @Test
    fun aHelloInAnInvalidProtocolStopsTheSession() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        channel.receive(SessionMessage.Hello(protocolVersion = 0, mode = SessionMode.PLAY))
        assertTrue(build.currentRun.isCancelled)
        assertEquals(SessionState.Idle, controller.sessionState.value)
        assertContains(controller.console.value, ConsoleEntry.Status(Res.string.status_unsupported_protocol, isError = true))
    }

    @Test
    fun logsOfTheSessionAppearInTheConsole() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Log(id = "1", message = "Started", details = null, source = "Game", timestamp = 0L, importance = "HIGH"))
        channel.receive(SessionMessage.Log(id = "2", message = "Ticking", details = null, source = null, timestamp = 0L, importance = "LOW"))
        assertEquals(
            listOf(
                ConsoleEntry.Log(message = "Started", source = "Game", isImportant = true),
                ConsoleEntry.Log(message = "Ticking", source = null, isImportant = false),
            ),
            controller.console.value,
        )
    }

    @Test
    fun aSessionFailureAppearsInTheConsoleInTheSessionsWords() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Failure(message = "No entry point."))
        assertEquals(listOf(ConsoleEntry.SessionFailure("No entry point.")), controller.console.value)
    }

    @Test
    fun problemsAppearWithTheirLocationRelativeToTheProject() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", File(rootDirectory, "src/Ball.kt"), line = 3, column = 5))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Somewhere else", file = null, line = null, column = null))
        assertEquals(
            listOf(
                ConsoleEntry.Problem("Unresolved reference", "src/Ball.kt:3:5"),
                ConsoleEntry.Problem("Somewhere else", null),
            ),
            controller.console.value.filterIsInstance<ConsoleEntry.Problem>(),
        )
    }

    @Test
    fun aFailedBuildWithoutProblemsShowsTheTailOfItsOutput() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        repeat(40) { build.currentRun.onEvent(GradleBuildEvent.Output("line $it", isError = it % 2 == 0)) }
        build.currentRun.finish(isSuccessful = false)
        assertEquals(Res.string.status_build_failed, controller.statuses.last())
        assertEquals((10 until 40).map { "line $it" }, controller.console.value.filterIsInstance<ConsoleEntry.BuildOutput>().map { it.line })
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun aFailedBuildWithProblemsShowsOnlyTheProblems() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        build.currentRun.onEvent(GradleBuildEvent.Output("e: noise", isError = true))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", file = null, line = null, column = null))
        build.currentRun.finish(isSuccessful = false)
        assertTrue(controller.console.value.none { it is ConsoleEntry.BuildOutput })
    }

    @Test
    fun aSessionThatExitsOnItsOwnEndsQuietly() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(Res.string.status_building, Res.string.status_built, Res.string.status_building, Res.string.status_session_ended), controller.statuses)
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun stoppingCancelsTheRunningSession() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        controller.stopSession()
        assertTrue(build.currentRun.isCancelled)
        assertEquals(Res.string.status_session_stopped, controller.statuses.last())
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun theBuildToolCompilesAndReturnsTheProblemsItReported() = runTest {
        val controller = createController()
        val outcome = async { controller.commands.call("build", JsonObject(emptyMap())) }
        runCurrent()
        assertEquals(":${SessionInitScript.CLASSPATH_TASK_NAME}", build.currentRun.taskPath)
        build.currentRun.onEvent(GradleBuildEvent.Problem(message = "Unresolved reference: speed", file = null, line = null, column = null))
        build.currentRun.finish(isSuccessful = false)
        val result = assertIs<CommandOutcome.Success>(outcome.await()).result.jsonObject
        assertEquals(false, result.getValue("isSuccessful").jsonPrimitive.boolean)
        assertEquals(
            listOf("Building the project…", "Unresolved reference: speed", "The build failed."),
            result.getValue("console").jsonArray.map { it.jsonObject.getValue("text").jsonPrimitive.content },
        )
    }

    @Test
    fun thePlayToolPlaysForAWhileOnceTheGameIsUpAndReturnsWhatItLogged() = runTest {
        val controller = createController()
        channel.receive(SessionMessage.Log(id = "0", message = "Before", details = null, source = null, timestamp = 0L, importance = "LOW"))
        val outcome = async { controller.commands.call("play", buildJsonObject { put("seconds", 2) }) }
        runCurrent()
        assertEquals(SessionState.Starting(SessionMode.PLAY), controller.sessionState.value)
        advanceTimeBy(10_000)
        assertFalse(outcome.isCompleted)
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY))
        channel.receive(SessionMessage.Log(id = "1", message = "Ball bounced", details = null, source = "Game", timestamp = 0L, importance = "LOW"))
        runCurrent()
        advanceTimeBy(1_999)
        assertFalse(outcome.isCompleted)
        advanceTimeBy(2)
        val result = assertIs<CommandOutcome.Success>(outcome.await()).result.jsonObject
        assertEquals(true, result.getValue("hasStarted").jsonPrimitive.boolean)
        assertEquals(
            listOf("Building the project…", "Session started.", "Ball bounced", "Session stopped."),
            result.getValue("console").jsonArray.map { it.jsonObject.getValue("text").jsonPrimitive.content },
        )
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun thePlayToolReportsAGameThatNeverStarted() = runTest {
        val controller = createController()
        val outcome = async { controller.commands.call("play", JsonObject(emptyMap())) }
        runCurrent()
        build.currentRun.finish(isSuccessful = false)
        val result = assertIs<CommandOutcome.Success>(outcome.await()).result.jsonObject
        assertEquals(false, result.getValue("hasStarted").jsonPrimitive.boolean)
    }

    @Test
    fun sessionsOfTheFirstProtocolVersionStillRunButCantBePlayedByTools() = runTest {
        val controller = createController()
        val outcome = async { controller.commands.call("start_game", JsonObject(emptyMap())) }
        runCurrent()
        channel.receive(SessionMessage.Hello(protocolVersion = 1, mode = SessionMode.PLAY))
        assertIs<CommandOutcome.Success>(outcome.await())
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.sessionState.value)
        assertTrue(channel.sent.isEmpty())
        val pressed = assertIs<CommandOutcome.Rejected>(controller.commands.call("press_keys", buildJsonObject { put("keys", JsonArray(listOf(JsonPrimitive("LEFT")))) }))
        assertEquals(Res.string.error_game_unsupported, pressed.rejection.reason)
    }

    @Test
    fun theGameToolsPlayTheRunningGameAndBringItToTheFrontFirst() = runTest {
        val controller = createController()
        val pressedWithoutGame = assertIs<CommandOutcome.Rejected>(controller.commands.call("press_keys", buildJsonObject { put("keys", JsonArray(listOf(JsonPrimitive("LEFT")))) }))
        assertEquals(Res.string.error_game_not_running, pressedWithoutGame.rejection.reason)
        val started = async { controller.commands.call("start_game", JsonObject(emptyMap())) }
        runCurrent()
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY))
        assertEquals(true, assertIs<CommandOutcome.Success>(started.await()).result.jsonObject.getValue("hasStarted").jsonPrimitive.boolean)
        channel.answer = { command ->
            if (command is SessionCommand.PressKeys) channel.receive(SessionMessage.Log(id = "1", message = "Paddle moved", details = null, source = "Paddle", timestamp = 0L, importance = "LOW"))
            SessionMessage.CommandDone(command.id)
        }
        val pressed = async { controller.commands.call("press_keys", buildJsonObject { put("keys", JsonArray(listOf(JsonPrimitive("left"), JsonPrimitive("SPACE")))); put("milliseconds", 200) }) }
        runCurrent()
        assertEquals(
            listOf("Paddle moved"),
            assertIs<CommandOutcome.Success>(pressed.await()).result.jsonObject.getValue("console").jsonArray.map { it.jsonObject.getValue("text").jsonPrimitive.content },
        )
        assertEquals(listOf("LEFT", "SPACE"), channel.sent.filterIsInstance<SessionCommand.PressKeys>().single().keys)
        assertEquals(200L, channel.sent.filterIsInstance<SessionCommand.PressKeys>().single().durationInMilliseconds)
        assertIs<SessionCommand.BringToFront>(channel.sent[channel.sent.indexOfFirst { it is SessionCommand.PressKeys } - 1])
        assertEquals(5_000L, channel.sent.filterIsInstance<SessionCommand.AwaitActorsDrawn>().single().timeoutInMilliseconds)
        channel.answer = { command ->
            when (command) {
                is SessionCommand.CaptureScreen -> SessionMessage.Screenshot(command.id, java.util.Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)))
                else -> SessionMessage.CommandDone(command.id)
            }
        }
        val screenshot = assertIs<CommandOutcome.Success>(controller.commands.call("take_screenshot", JsonObject(emptyMap())))
        assertEquals(listOf<Byte>(1, 2, 3), screenshot.image?.toList())
        assertEquals(1280, channel.sent.filterIsInstance<SessionCommand.CaptureScreen>().single().maximumWidth)
        channel.answer = { command -> if (command is SessionCommand.CaptureScreen) SessionMessage.CommandFailed(command.id, "The game hasn't drawn anything yet.") else SessionMessage.CommandDone(command.id) }
        val failed = assertIs<CommandOutcome.Rejected>(controller.commands.call("take_screenshot", JsonObject(emptyMap())))
        assertEquals(Rejection(Res.string.error_game_command_failed, "The game hasn't drawn anything yet."), failed.rejection)
        val stopped = assertIs<CommandOutcome.Success>(controller.commands.call("stop_game", JsonObject(emptyMap())))
        assertEquals(true, stopped.result.jsonObject.getValue("wasRunning").jsonPrimitive.boolean)
        assertEquals(SessionState.Idle, controller.sessionState.value)
    }

    @Test
    fun aGameThatDoesntAnswerIsReported() = runTest {
        val controller = createController()
        val started = async { controller.commands.call("start_game", JsonObject(emptyMap())) }
        runCurrent()
        channel.receive(SessionMessage.Hello(protocolVersion = SessionProtocol.VERSION, mode = SessionMode.PLAY))
        started.await()
        channel.answer = { null }
        val waited = async { controller.commands.call("wait", buildJsonObject { put("milliseconds", 500) }) }
        runCurrent()
        advanceTimeBy(20_000)
        assertEquals(Res.string.error_game_no_answer, assertIs<CommandOutcome.Rejected>(waited.await()).rejection.reason)
    }

    @Test
    fun stoppingWithoutASessionDoesNothing() = runTest {
        val controller = createController()
        controller.stopSession()
        assertTrue(controller.console.value.isEmpty())
    }

    @Test
    fun startingASessionReplacesTheRunningOne() = runTest {
        val controller = createController()
        controller.playThroughGradle()
        controller.playThroughGradle()
        assertTrue(build.runs[1].isCancelled, "the first session")
        assertFalse(build.currentRun.isCancelled)
        assertEquals(SessionState.Starting(SessionMode.PLAY), controller.sessionState.value)
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
    fun savingAnActorTypeRescansTheResourcesSoItsGeneratedCodeShowsUp() = runTest {
        var scanCount = 0
        val controller = createController(scanResources = { scanCount++; emptyMap() }, blockTypeRepository = FakeBlockTypeRepository())
        assertEquals(2, scanCount)
        assertNull(controller.createActorType("Player"))
        assertEquals(3, scanCount)
        assertEquals("Player", controller.blockTypes!!.selectedName.value)
    }

    @Test
    fun aFailedActorTypeSaveIsReportedInTheConsole() = runTest {
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository().apply { isFailing = true })
        controller.clearConsole()
        controller.createActorType("Player")
        assertEquals(listOf(ConsoleEntry.Status(Res.string.status_actor_type_not_saved, isError = true)), controller.console.value)
    }

    @Test
    fun anActorTypeNameThatCantBeUsedIsExplainedAndCreatesNothing() = runTest {
        val controller = createController(blockTypeRepository = FakeBlockTypeRepository())
        assertEquals(Res.string.error_type_name_invalid, controller.createActorType("ball game"))
        assertTrue(controller.blockTypes!!.actorTypes.value.isEmpty())
        assertNull(controller.commands.history.value.nextUndoTitle)
    }

    @Test
    fun aSceneNameThatCantBeUsedIsExplainedAndCreatesNothing() = runTest {
        val controller = createController()
        assertEquals(Res.string.error_scene_name_invalid, controller.createScene("my level"))
        assertNull(controller.commands.history.value.nextUndoTitle)
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
        assertEquals(listOf(ConsoleEntry.Status(Res.string.error_command_actor_type_missing, isError = true, arguments = listOf("Player"))), controller.console.value)
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
        val controller = createController(scanResources = { scanCount++; emptyMap() })
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
            File(root, "gradle.properties").writeText("org.gradle.caching=true\norg.gradle.configuration-cache=true\n")
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
            assertTrue("\"templateVersion\": 7" in File(root, ".kubriko/project.json").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    private companion object {
        const val CREATOR_KUBRIKO_VERSION = "1.0.0"
    }
}
