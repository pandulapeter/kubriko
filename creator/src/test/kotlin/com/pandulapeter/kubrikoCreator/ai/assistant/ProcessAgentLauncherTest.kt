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

import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProcessAgentLauncherTest {
    private val directory = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun stoppingAnAgentAlsoStopsTheProcessesItStarted() {
        if (System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")) return
        val channel = ProcessAgentLauncher(directory).launch(AgentCommand("sh", listOf("-c", "sleep 60 & wait")))
        var child: ProcessHandle? = null
        repeat(50) {
            child = child ?: ProcessHandle.current().descendants().filter { it.info().command().orElse("").endsWith("sleep") }.findFirst().orElse(null)
            if (child == null) Thread.sleep(100)
        }
        val sleep = assertNotNull(child)
        channel.stop()
        sleep.onExit().get(5, TimeUnit.SECONDS)
        assertFalse(sleep.isAlive)
    }
}
