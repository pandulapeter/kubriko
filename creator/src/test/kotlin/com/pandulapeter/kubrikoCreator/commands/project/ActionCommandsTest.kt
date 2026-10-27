/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_play_duration_invalid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ActionCommandsTest {

    @Test
    fun buildingReturnsWhetherItSucceededWithTheProblems() = runTest {
        val runner = FakeProjectRunner.failing(ConsoleEntry.Problem("Unresolved reference: speed", "src/Ball.kt:3:5"))
        val executor = CommandExecutor(commandContext(runner = runner), testScope())
        assertEquals(
            Json.parseToJsonElement("""{"isSuccessful":false,"console":[{"kind":"problem","text":"Unresolved reference: speed","location":"src/Ball.kt:3:5"}]}"""),
            assertIs<CommandOutcome.Success>(executor.call("build", JsonObject(emptyMap()))).result,
        )
        assertEquals(listOf("build"), runner.calls)
        assertEquals(CommandHistory(), executor.history.value)
    }

    @Test
    fun playingLastsTheGivenSecondsWithinLimits() = runTest {
        val runner = FakeProjectRunner()
        val executor = CommandExecutor(commandContext(runner = runner), testScope())
        assertEquals(
            Json.parseToJsonElement("""{"hasStarted":true,"console":[]}"""),
            assertIs<CommandOutcome.Success>(executor.call("play", JsonObject(emptyMap()))).result,
        )
        executor.call("play", buildJsonObject { put("seconds", 60) })
        assertEquals(listOf("play 5000", "play 60000"), runner.calls)
        listOf(0, 61).forEach { seconds ->
            assertEquals(
                CommandOutcome.Rejected(Rejection(Res.string.error_command_play_duration_invalid, "60")),
                executor.call("play", buildJsonObject { put("seconds", seconds) }),
            )
        }
        assertEquals(2, runner.calls.size)
    }
}
