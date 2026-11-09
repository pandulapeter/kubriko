/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleOpeningTest {
    private val log = ConsoleEntry.Log("Tick", source = "Paddle", isImportant = false)
    private val chatter = ConsoleEntry.BuildOutput("> Task :compileKotlinDesktop")
    private val problem = ConsoleEntry.Problem("Unresolved reference", location = null)

    private fun lines(revision: Long, vararg entries: ConsoleEntry) = ConsoleLines(revision, entries.toList())

    @Test
    fun whatTheGameLogsOpensTheConsoleOncePerPlaySession() {
        val opening = ConsoleOpening()
        assertEquals(true, opening.opensFor(lines(1, log)))
        assertEquals(false, opening.opensFor(lines(2, log, log)))
        opening.onPlayStarted()
        assertEquals(true, opening.opensFor(lines(3, log, log, log)))
    }

    @Test
    fun problemsAndErrorsOpenItEveryTime() {
        val opening = ConsoleOpening()
        assertEquals(true, opening.opensFor(lines(1, log)))
        assertEquals(true, opening.opensFor(lines(2, log, problem)))
        assertEquals(true, opening.opensFor(lines(3, log, problem, ConsoleEntry.Status(WorkspaceStatus.BUILT, isError = true))))
        assertEquals(false, opening.opensFor(lines(4, log, problem, ConsoleEntry.Status(WorkspaceStatus.BUILT, isError = true), chatter)))
    }

    @Test
    fun everyLineAddedSinceTheLastLookCountsNotOnlyTheNewest() {
        val opening = ConsoleOpening()
        assertEquals(false, opening.opensFor(lines(1, chatter)))
        assertEquals(true, opening.opensFor(lines(4, chatter, problem, chatter, chatter)))
        assertEquals(false, opening.opensFor(lines(4, chatter, problem, chatter, chatter)))
    }
}
