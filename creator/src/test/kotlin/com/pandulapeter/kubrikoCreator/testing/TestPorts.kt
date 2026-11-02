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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hands out free ports from [range], for code that needs a fixed port (`McpController`). Code that can bind port 0 itself should.
 *
 * The shared range lies outside every system's ephemeral range (Linux 32768–60999, macOS and Windows 49152–65535), where the
 * port-0 binds of other tests in the same JVM land, so nothing else takes a port between handing it out and the test binding it.
 */
internal class TestPorts(
    private val range: IntRange,
) {
    private val counter = AtomicInteger()

    /**
     * The next port of the range that can be bound on 127.0.0.1.
     *
     * @throws IllegalStateException When a full lap of the range finds none.
     */
    fun next(): Int {
        repeat(range.count()) {
            val port = range.first + Math.floorMod(counter.getAndIncrement(), range.count())
            if (isFree(port)) return port
        }
        throw IllegalStateException("No free port in $range.")
    }

    private fun isFree(port: Int) = runCatching { ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).close() }.isSuccess

    companion object {
        val shared = TestPorts(20_000 until 24_000)
    }
}
