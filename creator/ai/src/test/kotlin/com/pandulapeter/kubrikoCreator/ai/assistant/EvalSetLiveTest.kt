/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.context.EvalRecordings
import com.pandulapeter.kubrikoCreator.context.EvalSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * Has the real Claude Agent answer every request of the [EvalSet] on a fresh Breakout fixture of its own, as a user who
 * agrees with everything would (approving its plans, taking its first suggestion), and checks what each project became.
 * Costs requests, so it runs only through [liveTest]'s switch; with `KUBRIKO_CREATOR_EVAL_RECORD=true` too, it writes
 * the calls of each request that passed as its recording, which `EvalSetTest` replays for free from then on.
 */
class EvalSetLiveTest {
    private val isRecording = System.getenv("KUBRIKO_CREATOR_EVAL_RECORD") == "true"

    @Test
    fun theRealAgentDoesWhatEveryRequestAsks() = liveTest(timeout = REQUEST_TIMEOUT * EvalSet.requests.size) {
        val failures = EvalSet.requests.mapNotNull { request ->
            withLiveSession { session ->
                val outcome = runCatching {
                    answer(request.text, session)
                    request.check(session.context)
                }
                val calls = session.calls.toList()
                println("\"${request.text}\": ${if (outcome.isSuccess) "passed" else "failed"} with ${calls.size} calls")
                calls.forEach { println("  ${if (it.isError) "rejected" else "accepted"} ${it.call.tool} ${it.call.arguments}") }
                if (outcome.isSuccess && isRecording) EvalRecordings.write(request, calls.filterNot { it.isError }.map { it.call })
                outcome.exceptionOrNull()?.let { "\"${request.text}\": ${it.message}" }
            }
        }
        assertEquals(emptyList(), failures)
    }

    private suspend fun TestScope.answer(text: String, session: LiveSession) {
        val questions = session.context.questions
        val answering = launch(Dispatchers.Default) {
            val answered = HashSet<Long>()
            questions.pending.collect { pending ->
                pending.filter { answered.add(it.id) }.forEach { questions.answer(it.id, it.answers.firstOrNull() ?: "Your call.") }
            }
        }
        try {
            val chat = session.chat()
            chat.ask(text, timeout = REQUEST_TIMEOUT)
            if (chat.transcript.value.isPlanAwaitingApproval) chat.ask("The plan is approved: go ahead.", timeout = REQUEST_TIMEOUT)
        } finally {
            answering.cancel()
        }
    }

    private companion object {
        val REQUEST_TIMEOUT = 10.minutes
    }
}
