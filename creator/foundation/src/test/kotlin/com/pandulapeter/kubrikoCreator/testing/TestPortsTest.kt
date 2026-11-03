/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import java.net.InetAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestPortsTest {
    private val localhost = InetAddress.getByName("127.0.0.1")

    @Test
    fun portsAreDistinctAndBindable() {
        val ports = List(20) { TestPorts.shared.next() }
        assertEquals(20, ports.toSet().size)
        assertTrue(ports.all { it in 20_000 until 24_000 })
        ports.forEach { ServerSocket(it, 1, localhost).close() }
    }

    @Test
    fun aTakenPortIsSkippedAndAFullRangeThrows() {
        val port = TestPorts.shared.next()
        val ports = TestPorts(port until port + 2)
        ServerSocket(port, 1, localhost).use {
            assertEquals(port + 1, ports.next())
            ServerSocket(port + 1, 1, localhost).use {
                assertFailsWith<IllegalStateException> { ports.next() }
            }
        }
    }
}
