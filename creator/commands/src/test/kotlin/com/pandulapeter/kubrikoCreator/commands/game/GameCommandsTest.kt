/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.game

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameCommandsTest {
    private val runner = FakeProjectRunner()

    private fun TestScope.executor() = CommandExecutor(commandContext(runner = runner), testScope())

    private fun keys(vararg names: String, milliseconds: Int? = null) = buildJsonObject {
        put("keys", JsonArray(names.map(::JsonPrimitive)))
        milliseconds?.let { put("milliseconds", it) }
    }

    @Test
    fun keyPressesAreCheckedBeforeTheyReachTheGame() = runTest {
        val executor = executor()
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEYS_COUNT, "10")), executor.call("press_keys", keys()))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEYS_COUNT, "10")), executor.call("press_keys", keys(*Array(11) { "A" })))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEY_UNKNOWN, "JUMP")), executor.call("press_keys", keys("LEFT", "JUMP")))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_DURATION_INVALID, "10000")), executor.call("press_keys", keys("LEFT", milliseconds = 0)))
        assertTrue(runner.calls.isEmpty())
        assertIs<CommandOutcome.Success>(executor.call("press_keys", keys("left", "space")))
        assertEquals(listOf("press LEFT+SPACE 150"), runner.calls)
    }

    @Test
    fun stepsArePlayedInOrderAndAScreenshotIsTakenOnceTheKeysAreReleased() = runTest {
        val executor = executor()
        val steps = buildJsonObject {
            put("steps", Json.parseToJsonElement("""[{"keys": ["UP"], "milliseconds": 1800}, {"keys": ["down", "SPACE"]}]"""))
            put("screenshotWidth", 640)
        }
        runner.step = GameStep.Done(console = emptyList(), png = byteArrayOf(9))
        val outcome = assertIs<CommandOutcome.Success>(executor.call("press_keys", steps))
        assertEquals(listOf<Byte>(9), outcome.image?.toList())
        assertEquals(listOf("press UP 1800", "press DOWN+SPACE 150", "screenshot"), runner.calls)
    }

    @Test
    fun stepsAreCheckedBeforeAnyReachesTheGame() = runTest {
        val executor = executor()
        val both = buildJsonObject {
            put("keys", JsonArray(listOf(JsonPrimitive("A"))))
            put("steps", Json.parseToJsonElement("""[{"keys": ["B"]}]"""))
        }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEYS_OR_STEPS)), executor.call("press_keys", both))
        val tooMany = buildJsonObject { put("steps", JsonArray(List(21) { Json.parseToJsonElement("""{"keys": ["A"]}""") })) }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_STEPS_COUNT, "20")), executor.call("press_keys", tooMany))
        val tooLong = buildJsonObject { put("steps", Json.parseToJsonElement("""[{"keys": ["A"], "milliseconds": 10000}, {"keys": ["A"], "milliseconds": 10000}, {"keys": ["A"], "milliseconds": 10000}, {"keys": ["A"], "milliseconds": 1}]""")) }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_DURATION_INVALID, "30000")), executor.call("press_keys", tooLong))
        val unknownLater = buildJsonObject { put("steps", Json.parseToJsonElement("""[{"keys": ["A"]}, {"keys": ["JUMP"]}]""")) }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEY_UNKNOWN, "JUMP")), executor.call("press_keys", unknownLater))
        assertTrue(runner.calls.isEmpty())
    }

    @Test
    fun aStepTheGameCantPlayEndsTheRestWithItsReason() = runTest {
        runner.step = GameStep.NoAnswer
        val executor = executor()
        val steps = buildJsonObject { put("steps", Json.parseToJsonElement("""[{"keys": ["A"]}, {"keys": ["B"]}]""")); put("screenshotWidth", 640) }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_NO_ANSWER)), executor.call("press_keys", steps))
        assertEquals(listOf("press A 150"), runner.calls)
    }

    @Test
    fun waitingIsLimitedAndScreenshotsComeBackAsImages() = runTest {
        val executor = executor()
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_DURATION_INVALID, "30000")), executor.call("wait", buildJsonObject { put("milliseconds", 30_001) }))
        executor.call("wait", buildJsonObject { put("milliseconds", 2_000) })
        runner.step = GameStep.Done(console = emptyList(), png = byteArrayOf(9))
        assertEquals(listOf<Byte>(9), assertIs<CommandOutcome.Success>(executor.call("take_screenshot", JsonObject(emptyMap()))).image?.toList())
        assertEquals(listOf("wait 2000", "screenshot"), runner.calls)
    }

    @Test
    fun withoutARunningGameTheToolsSaySo() = runTest {
        runner.step = GameStep.NotPlaying
        val executor = executor()
        listOf("take_screenshot" to JsonObject(emptyMap()), "wait" to buildJsonObject { put("milliseconds", 10) }, "press_keys" to keys("A")).forEach { (name, arguments) ->
            assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_NOT_RUNNING)), executor.call(name, arguments))
        }
    }

    @Test
    fun startingAndStoppingReportWhatHappened() = runTest {
        val executor = executor()
        assertEquals("""{"hasStarted":true,"console":[]}""", assertIs<CommandOutcome.Success>(executor.call("start_game", JsonObject(emptyMap()))).result.toString())
        assertEquals("""{"wasRunning":true,"console":[]}""", assertIs<CommandOutcome.Success>(executor.call("stop_game", JsonObject(emptyMap()))).result.toString())
        assertEquals(listOf("start", "stop"), runner.calls)
    }

    @OptIn(EditorMutation::class)
    @Test
    fun aGameStartsInTheSceneItIsGivenWhenThereIsOne() = runTest {
        val context = commandContext(runner = runner)
        context.scenes.write("levels/bonus", "[]")
        val executor = CommandExecutor(context, testScope())
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.SCENE_MISSING, "levels/other")), executor.call("start_game", buildJsonObject { put("scene", "levels/other") }))
        assertTrue(runner.calls.isEmpty())
        assertIs<CommandOutcome.Success>(executor.call("start_game", buildJsonObject { put("scene", "levels/bonus") }))
        assertEquals(listOf("start levels/bonus"), runner.calls)
    }

    @Test
    fun theGameIsPlayedWhereTheTargetSaysAndOnlyWhereItCanBe() = runTest {
        val other = PlayTarget(id = "elsewhere", kind = PlayTarget.Kind.DESKTOP, name = "Elsewhere")
        runner.targets = PlayTargets(available = listOf(PlayTarget.DESKTOP, other), chosen = other)
        val executor = executor()
        assertEquals(
            CommandOutcome.Rejected(Rejection(CommandProblem.PLAY_TARGET_MISSING, "nowhere", "desktop, elsewhere")),
            executor.call("start_game", buildJsonObject { put("target", "nowhere") }),
        )
        assertEquals(
            CommandOutcome.Rejected(Rejection(CommandProblem.PLAY_TARGET_MISSING, "nowhere", "desktop, elsewhere")),
            executor.call("play", buildJsonObject { put("target", "nowhere") }),
        )
        assertEquals(listOf("targets", "targets"), runner.calls)
        runner.calls.clear()
        assertIs<CommandOutcome.Success>(executor.call("start_game", buildJsonObject { put("target", "elsewhere") }))
        assertIs<CommandOutcome.Success>(executor.call("start_game", buildJsonObject { put("target", "desktop") }))
        assertIs<CommandOutcome.Success>(executor.call("play", buildJsonObject { put("target", "elsewhere") }))
        assertEquals(listOf("targets", "start on elsewhere", "start", "targets", "play 5000 on elsewhere"), runner.calls)
    }

    @Test
    fun whereTheGameCanBePlayedIsListedWithTheUsersChoice() = runTest {
        val other = PlayTarget(id = "elsewhere", kind = PlayTarget.Kind.DESKTOP, name = "Elsewhere")
        runner.targets = PlayTargets(available = listOf(PlayTarget.DESKTOP, other), chosen = other)
        assertEquals(
            """{"targets":[{"id":"desktop","kind":"desktop"},{"id":"elsewhere","kind":"desktop","name":"Elsewhere"}],"chosenByUser":"elsewhere"}""",
            assertIs<CommandOutcome.Success>(executor().call("list_play_targets", JsonObject(emptyMap()))).result.toString(),
        )
    }

    @Test
    fun theToolsSayWhenTheGameRunsWhereTheyCantReach() = runTest {
        runner.step = GameStep.Elsewhere(PlayTarget.WEB)
        val executor = executor()
        listOf("take_screenshot" to JsonObject(emptyMap()), "wait" to buildJsonObject { put("milliseconds", 10) }, "press_keys" to keys("A")).forEach { (name, arguments) ->
            assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.GAME_NOT_ON_DESKTOP, "web")), executor.call(name, arguments))
        }
    }
}
