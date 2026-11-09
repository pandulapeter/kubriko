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

    private var lineCount = 0L

    /**
     * The console after [added] more lines, of which [entries] are the newest, the game's first logged one numbered
     * [firstLogNumber].
     */
    private fun lines(added: Int, vararg entries: ConsoleEntry, firstLogNumber: Long? = null): ConsoleLines {
        lineCount += added
        return ConsoleLines(revision = lineCount, entries = entries.toList(), firstNumber = lineCount - entries.size, firstLogNumber = firstLogNumber)
    }

    @Test
    fun onlyTheFirstLineTheGameLoggedSinceItWasPlayedOpensTheConsole() {
        val opening = ConsoleOpening(ConsoleLines())
        assertEquals(true, opening.opensFor(lines(1, log, firstLogNumber = 0)))
        assertEquals(false, opening.opensFor(lines(1, log, log, firstLogNumber = 0)))
        assertEquals(true, opening.opensFor(lines(1, log, log, log, firstLogNumber = 2)))
        assertEquals(false, opening.opensFor(lines(1, log, log, log, log, firstLogNumber = 2)))
    }

    @Test
    fun aConsoleShownAgainDoesntOpenForTheSameFirstLogAgain() {
        val before = lines(1, log, firstLogNumber = 0)
        assertEquals(true, ConsoleOpening(ConsoleLines()).opensFor(before))
        val again = ConsoleOpening(before)
        assertEquals(false, again.opensFor(lines(1, log, log, firstLogNumber = 0)))
    }

    @Test
    fun problemsAndErrorsOpenItEveryTime() {
        val opening = ConsoleOpening(ConsoleLines())
        assertEquals(true, opening.opensFor(lines(1, log, firstLogNumber = 0)))
        assertEquals(true, opening.opensFor(lines(1, log, problem, firstLogNumber = 0)))
        assertEquals(true, opening.opensFor(lines(1, log, problem, ConsoleEntry.Status(WorkspaceStatus.BUILT, isError = true), firstLogNumber = 0)))
        assertEquals(false, opening.opensFor(lines(1, log, problem, ConsoleEntry.Status(WorkspaceStatus.BUILT, isError = true), chatter, firstLogNumber = 0)))
    }

    @Test
    fun everyLineAddedSinceTheLastLookCountsNotOnlyTheNewest() {
        val opening = ConsoleOpening(ConsoleLines())
        assertEquals(false, opening.opensFor(lines(1, chatter)))
        assertEquals(true, opening.opensFor(lines(3, chatter, problem, chatter, chatter)))
        assertEquals(false, opening.opensFor(lines(0, chatter, problem, chatter, chatter)))
    }

    @Test
    fun linesAlreadyThereWhenTheConsoleIsShownDontOpenIt() {
        val opening = ConsoleOpening(lines(2, problem, log, firstLogNumber = 1))
        assertEquals(false, opening.opensFor(lines(0, problem, log, firstLogNumber = 1)))
        assertEquals(true, opening.opensFor(lines(1, problem, log, problem, firstLogNumber = 1)))
    }
}
