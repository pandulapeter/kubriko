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

import com.agentclientprotocol.model.AuthMethod
import com.agentclientprotocol.model.AuthMethodId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignInPartsTest {
    private val agent = AgentCommand("/opt/bin/npx", listOf("--yes", "adapter"), mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-personal"))

    @Test
    fun authStatusesAreReadFromTheAgentsNotification() {
        val account = AuthStatus.parse(
            Json.parseToJsonElement("""{"authStatus":{"kind":"account","label":"Claude Max","account":{"plan":"max","email":"me@example.com"}}}"""),
        )
        assertEquals(AuthStatus(AuthStatus.Kind.ACCOUNT, "Claude Max", email = "me@example.com"), account)
        val key = AuthStatus.parse(Json.parseToJsonElement("""{"authStatus":{"kind":"api_key","label":"Anthropic API key","detail":"ANTHROPIC_API_KEY"}}"""))
        assertEquals(AuthStatus(AuthStatus.Kind.API_KEY, "Anthropic API key", detail = "ANTHROPIC_API_KEY"), key)
        val none = AuthStatus.parse(Json.parseToJsonElement("""{"authStatus":{"kind":"none","label":"Not logged in"}}"""))
        assertFalse(none!!.isSignedIn)
        assertTrue(AuthStatus.parse(Json.parseToJsonElement("""{"authStatus":{"kind":"future"}}"""))!!.isSignedIn)
        assertNull(AuthStatus.parse(Json.parseToJsonElement("""{"other":1}""")))
        assertNull(AuthStatus.parse(null))
    }

    @Test
    fun terminalMethodsRunTheCommandTheirMetaNamesInTheAgentsEnvironment() {
        val method = AuthMethod.TerminalAuth(
            id = AuthMethodId("claude-ai-login"),
            name = "Claude Subscription",
            args = listOf("--cli", "auth", "login"),
            _meta = buildJsonObject {
                putJsonObject("terminal-auth") {
                    put("command", "/opt/bin/node")
                    putJsonArray("args") {
                        add("adapter.js")
                        add("--cli")
                    }
                    putJsonObject("env") { put("NO_COLOR", "1") }
                }
            },
        )
        val terminal = SignInMethod.of(listOf(method), agent).single().terminal!!
        assertEquals("/opt/bin/node", terminal.program)
        assertEquals(listOf("adapter.js", "--cli"), terminal.arguments)
        assertEquals(mapOf("CLAUDE_CONFIG_DIR" to "/Users/me/.claude-personal", "NO_COLOR" to "1"), terminal.environment)
    }

    @Test
    fun terminalMethodsWithoutMetaRunTheAgentWithTheirArguments() {
        val method = AuthMethod.TerminalAuth(id = AuthMethodId("login"), name = "Login", args = listOf("--login"), env = mapOf("A" to "b"))
        val terminal = SignInMethod.of(listOf(method), agent).single().terminal!!
        assertEquals(agent.program, terminal.program)
        assertEquals(listOf("--yes", "adapter", "--login"), terminal.arguments)
        assertEquals("b", terminal.environment["A"])
        assertEquals("/Users/me/.claude-personal", terminal.environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun agentMethodsAreKeptAndVariableMethodsLeftOut() {
        val methods = SignInMethod.of(
            listOf(
                AuthMethod.AgentAuth(AuthMethodId("gateway"), "Gateway", "Use a gateway"),
                AuthMethod.EnvVarAuth(AuthMethodId("variables"), "Variables", vars = emptyList()),
            ),
            agent,
        )
        assertEquals(listOf(SignInMethod(AuthMethodId("gateway"), "Gateway", "Use a gateway")), methods)
    }

    @Test
    fun terminalTextKeepsLinksAndDropsEscapeSequences() {
        val url = "https://claude.com/cai/oauth/authorize?code=true"
        val raw = "\u001B[1mOpening browser\u001B[0m…\nVisit: \u001B]8;;$url\u0007$url\u001B]8;;\u0007\nPaste code here if prompted > "
        val text = TerminalText.of(raw)
        assertEquals("Opening browser…\nVisit: $url\nPaste code here if prompted > ", text.text)
        assertEquals(listOf(url), text.links)
    }

    @Test
    fun terminalTextShowsTheLastVersionOfALineWrittenOver() {
        assertEquals("one\nthree\n", TerminalText.of("one\ntwo\rthree\r\n").text)
    }

    @Test
    fun terminalTextFindsBareLinksAndToleratesAnUnfinishedSequence() {
        val text = TerminalText.of("Go to https://example.com/login now\n\u001B]8;;https://exa")
        assertEquals("Go to https://example.com/login now\n", text.text)
        assertEquals(listOf("https://exa", "https://example.com/login"), text.links)
    }

    @Test
    fun secretsReachTheToolThroughItsInputOnly() {
        listOf(SecretCommands.MacKeychain, SecretCommands.SecretService("/usr/bin/secret-tool"), SecretCommands.CredentialLocker).forEach { commands ->
            val write = commands.write("anthropic-api-key")
            assertTrue(write.none { "sk-ant-secret" in it })
            assertTrue("sk-ant-secret" in commands.writeInput("sk-ant-secret"))
            assertTrue(commands.read("anthropic-api-key").any { "anthropic-api-key" in it })
        }
        assertEquals("sk\nsk\n", SecretCommands.MacKeychain.writeInput("sk"))
    }

    @Test
    fun eachPlatformHasItsSecretStore() {
        assertEquals(SecretCommands.MacKeychain, SecretCommands.forThisPlatform("Mac OS X", null))
        assertEquals(SecretCommands.CredentialLocker, SecretCommands.forThisPlatform("Windows 11", null))
        assertNull(SecretCommands.forThisPlatform("Linux", "/nowhere"))
    }
}
