/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.logger

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LoggerTest {

    @BeforeTest
    fun setUp() = resetLogger()

    @AfterTest
    fun tearDown() = resetLogger()

    @Test
    fun loggedEntryCarriesItsArguments() {
        Logger.log(message = "message", details = "details", source = "source", importance = Logger.Importance.LOW)

        val entry = Logger.logs.value.single()
        assertEquals("message", entry.message)
        assertEquals("details", entry.details)
        assertEquals("source", entry.source)
        assertEquals(Logger.Importance.LOW, entry.importance)
    }

    @Test
    fun entryDefaultsToHighImportanceWithoutDetailsOrSource() {
        Logger.log("message")

        val entry = Logger.logs.value.single()
        assertNull(entry.details)
        assertNull(entry.source)
        assertEquals(Logger.Importance.HIGH, entry.importance)
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun entryIsTimestampedWhenLogged() {
        val before = Clock.System.now().toEpochMilliseconds()
        Logger.log("message")
        val after = Clock.System.now().toEpochMilliseconds()

        assertTrue(Logger.logs.value.single().timestamp in before..after)
    }

    @Test
    fun newestEntryComesFirst() {
        Logger.log("first")
        Logger.log("second")

        assertEquals(listOf("second", "first"), Logger.logs.value.map { it.message })
    }

    @Test
    fun identicalMessagesGetDistinctIds() {
        Logger.log("message")
        Logger.log("message")

        val (newer, older) = Logger.logs.value
        assertNotEquals(newer.id, older.id)
    }

    @Test
    fun oldestEntriesAreDiscardedBeyondTheLimit() {
        Logger.entryLimit = 2

        Logger.log("first")
        Logger.log("second")
        Logger.log("third")

        assertEquals(listOf("third", "second"), Logger.logs.value.map { it.message })
    }

    @Test
    fun loweringTheLimitTrimsTheOldestEntriesImmediately() {
        repeat(3) { Logger.log("$it") }

        Logger.entryLimit = 1

        assertEquals(listOf("2"), Logger.logs.value.map { it.message })
    }

    @Test
    fun negativeLimitIsIgnored() {
        Logger.entryLimit = 2

        Logger.entryLimit = -1

        assertEquals(2, Logger.entryLimit)
    }

    @Test
    fun clearLogsRemovesEveryEntry() {
        Logger.log("first")
        Logger.log("second")

        Logger.clearLogs()

        assertTrue(Logger.logs.value.isEmpty())
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun latestEntryEmitsEveryNewEntryWithoutReplayingEarlierOnes() = runTest {
        Logger.log("before collecting")
        val emitted = mutableListOf<Logger.Entry>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { Logger.latestEntry.toList(emitted) }

        Logger.log("first")
        Logger.log("second")

        assertEquals(listOf("first", "second"), emitted.map { it.message })
        assertEquals(Logger.logs.value.first(), emitted.last())
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun zeroLimitKeepsNoEntriesButStillEmitsThem() = runTest {
        Logger.entryLimit = 0
        val emitted = mutableListOf<Logger.Entry>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { Logger.latestEntry.toList(emitted) }

        Logger.log("message")

        assertTrue(Logger.logs.value.isEmpty())
        assertEquals(listOf("message"), emitted.map { it.message })
    }

    private fun resetLogger() {
        Logger.entryLimit = DEFAULT_ENTRY_LIMIT
        Logger.clearLogs()
    }

    private companion object {
        const val DEFAULT_ENTRY_LIMIT = 1000
    }
}
