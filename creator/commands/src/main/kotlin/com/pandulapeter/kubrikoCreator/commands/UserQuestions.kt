/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * The questions an AI asks the user ([AskUser]) that wait for an answer, which the AI panel shows; Kubriko Creator owns the
 * tool, so every agent can ask, not only one with a question tool of its own.
 */
public class UserQuestions {

    /**
     * A question waiting for its answer: one of [answers], or anything the user types.
     *
     * @param origin The assistant's conversation and tool call that asked it, or null for an outside client.
     */
    public data class Question(
        val id: Long,
        val text: String,
        val answers: List<String>,
        val origin: AiStepOrigin? = null,
    )

    private class Pending(
        val question: Question,
        val answer: CompletableDeferred<String>,
    )

    private val ids = AtomicLong()
    private val lock = Any()
    private val waiting = ArrayList<Pending>()
    private val _pending = MutableStateFlow(emptyList<Question>())

    /**
     * The questions waiting for an answer, the oldest first.
     */
    public val pending: StateFlow<List<Question>> = _pending.asStateFlow()

    /**
     * Asks [text] with [answers] to choose from, returning the answer once the user gives one. A caller that is cancelled
     * (its turn stopped) takes its question away.
     */
    public suspend fun ask(text: String, answers: List<String>, origin: AiStepOrigin? = null): String {
        val pending = Pending(Question(ids.incrementAndGet(), text, answers, origin), CompletableDeferred())
        synchronized(lock) {
            waiting += pending
            publish()
        }
        try {
            return pending.answer.await()
        } finally {
            synchronized(lock) {
                waiting.remove(pending)
                publish()
            }
        }
    }

    /**
     * Answers the question [id] with [answer]; nothing happens if it no longer waits.
     */
    public fun answer(id: Long, answer: String) {
        synchronized(lock) { waiting.firstOrNull { it.question.id == id } }?.answer?.complete(answer)
    }

    private fun publish() {
        _pending.value = waiting.map { it.question }
    }
}
