/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Replays the calls a real agent made for each request of the [EvalSet] ([EvalRecordings]) against the tools on the
 * Breakout fixture: a change to a tool's name, input, or validation that would break what models do shows up here
 * without spending tokens. The live run (`EvalSetLiveTest`) records them again.
 */
class EvalSetTest {

    @Test
    fun everyRecordedCallIsAcceptedAndDoesWhatWasAsked() = runTest {
        EvalSet.requests.forEach { request ->
            val calls = EvalRecordings.of(request)
            assertTrue(calls.isNotEmpty(), "\"${request.text}\" has no recording: run EvalSetLiveTest with KUBRIKO_CREATOR_EVAL_RECORD=true.")
            val (executor, context) = breakout()
            calls.forEach { call ->
                val outcome = executor.call(call.tool, call.arguments)
                assertIs<CommandOutcome.Success>(outcome, "\"${request.text}\": ${call.tool} was rejected with $outcome")
            }
            request.check(context)
        }
    }

    @Test
    fun recordingsReadBackAsTheyWereWritten() {
        val calls = listOf(
            RecordedCall("read_actor_type", buildJsonObject { put("actorType", JsonPrimitive("Ball")) }),
            RecordedCall("create_page", buildJsonObject { put("text", JsonPrimitive("# Breakout\n\nLine \"two\".")) }),
        )
        assertEquals(calls, EvalRecordings.decode(EvalRecordings.encode(calls)))
        assertEquals(calls.size + 2, EvalRecordings.encode(calls).trim().lines().size, "Not one call per line.")
    }

    @OptIn(EditorMutation::class)
    private fun TestScope.breakout(): Pair<CommandExecutor, CommandContext> {
        val context = commandContext(*BreakoutFixture.types.toTypedArray())
        context.scenes.write("level-1", BreakoutFixture.level)
        return CommandExecutor(context, testScope()) to context
    }
}
