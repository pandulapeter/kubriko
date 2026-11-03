/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * What agents write to their standard error, kept in [file] for the user to open when an agent misbehaves: each start
 * of a process, then its lines. A file grown past [maximumBytes] is moved aside to `<name>.old` at the next start, so
 * only the last two stay. Writing never fails an agent.
 */
public class AgentErrorLog(
    public val file: File,
    private val maximumBytes: Long = 1_000_000,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {

    @Synchronized
    internal fun started(command: AgentCommand) = write {
        if (file.length() > maximumBytes) file.renameTo(File(file.parentFile, file.name + ".old").also { it.delete() })
        "\n--- ${now().format(timestamp)} ${(listOf(command.program) + command.arguments).joinToString(" ")}\n"
    }

    @Synchronized
    internal fun append(line: String) = write { line + "\n" }

    private fun write(text: () -> String) {
        try {
            file.parentFile?.mkdirs()
            file.appendText(text())
        } catch (_: IOException) {
            // The agent goes on without its log.
        }
    }

    public companion object {
        private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /**
         * The log in Creator's settings folder.
         */
        public fun default(): AgentErrorLog = AgentErrorLog(File(settingsDirectory(), "logs/agent.log"))
    }
}
