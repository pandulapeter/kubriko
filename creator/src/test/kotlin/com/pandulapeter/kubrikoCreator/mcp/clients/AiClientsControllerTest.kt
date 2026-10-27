/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.ServerSocket
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiClientsControllerTest {
    private val directory = createTempDirectory().toFile()
    private val runner = FakeProcessRunner(installed = mutableSetOf("claude", "codex"))
    private var mcp: McpController? = null

    @AfterTest
    fun tearDown() {
        mcp?.close()
        directory.deleteRecursively()
    }

    private fun TestScope.controller(): AiClientsController {
        val created = McpController(
            server = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
            store = McpSettingsStore(File(directory, "mcp.json")),
            scope = testScope(),
            ioDispatcher = Dispatchers.IO,
        )
        created.setPort(ServerSocket(0).use { it.localPort })
        mcp = created
        return AiClientsController(mcp = created, runner = runner, scope = testScope(), ioDispatcher = UnconfinedTestDispatcher(testScheduler))
    }

    private suspend fun AiClientsController.awaitStatus(client: AiClient, expected: AiClientStatus) = withContext(Dispatchers.IO) {
        withTimeout(5_000) { statuses.first { it[client] == expected } }
    }

    @Test
    fun refreshingTellsInstalledToolsApartAndFindsTheConnectedOnes() = runTest {
        runner.registered["codex"] = "mcp add kubriko-creator"
        val controller = controller()
        controller.refresh()
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.NotConnected)
        controller.awaitStatus(AiClient.CODEX, AiClientStatus.Connected)
        controller.awaitStatus(AiClient.GEMINI_CLI, AiClientStatus.NotInstalled)
    }

    @Test
    fun connectingTurnsTheServerOnAndRegistersItInTheTool() = runTest {
        val controller = controller()
        controller.connect(AiClient.CLAUDE_CODE)
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.Connected)
        val settings = mcp!!.settings.value
        assertTrue(settings.isEnabled)
        assertTrue(mcp!!.state.value is McpServerState.Running)
        assertEquals(setOf(AiClient.CLAUDE_CODE), settings.connectedClients)
        assertEquals("claude " + AiClient.CLAUDE_CODE.addArguments(settings).joinToString(" "), runner.calls.single { " add " in it })
    }

    @Test
    fun aFailedConnectionShowsWhatTheToolSaid() = runTest {
        runner.failingOutput = "Use specific commands: claude-personal"
        val controller = controller()
        controller.connect(AiClient.CLAUDE_CODE)
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.Failed("Use specific commands: claude-personal"))
        assertEquals(emptySet(), mcp!!.settings.value.connectedClients)
    }

    @Test
    fun aWrapperThatOnlyPrintsSomethingIsNeitherConnectedNorConnectable() = runTest {
        runner.printingOnly["claude"] = "Use specific commands: claude-flashfood or claude-personal"
        val controller = controller()
        controller.refresh()
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.NotConnected)
        controller.connect(AiClient.CLAUDE_CODE)
        val failed = controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.Failed("Use specific commands: claude-flashfood or claude-personal " + AiClient.CLAUDE_CODE.addArguments(mcp!!.settings.value).joinToString(" ")))
        assertTrue(failed.isNotEmpty())
        assertEquals(emptySet(), mcp!!.settings.value.connectedClients)
    }

    @Test
    fun disconnectingRemovesItEvenIfItWasAlreadyGone() = runTest {
        val controller = controller()
        controller.connect(AiClient.CODEX)
        controller.awaitStatus(AiClient.CODEX, AiClientStatus.Connected)
        controller.disconnect(AiClient.CODEX)
        controller.awaitStatus(AiClient.CODEX, AiClientStatus.NotConnected)
        controller.disconnect(AiClient.CODEX)
        controller.awaitStatus(AiClient.CODEX, AiClientStatus.NotConnected)
        assertEquals(emptySet(), mcp!!.settings.value.connectedClients)
    }

    @Test
    fun connectedToolsAreConnectedAgainWithANewToken() = runTest {
        val controller = controller()
        controller.connect(AiClient.CODEX)
        controller.awaitStatus(AiClient.CODEX, AiClientStatus.Connected)
        mcp!!.regenerateToken()
        val newToken = mcp!!.settings.value.token
        withContext(Dispatchers.IO) { withTimeout(5_000) { while (runner.registered["codex"]?.contains(newToken) != true) Thread.sleep(20) } }
    }

    @Test
    fun aReplacedCommandIsUsedFromThenOn() = runTest {
        runner.installed += "claude-personal"
        val controller = controller()
        controller.setCommand(AiClient.CLAUDE_CODE, "claude-personal")
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.NotConnected)
        controller.connect(AiClient.CLAUDE_CODE)
        controller.awaitStatus(AiClient.CLAUDE_CODE, AiClientStatus.Connected)
        assertTrue("claude-personal" in runner.registered)
    }
}
