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

import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.console_title
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleTest {

    @Test
    fun problemsErrorsAndWhatTheGameLogsOpenTheConsoleButBuildChatterDoesnt() {
        assertEquals(
            listOf(true, true, true, true, false, false),
            listOf(
                ConsoleEntry.Problem("Unresolved reference", location = null),
                ConsoleEntry.Log("Score: 3", source = "Paddle", isImportant = true),
                ConsoleEntry.SessionFailure("The game stopped"),
                ConsoleEntry.Status(Res.string.console_title, isError = true),
                ConsoleEntry.Status(Res.string.console_title),
                ConsoleEntry.BuildOutput("> Task :compileKotlinDesktop"),
            ).map { it.opensConsole },
        )
    }
}
