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
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameLaunchPreparation
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.testing.FakeProjectBuild
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionControllerTest {
    private val channel = FakeSessionChannel()
    private val console = ConsoleLog()

    /**
     * Runs the tasks the controller asks for, which last until each is finished or cancelled.
     */
    private val build = FakeProjectBuild()
    private val tasks = mutableListOf<Pair<String, Map<String, String>>>()

    private fun TestScope.createController(
        runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = { _, _ -> GameProcess.Exit(0, emptyList()) },
    ) = SessionController(
        channel = channel,
        runTask = { taskName, properties ->
            tasks += taskName to properties
            build.run(taskName, emptyList()) {}
        },
        console = console,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        runGame = runGame,
    )

    private val statuses get() = console.lines.value.entries.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun hello(protocolVersion: Int = SessionProtocol.VERSION) = channel.receive(SessionMessage.Hello(protocolVersion = protocolVersion, mode = SessionMode.PLAY))

    @Test
    fun aHelloInTheCurrentProtocolMarksTheSessionAsRunning() = runTest {
        val controller = createController()
        controller.play()
        hello()
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.state.value)
        assertEquals(WorkspaceStatus.SESSION_STARTED, statuses.last())
    }

    @Test
    fun aHelloInANewerProtocolStillRunsTheSession() = runTest {
        val controller = createController()
        controller.play()
        hello(protocolVersion = SessionProtocol.VERSION + 1)
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.state.value)
    }

    @Test
    fun aHelloInAnInvalidProtocolStopsTheSession() = runTest {
        val controller = createController()
        controller.play()
        hello(protocolVersion = 0)
        assertTrue(build.currentRun.isCancelled)
        assertEquals(SessionState.Idle, controller.state.value)
        assertContains(console.lines.value.entries, ConsoleEntry.Status(WorkspaceStatus.UNSUPPORTED_PROTOCOL, isError = true))
    }

    @Test
    fun logsOfTheSessionAppearInTheConsole() = runTest {
        createController()
        channel.receive(SessionMessage.Log(id = "1", message = "Started", details = null, source = "Game", timestamp = 0L, importance = "HIGH"))
        channel.receive(SessionMessage.Log(id = "2", message = "Ticking", details = null, source = null, timestamp = 0L, importance = "LOW"))
        assertEquals(
            listOf(
                ConsoleEntry.Log(message = "Started", source = "Game", isImportant = true),
                ConsoleEntry.Log(message = "Ticking", source = null, isImportant = false),
            ),
            console.lines.value.entries,
        )
    }

    @Test
    fun onlyTheFirstLineTheGameLogsOnceItIsUpCountsAsItsFirst() = runTest {
        val controller = createController()
        fun log(message: String) = channel.receive(SessionMessage.Log(id = message, message = message, details = null, source = null, timestamp = 0L, importance = "LOW"))
        controller.play()
        hello()
        log("First run")
        controller.play()
        log("Left over from the first run")
        hello()
        log("Second run")
        val lines = console.lines.value
        val firstLog = lines.entries[(checkNotNull(lines.firstLogNumber) - lines.firstNumber).toInt()]
        assertEquals(ConsoleEntry.Log(message = "Second run", source = null, isImportant = false), firstLog)
    }

    @Test
    fun aSessionFailureAppearsInTheConsoleInTheSessionsWords() = runTest {
        createController()
        channel.receive(SessionMessage.Failure(message = "No entry point."))
        assertEquals(listOf(ConsoleEntry.SessionFailure("No entry point.")), console.lines.value.entries)
    }

    @Test
    fun aSessionThatExitsOnItsOwnEndsQuietly() = runTest {
        val controller = createController()
        controller.play()
        build.currentRun.finish(isSuccessful = true)
        assertEquals(listOf(WorkspaceStatus.SESSION_ENDED), statuses)
        assertEquals(SessionState.Idle, controller.state.value)
    }

    @Test
    fun stoppingCancelsTheRunningSession() = runTest {
        val controller = createController()
        controller.play()
        controller.stop()
        assertTrue(build.currentRun.isCancelled)
        assertEquals(WorkspaceStatus.SESSION_STOPPED, statuses.last())
        assertEquals(SessionState.Idle, controller.state.value)
    }

    @Test
    fun stoppingWithoutASessionDoesNothing() = runTest {
        val controller = createController()
        controller.stop()
        assertTrue(console.lines.value.entries.isEmpty())
    }

    @Test
    fun startingASessionReplacesTheRunningOne() = runTest {
        val controller = createController()
        controller.play()
        controller.play()
        assertEquals(2, build.runs.size)
        assertTrue(build.runs[0].isCancelled, "the first session")
        assertFalse(build.currentRun.isCancelled)
        assertEquals(SessionState.Starting(SessionMode.PLAY), controller.state.value)
    }

    @Test
    fun restartPlaysFromTheSceneOfTheLastPlay() = runTest {
        val controller = createController()
        controller.play(firstScene = "levels/1-1")
        controller.restart()
        val properties = mapOf("kubrikoCreatorPort" to "4242", "kubrikoCreatorMode" to "play", "kubrikoCreatorScene" to "levels/1-1")
        assertEquals(List(2) { SessionInitScript.SESSION_TASK_NAME to properties }, tasks)
    }

    @Test
    fun restartPreparesTheGameAsTheLastPlayDid() = runTest {
        var preparationCount = 0
        val controller = createController()
        controller.play(
            prepareLaunch = {
                preparationCount++
                if (preparationCount == 1) awaitCancellation()
                GameLaunchPreparation.Failed
            },
        )
        controller.restart()
        assertEquals(2, preparationCount)
        assertEquals(emptyList(), tasks)
    }

    @Test
    fun aGameStoppedOrEndedIsntStartedAgainByARestart() = runTest {
        val controller = createController()
        controller.play()
        controller.stop()
        controller.restart()
        assertEquals(1, tasks.size)
        controller.play()
        build.currentRun.result.complete(true)
        controller.restart()
        assertEquals(2, tasks.size)
        assertEquals(SessionState.Idle, controller.state.value)
    }

    @Test
    fun aGameThatThrowsIOExceptionSaysItDidntStart() = runTest {
        val launch = GameLaunch(File("/java"), "M", File("/game"), emptyList(), listOf(File("/game/classes")))
        val controller = createController(runGame = { _, _ -> throw IOException("x") })
        controller.play(prepareLaunch = { GameLaunchPreparation.Ready(launch) })
        assertEquals(ConsoleEntry.Status(WorkspaceStatus.GAME_NOT_STARTED, isError = true, arguments = listOf("x")), console.lines.value.entries.last())
        assertEquals(SessionState.Idle, controller.state.value)
    }

    @Test
    fun requestsWhileNotPlayingAnswerNotPlaying() = runTest {
        val controller = createController()
        assertEquals(GameAnswer.NotPlaying, controller.request(1_000) { SessionCommand.BringToFront(it) })
        controller.play()
        assertEquals(GameAnswer.NotPlaying, controller.request(1_000) { SessionCommand.BringToFront(it) }, "the game is still starting")
        assertTrue(channel.sent.isEmpty())
    }

    @Test
    fun requestsToAnOlderProtocolAnswerUnsupported() = runTest {
        val controller = createController()
        controller.play()
        hello(protocolVersion = 2)
        assertEquals(GameAnswer.Unsupported, controller.request(1_000, minimumProtocolVersion = 3) { SessionCommand.AwaitActorsDrawn(it, 100) })
        assertTrue(channel.sent.isEmpty())
        assertEquals(GameAnswer.Answered(SessionMessage.CommandDone(1)), controller.request(1_000) { SessionCommand.BringToFront(it) })
    }

    @Test
    fun anUnansweredRequestTimesOut() = runTest {
        val controller = createController()
        controller.play()
        hello()
        channel.answer = { null }
        val answer = async { controller.request(1_000) { SessionCommand.BringToFront(it) } }
        advanceTimeBy(999)
        assertFalse(answer.isCompleted)
        advanceTimeBy(2)
        assertTrue(answer.isCompleted)
        assertEquals(GameAnswer.NoAnswer, answer.await())
    }

    @Test
    fun aRequestTheChannelCantSendAnswersNotPlaying() = runTest {
        val controller = createController()
        controller.play()
        hello()
        channel.isConnected = false
        assertEquals(GameAnswer.NotPlaying, controller.request(1_000) { SessionCommand.BringToFront(it) })
    }

    @Test
    fun closingCancelsTheSessionAndClosesTheChannel() = runTest {
        val controller = createController()
        controller.play()
        controller.close()
        assertTrue(build.currentRun.isCancelled)
        assertTrue(channel.isClosed)
        assertEquals(SessionState.Idle, controller.state.value)
    }

    @Test
    fun aGamePlayedElsewhereIsUpOnceItSaysSoAndStopsWhenCancelled() = runTest {
        val controller = createController()
        var onUp: (() -> Unit)? = null
        controller.playElsewhere(PlayTarget.WEB) { up ->
            onUp = up
            awaitCancellation()
        }
        assertEquals(SessionState.Starting(SessionMode.PLAY, PlayTarget.WEB), controller.state.value)
        checkNotNull(onUp).invoke()
        assertEquals(SessionState.Running(SessionMode.PLAY, PlayTarget.WEB), controller.state.value)
        controller.restart()
        assertTrue(tasks.isEmpty())
        controller.stop()
        assertEquals(SessionState.Idle, controller.state.value)
        assertEquals(listOf(WorkspaceStatus.SESSION_STARTED, WorkspaceStatus.SESSION_STOPPED), statuses)
    }

    @Test
    fun aGameElsewhereThatCantStartSaysWhy() = runTest {
        val controller = createController()
        controller.playElsewhere(PlayTarget.WEB) { throw IOException("No port") }
        assertEquals(SessionState.Idle, controller.state.value)
        assertEquals(listOf(WorkspaceStatus.GAME_NOT_STARTED), statuses)
        controller.playElsewhere(PlayTarget.WEB) { false }
        assertEquals(listOf(WorkspaceStatus.GAME_NOT_STARTED), statuses)
    }

    @Test
    fun playingOnTheDesktopReplacesAGameElsewhere() = runTest {
        val controller = createController()
        var isCancelled = false
        controller.playElsewhere(PlayTarget.WEB) { up ->
            up()
            try {
                awaitCancellation()
            } finally {
                isCancelled = true
            }
        }
        controller.play()
        assertTrue(isCancelled)
        hello()
        assertEquals(SessionState.Running(SessionMode.PLAY), controller.state.value)
    }

    @Test
    fun aRestartAskedForWhileTheGameMovesElsewhereStartsNoDesktopGame() = runTest {
        val controller = createController()
        controller.play()
        hello()
        val starts = tasks.size
        controller.playElsewhere(PlayTarget.WEB) { awaitCancellation() }
        controller.restart()
        assertEquals(starts, tasks.size)
        assertEquals(SessionState.Starting(SessionMode.PLAY, PlayTarget.WEB), controller.state.value)
    }
}
