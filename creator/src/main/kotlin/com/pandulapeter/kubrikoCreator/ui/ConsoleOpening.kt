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

/**
 * When the console opens by itself: for every line that [opensConsole] added since [initial] or the last [opensFor],
 * except that of what the game logs, only its first line since it started ([ConsoleLines.firstLogNumber]) does, so a
 * game logging every frame doesn't open it again each time it is closed. That holds for its important lines too, as
 * importance only says how a line is shown (Kubriko's engine logs routine messages as important, and a device's
 * printed Log Blocks all are); what goes wrong opens it every time as a problem or a failure.
 */
internal class ConsoleOpening(initial: ConsoleLines) {
    private var seenLineCount = initial.lineCount

    /**
     * Whether [lines] added a line that opens the console since the last call; every line added since is looked at,
     * however many came between two frames.
     */
    fun opensFor(lines: ConsoleLines): Boolean {
        val added = (lines.lineCount - seenLineCount).coerceIn(0, lines.entries.size.toLong()).toInt()
        seenLineCount = lines.lineCount
        var opens = false
        for (index in lines.entries.size - added until lines.entries.size) {
            val entry = lines.entries[index]
            opens = opens || if (entry is ConsoleEntry.Log) lines.firstNumber + index == lines.firstLogNumber else entry.opensConsole
        }
        return opens
    }

    /**
     * How many lines were ever added, up to the newest of these.
     */
    private val ConsoleLines.lineCount get() = firstNumber + entries.size
}
