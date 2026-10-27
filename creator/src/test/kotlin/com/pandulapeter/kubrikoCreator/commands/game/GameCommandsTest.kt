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
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_game_duration_invalid
import kubriko.creator.generated.resources.error_game_key_unknown
import kubriko.creator.generated.resources.error_game_keys_count
import kubriko.creator.generated.resources.error_game_not_running
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
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_keys_count, "10")), executor.call("press_keys", keys()))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_keys_count, "10")), executor.call("press_keys", keys(*Array(11) { "A" })))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_key_unknown, "JUMP")), executor.call("press_keys", keys("LEFT", "JUMP")))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_duration_invalid, "10000")), executor.call("press_keys", keys("LEFT", milliseconds = 0)))
        assertTrue(runner.calls.isEmpty())
        assertIs<CommandOutcome.Success>(executor.call("press_keys", keys("left", "space")))
        assertEquals(listOf("press LEFT+SPACE 150"), runner.calls)
    }

    @Test
    fun waitingIsLimitedAndScreenshotsComeBackAsImages() = runTest {
        val executor = executor()
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_duration_invalid, "30000")), executor.call("wait", buildJsonObject { put("milliseconds", 30_001) }))
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
            assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_game_not_running)), executor.call(name, arguments))
        }
    }

    @Test
    fun startingAndStoppingReportWhatHappened() = runTest {
        val executor = executor()
        assertEquals("""{"hasStarted":true,"console":[]}""", assertIs<CommandOutcome.Success>(executor.call("start_game", JsonObject(emptyMap()))).result.toString())
        assertEquals("""{"wasRunning":true,"console":[]}""", assertIs<CommandOutcome.Success>(executor.call("stop_game", JsonObject(emptyMap()))).result.toString())
        assertEquals(listOf("start", "stop"), runner.calls)
    }
}
