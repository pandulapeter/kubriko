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

import kotlinx.serialization.SerializationException
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AgentRegistryTest {
    private val directory = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun theRegistryIsReadIntoWhatThisPlatformCanRunClaudeAgentFirst() {
        val agents = AgentRegistry.parse(REGISTRY, platform = "darwin-aarch64")
        assertEquals(listOf("claude-acp", "codex-acp", "fast-agent", "antigravity-acp"), agents.map { it.id })
        assertEquals(RegistryAgent.claude, agents[0])
        assertEquals(AgentDistribution.Npx("@zed-industries/codex-acp@2.4.0", environment = mapOf("MODE" to "acp")), agents[1].distribution)
        assertEquals(AgentDistribution.Uvx("fast-agent-acp==0.10.1", listOf("-x")), agents[2].distribution)
        assertEquals(
            AgentDistribution.Binary("https://dl.google.com/agy-darwin-arm64.zip", "./agy_acp_server.par", sha256 = "abc"),
            agents[3].distribution,
        )
        assertEquals("Google Antigravity", agents[3].name)
    }

    @Test
    fun anAgentWithoutABuildForThisPlatformIsLeftOut() {
        assertEquals(listOf("claude-acp", "codex-acp", "fast-agent"), AgentRegistry.parse(REGISTRY, platform = "linux-x86_64").map { it.id })
        assertEquals(listOf("claude-acp", "codex-acp", "fast-agent"), AgentRegistry.parse(REGISTRY, platform = null).map { it.id })
    }

    @Test
    fun somethingElseThanTheRegistryIsRejected() {
        assertFailsWith<SerializationException> { AgentRegistry.parse("""{"agents": "none"}""", "darwin-aarch64") }
    }

    @Test
    fun anEntryThatCantBeReadIsLeftOutAndTheOthersStay() {
        val registry = """{"agents": [
            {"id": "no-version", "name": "No version", "distribution": {"npx": {"package": "no-version"}}},
            {"id": "odd-distribution", "name": "Odd distribution", "version": "1.0.0", "distribution": "npx"},
            {"id": 1},
            {"id": "good", "name": "Good", "version": "1.0.0", "distribution": {"npx": {"package": "good@1.0.0"}}}
        ]}"""
        assertEquals(listOf("claude-acp", "good"), AgentRegistry.parse(registry, "darwin-aarch64").map { it.id })
    }

    @Test
    fun platformsAreNamedAsTheRegistryNamesThem() {
        assertEquals("darwin-aarch64", AgentRegistry.platformKey("Mac OS X", "aarch64"))
        assertEquals("windows-x86_64", AgentRegistry.platformKey("Windows 11", "amd64"))
        assertEquals("linux-aarch64", AgentRegistry.platformKey("Linux", "arm64"))
        assertNull(AgentRegistry.platformKey("FreeBSD", "amd64"))
        assertNull(AgentRegistry.platformKey("Linux", "riscv64"))
    }

    @Test
    fun theListWorksOfflineFromWhatWasLastFetched() {
        val cache = File(directory, "agent-registry.json")
        var response: String? = null
        val store = AgentRegistryStore(cache, "darwin-aarch64") { response }
        assertEquals(listOf(RegistryAgent.claude), store.cached())
        assertNull(store.refresh())
        response = REGISTRY
        assertEquals(4, store.refresh()?.size)
        response = "<html>Not found</html>"
        assertNull(store.refresh())
        assertEquals(4, AgentRegistryStore(cache, "darwin-aarch64") { null }.cached().size)
    }

    private companion object {
        val REGISTRY = """
            {
              "version": "1.0.0",
              "agents": [
                {"id": "claude-acp", "name": "Claude Agent", "version": "1.0.0", "distribution": {"npx": {"package": "@agentclientprotocol/claude-agent-acp@1.0.0"}}},
                {"id": "antigravity-acp", "name": "Google Antigravity", "version": "1.4.5", "license": "proprietary", "distribution": {"binary": {
                  "darwin-aarch64": {"archive": "https://dl.google.com/agy-darwin-arm64.zip", "cmd": "./agy_acp_server.par", "sha256": "abc"}
                }}},
                {"id": "codex-acp", "name": "Codex", "version": "2.4.0", "distribution": {"npx": {"package": "@zed-industries/codex-acp@2.4.0", "env": {"MODE": "acp"}}}},
                {"id": "fast-agent", "name": "fast-agent", "version": "0.10.1", "distribution": {"uvx": {"package": "fast-agent-acp==0.10.1", "args": ["-x"]}}}
              ],
              "extensions": []
            }
        """.trimIndent()
    }
}
