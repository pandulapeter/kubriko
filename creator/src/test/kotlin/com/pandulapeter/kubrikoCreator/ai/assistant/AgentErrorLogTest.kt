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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.time.LocalDateTime
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentErrorLogTest {
    private val directory = createTempDirectory().toFile()
    private val file = File(directory, "logs/agent.log")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun eachStartIsHeadedByItsTimeAndCommandFollowedByItsLines() {
        val log = AgentErrorLog(file, now = { LocalDateTime.of(2026, 11, 1, 18, 48, 5) })
        log.started(AgentCommand("agy", listOf("--acp")))
        log.append("Starting server")
        assertEquals("\n--- 2026-11-01 18:48:05 agy --acp\nStarting server\n", file.readText())
    }

    @Test
    fun aLogGrownTooLargeIsMovedAsideAtTheNextStart() {
        val log = AgentErrorLog(file, maximumBytes = 10)
        log.started(AgentCommand("agy"))
        log.append("a line longer than ten bytes")
        log.started(AgentCommand("agy"))
        assertTrue(File(file.parentFile, "agent.log.old").readText().contains("a line longer"))
        assertFalse(file.readText().contains("a line longer"))
    }

    @Test
    fun anAgentsStandardErrorReachesTheLog() = runBlocking {
        val launcher = FakeAgentLauncher()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val agent = AcpAgent(AgentCommand("fake-agent"), launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO, errorLog = AgentErrorLog(file))
        try {
            agent.start()
            withTimeout(5_000) { while (!file.isFile || "Fake agent started." !in file.readText()) delay(20) }
        } finally {
            agent.close()
            launcher.close()
            scope.coroutineContext[Job]?.cancel()
        }
    }
}
