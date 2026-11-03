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

import com.pandulapeter.kubrikoCreator.session.LiveGameProcesses
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Starts agents as processes of their own, in [workingDirectory].
 */
public class ProcessAgentLauncher(
    private val workingDirectory: File,
) : AgentLauncher {

    override fun launch(command: AgentCommand): AgentChannel {
        val process = ProcessBuilder(commandLineOf(command))
            .directory(workingDirectory)
            .apply { environment().putAll(command.environment) }
            .start()
        LiveGameProcesses.add(process)
        process.onExit().thenRun { LiveGameProcesses.remove(process) }
        return ProcessAgentChannel(process)
    }

    private class ProcessAgentChannel(
        private val process: Process,
    ) : AgentChannel {
        override val fromAgent = process.inputStream!!
        override val toAgent = process.outputStream!!
        override val errors = process.errorStream!!

        override suspend fun awaitExit() = runInterruptible { process.waitFor() }

        override fun stop() {
            // npx runs the agent, which runs claude, and neither a forced kill nor Windows passes the signal on.
            val descendants = process.descendants().toList()
            descendants.forEach { it.destroy() }
            process.destroy()
            if (!process.waitFor(STOP_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor()
            }
            descendants.forEach { it.destroyForcibly() }
            LiveGameProcesses.remove(process)
        }
    }

    internal companion object {
        private const val STOP_TIMEOUT_IN_SECONDS = 3L
        private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")

        /**
         * What starts [command]: a Windows batch file, such as the `npx.cmd` npm installs, only runs through `cmd`.
         */
        internal fun commandLineOf(command: AgentCommand, isWindows: Boolean = this.isWindows) =
            if (isWindows && command.program.lowercase().let { it.endsWith(".cmd") || it.endsWith(".bat") }) {
                listOf("cmd", "/c", command.program) + command.arguments
            } else {
                listOf(command.program) + command.arguments
            }
    }
}
