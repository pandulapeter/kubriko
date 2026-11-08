/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettledAnswerTest {

    private val dispatcher = StandardTestDispatcher()
    private var askCount = 0
    private var reply = CompletableDeferred<Boolean>()
    private val answer = SettledAnswer {
        askCount++
        reply.await()
    }

    @Test
    fun `a background attempt settles the answer once`() {
        val scope = TestScope(dispatcher)

        answer.settleInBackground(scope)
        answer.settleInBackground(scope)
        dispatcher.scheduler.runCurrent()
        reply.complete(false)
        dispatcher.scheduler.runCurrent()
        answer.settleInBackground(scope)
        dispatcher.scheduler.runCurrent()

        assertEquals(false, answer.known)
        assertEquals(1, askCount)
    }

    @Test
    fun `an attempt cancelled before it starts leaves the question open`() {
        val cancelled = TestScope(dispatcher).apply { cancel() }

        answer.settleInBackground(cancelled)
        dispatcher.scheduler.runCurrent()
        assertNull(answer.known)

        reply.complete(true)
        answer.settleInBackground(TestScope(dispatcher))
        dispatcher.scheduler.runCurrent()
        assertEquals(true, answer.known)
    }

    @Test
    fun `an attempt cancelled while it waits leaves the question open`() {
        val scope = TestScope(dispatcher)
        answer.settleInBackground(scope)
        dispatcher.scheduler.runCurrent()

        scope.cancel()
        dispatcher.scheduler.runCurrent()
        assertNull(answer.known)

        reply = CompletableDeferred(true)
        answer.settleInBackground(TestScope(dispatcher))
        dispatcher.scheduler.runCurrent()
        assertEquals(true, answer.known)
        assertEquals(2, askCount)
    }
}
