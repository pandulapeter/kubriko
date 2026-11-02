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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionCommand
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.ProjectRunner
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceRunnerTest {
    private val channel = FakeSessionChannel()
    private val console = ConsoleLog()
    private val classpathFile = File("/settings/session/build-classpath.txt")

    /**
     * Runs the tasks the runner and its sessions ask for, which last until each is finished or cancelled.
     */
    private val build = FakeProjectBuild()
    private val tasks = mutableListOf<Pair<String, Map<String, String>>>()
    private val runTask: suspend (String, Map<String, String>) -> Boolean = { taskName, properties ->
        tasks += taskName to properties
        build.run(taskName, emptyList()) {}
    }
    private lateinit var sessions: SessionController

    private fun TestScope.createRunner(): WorkspaceRunner {
        sessions = SessionController(
            channel = channel,
            runTask = runTask,
            console = console,
            scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        )
        return WorkspaceRunner(sessions = sessions, runTask = runTask, console = console, classpathFile = classpathFile)
    }

    private fun hello(protocolVersion: Int = SessionProtocol.VERSION) = channel.receive(SessionMessage.Hello(protocolVersion = protocolVersion, mode = SessionMode.PLAY))

    private fun log(message: String) = SessionMessage.Log(id = message, message = message, details = null, source = null, timestamp = 0L, importance = "LOW")

    private suspend fun TestScope.startGame(runner: WorkspaceRunner, protocolVersion: Int = SessionProtocol.VERSION): ProjectRunner.RunResult {
        val started = async { runner.startGame(firstScene = null) }
        runCurrent()
        hello(protocolVersion)
        return started.await()
    }

    @Test
    fun theBuildToolCompilesAndReturnsTheProblemsItReported() = runTest {
        val runner = createRunner()
        val result = async { runner.build() }
        runCurrent()
        assertEquals(
            listOf(SessionInitScript.CLASSPATH_TASK_NAME to mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath)),
            tasks,
        )
        console.append(ConsoleEntry.Problem("Unresolved reference: speed", null))
        build.currentRun.finish(isSuccessful = false)
        assertEquals(ProjectRunner.RunResult(isSuccessful = false, console = listOf(ConsoleEntry.Problem("Unresolved reference: speed", null))), result.await())
    }

    @Test
    fun thePlayToolPlaysForAWhileOnceTheGameIsUpAndReturnsWhatItLogged() = runTest {
        val runner = createRunner()
        channel.receive(log("Before"))
        val result = async { runner.play(durationInMilliseconds = 2_000) }
        runCurrent()
        assertEquals(SessionState.Starting(SessionMode.PLAY), sessions.state.value)
        advanceTimeBy(10_000)
        assertFalse(result.isCompleted)
        hello()
        channel.receive(log("Ball bounced"))
        runCurrent()
        advanceTimeBy(1_999)
        assertFalse(result.isCompleted)
        advanceTimeBy(2)
        assertEquals(
            ProjectRunner.RunResult(
                isSuccessful = true,
                console = listOf(
                    ConsoleEntry.Status(Res.string.status_session_started),
                    ConsoleEntry.Log(message = "Ball bounced", source = null, isImportant = false),
                    ConsoleEntry.Status(Res.string.status_session_stopped),
                ),
            ),
            result.await(),
        )
        assertEquals(SessionState.Idle, sessions.state.value)
    }

    @Test
    fun thePlayToolReportsAGameThatNeverStarted() = runTest {
        val runner = createRunner()
        val result = async { runner.play(durationInMilliseconds = 2_000) }
        runCurrent()
        build.currentRun.finish(isSuccessful = false)
        assertFalse(result.await().isSuccessful)
    }

    @Test
    fun sessionsOfTheFirstProtocolVersionStillRunButCantBePlayedByTools() = runTest {
        val runner = createRunner()
        assertTrue(startGame(runner, protocolVersion = 1).isSuccessful)
        assertEquals(SessionState.Running(SessionMode.PLAY), sessions.state.value)
        assertTrue(channel.sent.isEmpty())
        assertEquals(GameStep.Unsupported, runner.pressKeys(listOf("LEFT"), durationInMilliseconds = 100))
    }

    @Test
    fun theGameToolsPlayTheRunningGameAndBringItToTheFrontFirst() = runTest {
        val runner = createRunner()
        assertEquals(GameStep.NotPlaying, runner.pressKeys(listOf("LEFT"), durationInMilliseconds = 100))
        assertTrue(startGame(runner).isSuccessful)
        channel.answer = { command ->
            if (command is SessionCommand.PressKeys) channel.receive(SessionMessage.Log(id = "1", message = "Paddle moved", details = null, source = "Paddle", timestamp = 0L, importance = "LOW"))
            SessionMessage.CommandDone(command.id)
        }
        val pressed = async { runner.pressKeys(listOf("LEFT", "SPACE"), durationInMilliseconds = 200) }
        runCurrent()
        assertEquals(listOf(ConsoleEntry.Log(message = "Paddle moved", source = "Paddle", isImportant = false)), assertIs<GameStep.Done>(pressed.await()).console)
        assertEquals(listOf("LEFT", "SPACE"), channel.sent.filterIsInstance<SessionCommand.PressKeys>().single().keys)
        assertEquals(200L, channel.sent.filterIsInstance<SessionCommand.PressKeys>().single().durationInMilliseconds)
        assertIs<SessionCommand.BringToFront>(channel.sent[channel.sent.indexOfFirst { it is SessionCommand.PressKeys } - 1])
        assertEquals(5_000L, channel.sent.filterIsInstance<SessionCommand.AwaitActorsDrawn>().single().timeoutInMilliseconds)
        channel.answer = { command ->
            when (command) {
                is SessionCommand.CaptureScreen -> SessionMessage.Screenshot(command.id, Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)))
                else -> SessionMessage.CommandDone(command.id)
            }
        }
        assertEquals(listOf<Byte>(1, 2, 3), assertIs<GameStep.Done>(runner.captureScreen()).png?.toList())
        assertEquals(1280, channel.sent.filterIsInstance<SessionCommand.CaptureScreen>().single().maximumWidth)
        channel.answer = { command -> if (command is SessionCommand.CaptureScreen) SessionMessage.CommandFailed(command.id, "The game hasn't drawn anything yet.") else SessionMessage.CommandDone(command.id) }
        assertEquals(GameStep.Failed("The game hasn't drawn anything yet."), runner.captureScreen())
        assertTrue(runner.stopGame().isSuccessful, "the game was running")
        assertEquals(SessionState.Idle, sessions.state.value)
    }

    @Test
    fun aGameThatDoesntAnswerIsReported() = runTest {
        val runner = createRunner()
        startGame(runner)
        channel.answer = { null }
        val waited = async { runner.wait(durationInMilliseconds = 500) }
        runCurrent()
        advanceTimeBy(20_000)
        assertEquals(GameStep.NoAnswer, waited.await())
    }

    @Test
    fun aStepWhoseBringToFrontFailedStopsThere() = runTest {
        val runner = createRunner()
        startGame(runner)
        channel.sent.clear()
        channel.answer = { command -> SessionMessage.CommandFailed(command.id, "The window is gone.") }
        assertEquals(GameStep.Failed("The window is gone."), runner.pressKeys(listOf("LEFT"), durationInMilliseconds = 100))
        assertIs<SessionCommand.BringToFront>(channel.sent.single())
    }

    @Test
    fun aScreenshotIsDecodedFromBase64() = runTest {
        val runner = createRunner()
        startGame(runner)
        channel.answer = { command ->
            when (command) {
                is SessionCommand.CaptureScreen -> SessionMessage.Screenshot(command.id, "AQID")
                else -> SessionMessage.CommandDone(command.id)
            }
        }
        assertEquals(listOf<Byte>(1, 2, 3), assertIs<GameStep.Done>(runner.captureScreen()).png?.toList())
    }

    @Test
    fun stopGameWithoutAGameReportsFailure() = runTest {
        assertEquals(ProjectRunner.RunResult(isSuccessful = false, console = emptyList()), createRunner().stopGame())
    }
}
