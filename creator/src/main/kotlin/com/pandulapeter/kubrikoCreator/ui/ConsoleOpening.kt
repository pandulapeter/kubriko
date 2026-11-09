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
 * When the console opens by itself: for every line that [opensConsole] added since the last [opensFor], except that what
 * the game logs opens it only once per play session ([onPlayStarted]), so a game logging every frame doesn't open it
 * again each time it is closed.
 */
internal class ConsoleOpening {
    private var seenRevision = 0L
    private var hasLogOpened = false

    fun onPlayStarted() {
        hasLogOpened = false
    }

    /**
     * Whether [lines] added a line that opens the console since the last call; every line added since is looked at,
     * however many came between two frames.
     */
    fun opensFor(lines: ConsoleLines): Boolean {
        val added = (lines.revision - seenRevision).coerceIn(0, lines.entries.size.toLong()).toInt()
        seenRevision = lines.revision
        var opens = false
        for (index in lines.entries.size - added until lines.entries.size) {
            val entry = lines.entries[index]
            when {
                entry is ConsoleEntry.Log -> if (!hasLogOpened) {
                    hasLogOpened = true
                    opens = true
                }
                entry.opensConsole -> opens = true
            }
        }
        return opens
    }
}
