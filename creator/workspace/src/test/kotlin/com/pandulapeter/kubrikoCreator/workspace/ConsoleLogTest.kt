/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ConsoleLogTest {
    private fun line(number: Int) = ConsoleEntry.BuildOutput("Line $number")

    private fun ConsoleLog.texts() = lines.value.entries.map { (it as ConsoleEntry.BuildOutput).line }

    @Test
    fun theNewestLinesAreKeptOldestFirstWithTheNumberOfTheFirst() {
        val log = ConsoleLog(capacity = 3)
        (1..7).forEach { log.append(line(it)) }
        assertEquals(listOf("Line 5", "Line 6", "Line 7"), log.texts())
        assertEquals(4L, log.lines.value.firstNumber)
        assertEquals(7L, log.lines.value.revision)
    }

    @Test
    fun linesOnceShownNeverChangeWhileMoreArrive() {
        val log = ConsoleLog(capacity = 3)
        (1..2).forEach { log.append(line(it)) }
        val shown = log.lines.value
        (3..20).forEach { log.append(line(it)) }
        assertEquals(listOf(line(1), line(2)), shown.entries)
        assertEquals(listOf("Line 18", "Line 19", "Line 20"), log.texts())
    }

    @Test
    fun theSameLineAgainIsStillANewRevision() {
        val log = ConsoleLog(capacity = 3)
        log.append(line(1))
        val before = log.lines.value
        log.append(line(1))
        log.append(line(1))
        log.append(line(1))
        assertNotEquals(before, log.lines.value)
        assertEquals(List(3) { line(1) }, log.lines.value.entries)
    }

    @Test
    fun clearingEmptiesTheLinesAndNumbersOnFromWhereTheyEnded() {
        val log = ConsoleLog(capacity = 3)
        (1..5).forEach { log.append(line(it)) }
        val shown = log.lines.value
        log.clear()
        assertEquals(ConsoleLines(revision = 6, firstNumber = 5), log.lines.value)
        log.append(line(6))
        assertEquals(listOf("Line 6"), log.texts())
        assertEquals(5L, log.lines.value.firstNumber)
        assertEquals(listOf("Line 3", "Line 4", "Line 5"), shown.entries.map { (it as ConsoleEntry.BuildOutput).line })
    }

    @Test
    fun linesFromManyThreadsAreAllCounted() {
        val log = ConsoleLog(capacity = 100)
        val start = CountDownLatch(1)
        val threads = List(4) { index ->
            thread {
                start.await()
                repeat(1_000) { log.append(line(index)) }
            }
        }
        start.countDown()
        threads.forEach { it.join(TimeUnit.SECONDS.toMillis(10)) }
        assertEquals(4_000L, log.lines.value.revision)
        assertEquals(100, log.lines.value.entries.size)
        assertEquals(3_900L, log.lines.value.firstNumber)
    }

    @Test
    fun readingOutsideTheLinesFails() {
        val log = ConsoleLog(capacity = 3)
        log.append(line(1))
        assertTrue(runCatching { log.lines.value.entries[1] }.exceptionOrNull() is IndexOutOfBoundsException)
    }
}
