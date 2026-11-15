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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * Has the real Claude Agent answer every request of the [EvalSet] on a fresh project of its own (the Breakout fixture or an empty one), as a user who
 * agrees with everything would (approving its plans, taking its first suggestion), and checks what each project became.
 * Costs requests, so it runs only through [liveTest]'s switch; with `KUBRIKO_CREATOR_EVAL_RECORD=true` too, it writes
 * the calls of each request that passed as its recording, which `EvalSetTest` replays for free from then on.
 * `KUBRIKO_CREATOR_EVAL_REQUESTS` (IDs separated by commas) asks only those requests, to record a new or changed one
 * without spending requests on the rest.
 */
class EvalSetLiveTest {
    private val isRecording = System.getenv("KUBRIKO_CREATOR_EVAL_RECORD") == "true"
    private val requests = System.getenv("KUBRIKO_CREATOR_EVAL_REQUESTS")?.split(',')?.map { it.trim() }?.toSet()
        ?.let { ids -> EvalSet.requests.filter { it.id in ids }.also { assertEquals(ids, it.map { request -> request.id }.toSet(), "Not every request asked for exists.") } }
        ?: EvalSet.requests

    @Test
    fun theRealAgentDoesWhatEveryRequestAsks() = liveTest(timeout = (REQUEST_TIMEOUT * 2 + SESSION_TIMEOUT) * requests.size) {
        val failures = requests.mapNotNull { request ->
            withLiveSession(isBlank = request.isBlank) { session ->
                val outcome = try {
                    answer(request.text, session)
                    Result.success(request.check(session.context))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    Result.failure(throwable)
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
        /**
         * How long a turn may take: each request takes one, and a second when it ends on a plan to approve.
         */
        val REQUEST_TIMEOUT = 20.minutes

        /**
         * How long starting and closing a session may take besides its turns.
         */
        val SESSION_TIMEOUT = 1.minutes
    }
}
