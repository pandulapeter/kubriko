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
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Runs sign-in commands in the system's terminal ([SystemTerminal]) rather than through pipes, for a command that needs a
 * terminal device. The window is the terminal's, so its channel carries no output and takes no input: it only learns how
 * the command ended, from the files its script writes, and that it ended without saying so (the window closed, or never
 * ran the script), when the script's process is gone, or never started within [startTimeout].
 *
 * @param scripts Where the scripts and their files are kept, each until it is read; what is left of a terminal that never
 *   ran its script, or a command nobody waited for any more, is deleted with the next launch.
 * @param workingDirectory Where the command runs, as [ProcessAgentLauncher] runs it.
 * @param isAlive Whether the process of an ID a script wrote still runs.
 * @param open Starts the program that opens the window, throwing an [IOException] if it can't.
 */
public class SystemTerminalLauncher internal constructor(
    private val terminal: SystemTerminal,
    private val scripts: File,
    private val workingDirectory: File,
    private val pollInterval: Duration = 250.milliseconds,
    private val startTimeout: Duration = 60.seconds,
    private val clock: () -> Long = System::currentTimeMillis,
    private val isAlive: (Long) -> Boolean = { processId -> ProcessHandle.of(processId).map { it.isAlive }.orElse(false) },
    private val open: (List<String>) -> Unit = { command -> openWindow(command, workingDirectory) },
) : AgentLauncher {

    override fun launch(command: AgentCommand): AgentChannel {
        if (!scripts.isDirectory && !scripts.mkdirs()) throw IOException("Can't create ${scripts.path}")
        deleteLeftovers()
        val id = UUID.randomUUID().toString()
        val files = TerminalScriptFiles(
            script = File(scripts, "$id.${terminal.script.extension}"),
            processId = File(scripts, "$id.pid"),
            exitCode = File(scripts, "$id.exit"),
        )
        files.script.writeTextAtomically(terminal.script.text(command, workingDirectory, files), isPrivate = true)
        files.script.setExecutable(true, true)
        try {
            open(terminal.openCommand(files.script))
        } catch (exception: IOException) {
            files.script.delete()
            throw exception
        }
        return Channel(files)
    }

    private fun deleteLeftovers() {
        val oldest = clock() - LEFTOVER_AGE_IN_MILLISECONDS
        scripts.listFiles()?.filter { it.isFile && it.lastModified() < oldest }?.forEach { it.delete() }
    }

    private inner class Channel(
        private val files: TerminalScriptFiles,
    ) : AgentChannel {
        override val fromAgent: InputStream = InputStream.nullInputStream()
        override val toAgent: OutputStream = OutputStream.nullOutputStream()
        override val errors: InputStream = InputStream.nullInputStream()

        override suspend fun awaitExit(): Int {
            val startedBy = clock() + startTimeout.inWholeMilliseconds
            var processId: Long? = null
            while (true) {
                exitCode()?.let { return it }
                processId = processId ?: runInterruptible { files.processId.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() }
                val hasEnded = processId?.let { !isAlive(it) } ?: (clock() > startedBy)
                // The process may write its exit code just before it ends.
                if (hasEnded) return exitCode() ?: ENDED_UNREPORTED.also { runInterruptible { deleteFiles() } }
                delay(pollInterval)
            }
        }

        private suspend fun exitCode() = runInterruptible {
            files.exitCode.takeIf { it.isFile }?.readText()?.let { text ->
                deleteFiles()
                text.trim().toIntOrNull() ?: ENDED_UNREPORTED
            }
        }

        private fun deleteFiles() {
            files.script.delete()
            files.processId.delete()
            files.exitCode.delete()
        }

        /**
         * Stops waiting, and deletes the script if the terminal hasn't run it yet: the window is the user's to close.
         */
        override fun stop() = deleteFiles()
    }

    public companion object {
        /**
         * What a command that ended without writing its exit code ended with.
         */
        private const val ENDED_UNREPORTED = -1
        private const val LEFTOVER_AGE_IN_MILLISECONDS = 10L * 60 * 1000
        private const val OPENING_CHECK_IN_SECONDS = 3L

        /**
         * The launcher of this computer's terminal, keeping its scripts in [scripts], or null where no terminal is found.
         */
        public fun forThisComputer(scripts: File, workingDirectory: File): SystemTerminalLauncher? =
            SystemTerminal.find()?.let { SystemTerminalLauncher(it, scripts, workingDirectory) }

        /**
         * Runs [command], which opens the window, discarding what it prints, since a terminal emulator that stays in the
         * foreground prints for as long as its window is open. One that fails within a few seconds couldn't open it.
         */
        private fun openWindow(command: List<String>, workingDirectory: File) {
            val process = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            process.outputStream.close()
            if (process.waitFor(OPENING_CHECK_IN_SECONDS, TimeUnit.SECONDS) && process.exitValue() != 0) {
                throw IOException("${command.first()} ended with exit code ${process.exitValue()}")
            }
        }
    }
}
