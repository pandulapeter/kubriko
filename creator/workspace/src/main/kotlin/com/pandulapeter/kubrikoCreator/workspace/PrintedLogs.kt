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

/**
 * What a game prints, as a log in the console: the generated `KubrikoWiring.log` prints the Log Blocks as
 * `[source] message` where Kubriko Creator reads what the game prints rather than its session.
 */
internal object PrintedLogs {
    private val pattern = Regex("""\[([^\]]+)] (.*)""")

    fun entryOf(line: String): ConsoleEntry.Log = pattern.matchEntire(line)?.destructured?.let { (source, message) ->
        ConsoleEntry.Log(message = message, source = source, isImportant = true)
    } ?: ConsoleEntry.Log(message = line, source = null, isImportant = false)
}
