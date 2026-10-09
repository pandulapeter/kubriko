/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation.ui

import com.pandulapeter.kubriko.logger.Logger
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogEntryTextTest {

    private fun entry(
        source: String? = null,
        details: String? = null,
    ) = Logger.Entry(
        id = "1",
        message = "m",
        details = details,
        source = source,
        timestamp = 3_723_004L,
        importance = Logger.Importance.LOW,
    )

    @Test
    fun entryWithoutSourceOrDetailsShowsTheTimeAndMessage() {
        assertEquals("[01:02:03.004] m", logEntryText(entry(), TimeZone.UTC))
    }

    @Test
    fun sourceIsPrefixedToTheMessage() {
        assertEquals("[01:02:03.004] Src: m", logEntryText(entry(source = "Src"), TimeZone.UTC))
    }

    @Test
    fun onlyNonBlankDetailsAddTheMarker() {
        assertTrue(logEntryText(entry(details = "d"), TimeZone.UTC).endsWith("*"))
        assertEquals("[01:02:03.004] m", logEntryText(entry(details = " "), TimeZone.UTC))
    }

    @Test
    fun hueDependsOnlyOnThePartAfterTheLastAt() {
        assertEquals(sourceHue("A@1"), sourceHue("B@1"))
    }

    @Test
    fun hueIsAlwaysWithinTheColorWheel() {
        assertTrue("polygenelubricants".hashCode() < 0)
        listOf("", "Src", "A@1", "polygenelubricants", "com.example.Actor@7f31245a").forEach { source ->
            assertTrue(sourceHue(source) in 0f..<360f, "Hue of \"$source\" was ${sourceHue(source)}")
        }
    }
}
