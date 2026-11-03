/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.testing.TestPorts
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * Kubriko Creator's real MCP server, turned on by the settings folder [CreatorProcess.writeMcpSettings] seeds, as the
 * started app's is, serving a project whose game is [runner]. [block] gets [CreatorTools] reaching it, and opens the
 * project with [ServedProject.open]; it runs on [Dispatchers.IO], since the tools block.
 */
internal suspend fun TestScope.withServedTools(runner: FakeProjectRunner = FakeProjectRunner(), block: suspend ServedProject.() -> Unit) {
    val settingsDirectory = testDirectory()
    val settings = McpSettings(isEnabled = true, port = TestPorts.shared.next(), token = "end-to-end-token")
    CreatorProcess.writeMcpSettings(settingsDirectory, settings)
    val project = ServedProject(CreatorTools(settings), CommandExecutor(commandContext(runner = runner), testScope()))
    val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val controller = McpController(
        server = McpToolServer(executor = { project.openExecutor }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
        store = McpSettingsStore(File(settingsDirectory, "mcp.json")),
        scope = serverScope,
        ioDispatcher = Dispatchers.IO,
    )
    try {
        // Closing a server that is still starting fails in the server's scope, so the block starts once it runs.
        withContext(Dispatchers.IO) {
            val state = withTimeout(10.seconds) { controller.state.first { it is McpServerState.Running || it is McpServerState.Failed } }
            check(state is McpServerState.Running) { "The MCP server didn't start: $state" }
            project.block()
        }
    } finally {
        project.tools.close()
        controller.close()
        serverScope.cancel()
    }
}

internal class ServedProject(
    val tools: CreatorTools,
    private val executor: CommandExecutor,
) {
    @Volatile
    var openExecutor: CommandExecutor? = null
        private set

    fun open() {
        openExecutor = executor
    }
}
