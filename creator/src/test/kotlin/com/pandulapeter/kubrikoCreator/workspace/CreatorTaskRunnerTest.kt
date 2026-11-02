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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy
import com.pandulapeter.kubrikoCreator.project.ExpectDeclarations
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
import kubriko.creator.generated.resources.error_not_a_gradle_project
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_built
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorTaskRunnerTest {
    private val rootDirectory = File("/projects/game")
    private val build = FakeProjectBuild()
    private val console = ConsoleLog()

    private fun TestScope.createRunner(
        rootDirectory: File = this@CreatorTaskRunnerTest.rootDirectory,
        findDesktopModulePath: suspend () -> String = { ":" },
        hasExpectDeclarations: suspend () -> Boolean = { false },
        awaitWrites: suspend () -> Unit = {},
        currentTimeMillis: () -> Long = { 0L },
    ): CreatorTaskRunner {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return CreatorTaskRunner(
            rootDirectory = rootDirectory,
            build = build,
            desktopModulePath = DesktopModuleLookup(CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]) + dispatcher), dispatcher, findDesktopModulePath),
            incrementalCompilation = IncrementalCompilationPolicy(hasExpectDeclarations),
            console = console,
            awaitWrites = awaitWrites,
            currentTimeMillis = currentTimeMillis,
        )
    }

    private fun TestScope.start(runner: CreatorTaskRunner, taskName: String = SessionInitScript.CLASSPATH_TASK_NAME, properties: Map<String, String> = emptyMap()) =
        backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { runner.run(taskName, properties) }

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    @Test
    fun commonCodeCompilesIncrementallyUnlessTheProjectDeclaresExpectsAndAFailedBuildIsFollowedByACleanOne() = runTest {
        val root = testDirectory()
        val declarations = ExpectDeclarations(root)
        val runner = createRunner(rootDirectory = root, hasExpectDeclarations = { declarations.any() })
        val property = "-P${SessionInitScript.INCREMENTAL_COMMON_CODE_PROPERTY}"
        start(runner)
        assertContains(build.currentRun.arguments, "$property=true")
        assertFalse("--rerun-tasks" in build.currentRun.arguments)
        build.currentRun.finish(isSuccessful = false)
        start(runner)
        assertContains(build.currentRun.arguments, "$property=true")
        assertContains(build.currentRun.arguments, "--rerun-tasks")
        build.currentRun.finish(isSuccessful = true)
        start(runner)
        assertFalse("--rerun-tasks" in build.currentRun.arguments)
        build.currentRun.finish(isSuccessful = true)
        root.resolve("src/commonMain/kotlin/Platform.kt").apply { parentFile.mkdirs() }.writeText("expect fun name(): String\n")
        start(runner)
        assertContains(build.currentRun.arguments, "$property=false")
    }

    @Test
    fun aProjectWithoutADesktopModuleSaysWhyItCantBuild() = runTest {
        val result = start(createRunner(findDesktopModulePath = { throw ProjectInspectionException(Res.string.error_no_desktop_module) }))
        assertFalse(result.await())
        assertEquals(
            listOf(ConsoleEntry.Status(Res.string.status_building), ConsoleEntry.Status(Res.string.error_no_desktop_module, isError = true)),
            console.entries.value,
        )
        assertTrue(build.runs.isEmpty())
    }

    @Test
    fun aLookupFailingWithAnUnexpectedExceptionSaysItIsNotAGradleProject() = runTest {
        val result = start(createRunner(findDesktopModulePath = { throw IllegalStateException("Broken.") }))
        assertFalse(result.await())
        assertEquals(
            listOf(ConsoleEntry.Status(Res.string.status_building), ConsoleEntry.Status(Res.string.error_not_a_gradle_project, isError = true)),
            console.entries.value,
        )
        assertTrue(build.runs.isEmpty())
    }

    @Test
    fun cancellingDuringTheLookupIsNotReportedAsAFailure() = runTest {
        val lookup = CompletableDeferred<String>()
        val result = start(createRunner(findDesktopModulePath = { lookup.await() }))
        result.cancel()
        assertTrue(result.isCancelled)
        assertEquals(listOf(ConsoleEntry.Status(Res.string.status_building)), console.entries.value)
        assertTrue(build.runs.isEmpty())
    }

    @Test
    fun aBuildWaitsForTheChangesThatAreStillBeingWritten() = runTest {
        val writes = CompletableDeferred<Unit>()
        start(createRunner(awaitWrites = { writes.await() }))
        assertTrue(build.runs.isEmpty())
        writes.complete(Unit)
        assertEquals(1, build.runs.size)
    }

    @Test
    fun aSessionWaitsForTheClasspathBuildBeforeIt() = runTest {
        val runner = createRunner()
        start(runner)
        start(runner, SessionInitScript.SESSION_TASK_NAME)
        assertEquals(1, build.runs.size, "the session's build waits for the classpath build")
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(":${SessionInitScript.CLASSPATH_TASK_NAME}", ":${SessionInitScript.SESSION_TASK_NAME}"), build.runs.map { it.taskPath })
    }

    @Test
    fun twoClasspathBuildsDontOverlap() = runTest {
        val runner = createRunner()
        start(runner)
        start(runner)
        assertEquals(1, build.runs.size)
        build.currentRun.finish(isSuccessful = true)
        assertEquals(2, build.runs.size)
    }

    @Test
    fun problemsAppearWithTheirLocationRelativeToTheProject() = runTest {
        start(createRunner())
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", File(rootDirectory, "src/Ball.kt"), line = 3, column = 5))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Somewhere else", file = null, line = null, column = null))
        assertEquals(
            listOf(
                ConsoleEntry.Problem("Unresolved reference", "src/Ball.kt:3:5"),
                ConsoleEntry.Problem("Somewhere else", null),
            ),
            console.entries.value.filterIsInstance<ConsoleEntry.Problem>(),
        )
    }

    @Test
    fun aFailedBuildWithoutProblemsShowsTheTailOfItsOutput() = runTest {
        val result = start(createRunner())
        repeat(40) { build.currentRun.onEvent(GradleBuildEvent.Output("line $it", isError = it % 2 == 0)) }
        build.currentRun.finish(isSuccessful = false)
        assertFalse(result.await())
        assertEquals(Res.string.status_build_failed, statuses.last())
        assertEquals((10 until 40).map { "line $it" }, console.entries.value.filterIsInstance<ConsoleEntry.BuildOutput>().map { it.line })
    }

    @Test
    fun onlyTheLast30OutputLinesAreShown() = runTest {
        start(createRunner())
        (1..35).forEach { build.currentRun.onEvent(GradleBuildEvent.Output("line $it", isError = false)) }
        build.currentRun.finish(isSuccessful = false)
        assertEquals(
            listOf(ConsoleEntry.Status(Res.string.status_building), ConsoleEntry.Status(Res.string.status_build_failed, isError = true)) +
                (6..35).map { ConsoleEntry.BuildOutput("line $it") },
            console.entries.value,
        )
    }

    @Test
    fun aFailedBuildWithProblemsShowsOnlyTheProblems() = runTest {
        start(createRunner())
        build.currentRun.onEvent(GradleBuildEvent.Output("e: noise", isError = true))
        build.currentRun.onEvent(GradleBuildEvent.Problem("Unresolved reference", file = null, line = null, column = null))
        build.currentRun.finish(isSuccessful = false)
        assertTrue(console.entries.value.none { it is ConsoleEntry.BuildOutput })
    }

    @Test
    fun aSuccessfulClasspathBuildReportsItsTime() = runTest {
        val times = ArrayDeque(listOf(1_000L, 2_500L))
        val result = start(createRunner(currentTimeMillis = { times.removeFirst() }))
        build.currentRun.finish(isSuccessful = true)
        assertTrue(result.await())
        assertEquals(
            listOf(ConsoleEntry.Status(Res.string.status_building), ConsoleEntry.Status(Res.string.status_built, arguments = listOf("1.5"))),
            console.entries.value,
        )
    }

    @Test
    fun theSessionTaskOfANestedModuleIsAddressedByItsPath() = runTest {
        start(createRunner(findDesktopModulePath = { ":app:desktop" }), SessionInitScript.SESSION_TASK_NAME)
        assertEquals(":app:desktop:${SessionInitScript.SESSION_TASK_NAME}", build.currentRun.taskPath)
    }

    @Test
    fun warmingUpWithoutADesktopModuleDoesNothing() = runTest {
        val runner = createRunner(findDesktopModulePath = { throw ProjectInspectionException(Res.string.error_no_desktop_module) })
        runner.warmUpCompiler()
        assertTrue(build.runs.isEmpty())
        assertTrue(console.entries.value.isEmpty())
    }
}
