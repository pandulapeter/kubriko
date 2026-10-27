/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LogForwarderTest {
    private val sent = mutableListOf<SessionMessage.Log>()

    @BeforeTest
    fun setUp() = Logger.clearLogs()

    @AfterTest
    fun tearDown() = Logger.clearLogs()

    @Test
    fun entriesLoggedBeforeStartingAreSentOldestFirst() = runTest {
        Logger.log("first", source = "Test", importance = Logger.Importance.LOW)
        Logger.log("second")
        LogForwarder(send = { sent += it as SessionMessage.Log }, scope = backgroundScope.withUnconfinedDispatcher()).start()
        assertEquals(listOf("first", "second"), sent.map { it.message })
        assertEquals(SessionMessage.Log(sent[0].id, "first", null, "Test", sent[0].timestamp, "LOW"), sent[0])
    }

    @Test
    fun entriesLoggedAfterStartingAreSentExactlyOnce() = runTest {
        Logger.log("before")
        LogForwarder(send = { sent += it as SessionMessage.Log }, scope = backgroundScope.withUnconfinedDispatcher()).start()
        Logger.log("after", details = "details")
        assertEquals(listOf("before", "after"), sent.map { it.message })
        assertEquals("details", sent.last().details)
        assertEquals(sent.size, sent.map { it.id }.toSet().size)
    }

    private fun CoroutineScope.withUnconfinedDispatcher() = CoroutineScope(coroutineContext + UnconfinedTestDispatcher())
}
