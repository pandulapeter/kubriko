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

import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs sign-in commands in the system's terminal ([SystemTerminal]) rather than through pipes, for a command that needs a
 * terminal device. The window is the terminal's, so its channel carries no output and takes no input: it only learns how
 * the command ended, from the file its script writes.
 *
 * @param scripts Where the scripts and their exit codes are kept, each until it is read; what is left of a terminal that
 *   never ran its script, or a command nobody waited for any more, is deleted with the next launch.
 * @param workingDirectory Where the command runs, as [ProcessAgentLauncher] runs it.
 * @param open Starts the program that opens the window, which may end before the window does.
 */
public class SystemTerminalLauncher internal constructor(
    private val terminal: SystemTerminal,
    private val scripts: File,
    private val workingDirectory: File,
    private val pollInterval: Duration = 250.milliseconds,
    private val clock: () -> Long = System::currentTimeMillis,
    private val open: (List<String>) -> Unit = { ProcessBuilder(it).directory(workingDirectory).start() },
) : AgentLauncher {

    override fun launch(command: AgentCommand): AgentChannel {
        if (!scripts.isDirectory && !scripts.mkdirs()) throw IOException("Can't create ${scripts.path}")
        deleteLeftovers()
        val id = UUID.randomUUID().toString()
        val script = File(scripts, "$id.${terminal.script.extension}")
        val exitFile = File(scripts, "$id.exit")
        script.writeTextAtomically(terminal.script.text(command, workingDirectory, script, exitFile), isPrivate = true)
        script.setExecutable(true, true)
        try {
            open(terminal.openCommand(script))
        } catch (exception: IOException) {
            script.delete()
            throw exception
        }
        return Channel(script, exitFile, pollInterval)
    }

    private fun deleteLeftovers() {
        val oldest = clock() - LEFTOVER_AGE_IN_MILLISECONDS
        scripts.listFiles()?.filter { it.isFile && it.lastModified() < oldest }?.forEach { it.delete() }
    }

    private class Channel(
        private val script: File,
        private val exitFile: File,
        private val pollInterval: Duration,
    ) : AgentChannel {
        override val fromAgent: InputStream = InputStream.nullInputStream()
        override val toAgent: OutputStream = OutputStream.nullOutputStream()
        override val errors: InputStream = InputStream.nullInputStream()

        override suspend fun awaitExit(): Int {
            while (true) {
                val text = runInterruptible { exitFile.takeIf { it.isFile }?.readText() }
                if (text != null) {
                    runInterruptible { exitFile.delete() }
                    return text.trim().toIntOrNull() ?: UNREADABLE_EXIT_CODE
                }
                delay(pollInterval)
            }
        }

        /**
         * Stops waiting, and deletes the script if the terminal hasn't run it yet: the window is the user's to close.
         */
        override fun stop() {
            script.delete()
            exitFile.delete()
        }
    }

    public companion object {
        private const val UNREADABLE_EXIT_CODE = -1
        private const val LEFTOVER_AGE_IN_MILLISECONDS = 10L * 60 * 1000

        /**
         * The launcher of this computer's terminal, keeping its scripts in [scripts], or null where no terminal is found.
         */
        public fun forThisComputer(scripts: File, workingDirectory: File): SystemTerminalLauncher? =
            SystemTerminal.find()?.let { SystemTerminalLauncher(it, scripts, workingDirectory) }
    }
}
