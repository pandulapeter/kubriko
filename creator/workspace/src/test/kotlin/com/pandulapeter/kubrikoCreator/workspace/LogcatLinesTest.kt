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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogcatLinesTest {

    @Test
    fun whatTheGamePrintedShowsAsLogsAndItsLogBlocksWithTheirSource() {
        assertEquals(ConsoleEntry.Log(message = "Hit a brick", source = "Ball", isImportant = true), LogcatLines.entryOf("I/System.out: [Ball] Hit a brick"))
        assertEquals(ConsoleEntry.Log(message = "plain", source = null, isImportant = false), LogcatLines.entryOf("I/System.out    : plain"))
        assertEquals(ConsoleEntry.Log(message = "", source = null, isImportant = false), LogcatLines.entryOf("I/System.out: "))
    }

    @Test
    fun onlyWhatTheProcessWroteToItsErrorsOrLoggedAsOneShows() {
        assertEquals(ConsoleEntry.BuildOutput("FATAL EXCEPTION: main"), LogcatLines.entryOf("E/AndroidRuntime: FATAL EXCEPTION: main"))
        assertEquals(ConsoleEntry.BuildOutput("Abort message"), LogcatLines.entryOf("F/libc: Abort message"))
        assertEquals(ConsoleEntry.BuildOutput("java.io.IOException: Gone"), LogcatLines.entryOf("W/System.err: java.io.IOException: Gone"))
        assertNull(LogcatLines.entryOf("W/HWUI: Unknown dataspace 0"))
        assertNull(LogcatLines.entryOf("D/CompatChangeReporter: Compat change id reported"))
        assertNull(LogcatLines.entryOf("--------- beginning of main"))
    }

    @Test
    fun aCrashIsWhereAndroidSaysTheAppDied() {
        assertTrue(LogcatLines.isCrash("E/AndroidRuntime: FATAL EXCEPTION: main"))
        assertFalse(LogcatLines.isCrash("E/AndroidRuntime: Process: com.example.mygame, PID: 1234"))
        assertFalse(LogcatLines.isCrash("I/System.out: [Ball] FATAL EXCEPTION"))
    }
}
