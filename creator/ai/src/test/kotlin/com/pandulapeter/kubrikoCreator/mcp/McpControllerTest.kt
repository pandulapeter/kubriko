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
import com.pandulapeter.kubrikoCreator.testing.TestPorts
import com.pandulapeter.kubrikoCreator.testing.awaitTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private fun TestScope.controller(isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true)) = McpController(
        server = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
        store = store,
        scope = testScope(),
        ioDispatcher = Dispatchers.IO,
        isAiEnabled = isAiEnabled,
    ).also { controllers += it }

    private fun unusedPortAbove(start: Int) = generateSequence(start) { it + ROUNDS * 3 }.first { port -> runCatching { ServerSocket(port).close() }.isSuccess }

    private suspend fun McpController.awaitState(expected: McpServerState) = withContext(Dispatchers.IO) {
        withTimeoutOrNull(5_000) { state.first { it == expected } } ?: state.value
    }

    private fun isListening(port: Int) = runCatching { Socket("127.0.0.1", port).close() }.isSuccess

    @Test
    fun theServerIsOffUntilEnabledAndStopsWhenDisabled() = runTest {
        val controller = controller()
        assertEquals(McpServerState.Off, controller.state.value)
        val port = TestPorts.shared.next()
        assertTrue(controller.setPort(port))
        controller.setEnabled(true)
        assertEquals(McpServerState.Running(port), controller.awaitState(McpServerState.Running(port)))
        assertTrue(isListening(port))
        controller.setEnabled(false)
        assertEquals(McpServerState.Off, controller.awaitState(McpServerState.Off))
        assertFalse(isListening(port))
    }

    @Test
    fun turningAiFeaturesOffStopsTheServerAndTurningThemOnStartsItAsItWas() = runTest {
        val port = TestPorts.shared.next()
        store.save(store.load().copy(isEnabled = true, port = port))
        val isAiEnabled = MutableStateFlow(false)
        val controller = controller(isAiEnabled)
        assertEquals(McpServerState.Off, controller.awaitState(McpServerState.Off))
        assertFalse(isListening(port))
        isAiEnabled.value = true
        assertEquals(McpServerState.Running(port), controller.awaitState(McpServerState.Running(port)))
        isAiEnabled.value = false
        assertEquals(McpServerState.Off, controller.awaitState(McpServerState.Off))
        assertFalse(isListening(port))
        assertTrue(controller.settings.value.isEnabled)
        controller.setPort(port)
        controller.setEnabled(true)
        assertEquals(McpServerState.Off, controller.enable())
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
    fun quickChangesEndWithOnlyTheLastServerRunning() = runTest {
        val controller = controller()
        // Each round has ports of its own, since binding a port again right after it was let go may fail, taken outside
        // the range the system hands out for port 0, which other tests' servers listen on.
        val rounds = List(ROUNDS) { round -> List(3) { index -> unusedPortAbove(RESERVED_PORTS_START + round * 3 + index) } }
        rounds.forEachIndexed { round, ports ->
            controller.setEnabled(true)
            ports.forEach { port ->
                controller.setPort(port)
                withContext(Dispatchers.IO) { Thread.sleep(round % 4L) }
            }
            if (round < rounds.lastIndex) controller.setEnabled(false)
        }
        val last = rounds.last().last()
        val listening = withContext(Dispatchers.IO) {
            withTimeoutOrNull(5_000) {
                while (!isListening(last) || controller.state.value != McpServerState.Running(last) || rounds.flatten().any { it != last && isListening(it) }) delay(10)
            }
            rounds.flatten().filter(::isListening)
        }
        assertEquals(listOf(last), listening)
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
        val port = TestPorts.shared.next()
        controller.setPort(port)
        val oldToken = controller.settings.value.token
        controller.regenerateToken()
        assertNotEquals(oldToken, controller.settings.value.token)
        awaitTrue(description = "the settings were saved") { store.load() == controller.settings.value }
    }

    private companion object {
        const val ROUNDS = 20
        const val RESERVED_PORTS_START = 24_000
    }
}
