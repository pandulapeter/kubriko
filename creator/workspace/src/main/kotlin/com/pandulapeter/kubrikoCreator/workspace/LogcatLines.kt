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
 * What a line of `adb logcat -v tag` (`I/Tag: message`) shows in the console: what the game printed, its Log Blocks as
 * logs, and the errors of its process; the rest, which Android logs for every app, is left out.
 */
internal object LogcatLines {
    private val pattern = Regex("""([VDIWEFA])/(.+?)\s*: ?(.*)""")
    private const val PRINTED_TAG = "System.out"
    private const val CRASH_TAG = "AndroidRuntime"
    private const val ERROR_LEVELS = "EFA"

    fun entryOf(line: String): ConsoleEntry? {
        val match = pattern.matchEntire(line) ?: return null
        val (level, tag, message) = match.destructured
        return when {
            tag == PRINTED_TAG -> PrintedLogs.entryOf(message)
            level in ERROR_LEVELS -> ConsoleEntry.BuildOutput(message)
            else -> null
        }
    }

    /**
     * Whether [line] is where Android says the app crashed.
     */
    fun isCrash(line: String) = pattern.matchEntire(line)?.destructured?.let { (_, tag, message) -> tag == CRASH_TAG && message.startsWith("FATAL EXCEPTION") } == true
}
