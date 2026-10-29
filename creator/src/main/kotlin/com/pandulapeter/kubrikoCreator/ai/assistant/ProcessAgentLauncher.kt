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

import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Starts agents as processes of their own, in [workingDirectory].
 */
internal class ProcessAgentLauncher(
    private val workingDirectory: File,
) : AgentLauncher {

    override fun launch(command: AgentCommand): AgentChannel {
        val process = ProcessBuilder(listOf(command.program) + command.arguments)
            .directory(workingDirectory)
            .apply { environment().putAll(command.environment) }
            .start()
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
            process.destroy()
            if (!process.waitFor(STOP_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor()
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_IN_SECONDS = 3L
    }
}
