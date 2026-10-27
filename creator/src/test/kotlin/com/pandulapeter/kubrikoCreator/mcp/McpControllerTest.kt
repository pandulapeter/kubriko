/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class McpControllerTest {
    private val directory = createTempDirectory().toFile()
    private val store = McpSettingsStore(File(directory, "mcp.json"))
    private val controllers = mutableListOf<McpController>()

    @AfterTest
    fun tearDown() {
        controllers.forEach { it.close() }
        directory.deleteRecursively()
    }

    private fun TestScope.controller() = McpController(
        server = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
        store = store,
        scope = testScope(),
        ioDispatcher = Dispatchers.IO,
    ).also { controllers += it }

    private fun freePort() = ServerSocket(0).use { it.localPort }

    private suspend fun McpController.awaitState(expected: McpServerState) = withContext(Dispatchers.IO) {
        withTimeoutOrNull(5_000) { state.first { it == expected } } ?: state.value
    }

    private fun isListening(port: Int) = runCatching { Socket("127.0.0.1", port).close() }.isSuccess

    @Test
    fun theServerIsOffUntilEnabledAndStopsWhenDisabled() = runTest {
        val controller = controller()
        assertEquals(McpServerState.Off, controller.state.value)
        val port = freePort()
        assertTrue(controller.setPort(port))
        controller.setEnabled(true)
        assertEquals(McpServerState.Running(port), controller.awaitState(McpServerState.Running(port)))
        assertTrue(isListening(port))
        controller.setEnabled(false)
        assertEquals(McpServerState.Off, controller.awaitState(McpServerState.Off))
        assertFalse(isListening(port))
    }

    @Test
    fun aTakenPortIsReported() = runTest {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { taken ->
            val controller = controller()
            controller.setPort(taken.localPort)
            controller.setEnabled(true)
            assertEquals(McpServerState.Failed(taken.localPort), controller.awaitState(McpServerState.Failed(taken.localPort)))
        }
    }

    @Test
    fun portsOutsideTheUserRangeAreRefused() = runTest {
        val controller = controller()
        assertFalse(controller.setPort(80))
        assertFalse(controller.setPort(70_000))
        assertEquals(McpSettings.DEFAULT_PORT, controller.settings.value.port)
    }

    @Test
    fun settingsAreKeptAndANewTokenReplacesTheOldOne() = runTest {
        val controller = controller()
        val port = freePort()
        controller.setPort(port)
        val oldToken = controller.settings.value.token
        controller.regenerateToken()
        assertNotEquals(oldToken, controller.settings.value.token)
        withContext(Dispatchers.IO) { Thread.sleep(200) }
        assertEquals(controller.settings.value, store.load())
    }
}
