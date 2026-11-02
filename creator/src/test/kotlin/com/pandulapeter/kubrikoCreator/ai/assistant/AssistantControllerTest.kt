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

import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.FakeProcessRunner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AssistantControllerTest {
    private val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + mainThread)
    private val isSignedIn = AtomicBoolean(true)
    private val nextAgents = ConcurrentLinkedQueue<FakeAgent>()
    private val launcher = FakeAgentLauncher({ nextAgents.poll() ?: FakeAgent(isSignedIn = isSignedIn) })
    private val secrets = FakeSecretStore()
    private val terminals = FakeTerminalLauncher()
    private val runner = FakeProcessRunner(installed = mutableSetOf("npx"))
    private val settingsDirectory = createTempDirectory().toFile()
    private val commands = CopyOnWriteArrayList<AgentCommand>()
    private val profiles = CopyOnWriteArrayList<AgentProfile>()
    private val controllers = mutableListOf<AssistantController>()
    private val projectDirectory = File("/projects/breakout")

    @AfterTest
    fun tearDown() {
        controllers.forEach { it.close() }
        launcher.close()
        scope.cancel()
        mainThread.close()
        settingsDirectory.deleteRecursively()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { withTimeout(10.seconds) { block() } }

    private fun controller(
        registry: AgentRegistryStore? = null,
        installer: BinaryAgentInstaller? = null,
        isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
        secretStore: SecretStore = secrets,
    ) = AssistantController(
        scope = scope,
        runner = runner,
        store = AssistantSettingsStore(File(settingsDirectory, "assistant.json")),
        secrets = secretStore,
        signInLauncher = terminals,
        ioDispatcher = Dispatchers.IO,
        createAssistant = { command, _, profile ->
            profiles += profile
            commands += command
            Assistant(
                agent = AcpAgent(command, launcher, AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO, conversationCloseWait = 300.milliseconds),
                toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
                ioDispatcher = Dispatchers.IO,
                profile = profile,
                agentFolders = File(settingsDirectory, "agents"),
            )
        },
        home = "/Users/me",
        registry = registry,
        installer = installer,
        conversations = SavedConversationStore(File(settingsDirectory, "conversations.json")),
        clock = { 1_000 },
        isAiEnabled = isAiEnabled,
    ).also { controllers += it }

    private fun registry(vararg agents: String) = AgentRegistryStore(File(settingsDirectory, "agent-registry.json"), "darwin-aarch64") {
        """{"agents": [${agents.joinToString(",")}]}"""
    }

    private suspend fun <T> onMain(block: suspend () -> T) = withContext(mainThread) { block() }

    private suspend fun AssistantController.startChat(): AssistantChat {
        onMain {
            setProject(projectDirectory)
            newChat()
        }
        return currentChat.value!!
    }

    @Test
    fun conversationsNeedAnOpenProject() = test {
        val controller = controller()
        onMain { controller.newChat() }
        assertNull(controller.currentChat.value)
    }

    @Test
    fun withAiFeaturesOffNoConversationOpensNoAgentStartsAndTheRegistryIsNotFetched() = test {
        val fetches = AtomicInteger()
        val registry = AgentRegistryStore(File(settingsDirectory, "agent-registry.json"), "darwin-aarch64") {
            fetches.incrementAndGet()
            """{"agents": []}"""
        }
        val isAiEnabled = MutableStateFlow(false)
        val controller = controller(registry = registry, isAiEnabled = isAiEnabled)
        onMain {
            controller.setProject(projectDirectory)
            controller.newChat()
            controller.installNode()
        }
        delay(200)
        assertNull(controller.currentChat.value)
        assertTrue(commands.isEmpty())
        assertEquals(0, fetches.get())
        isAiEnabled.value = true
        withTimeout(5.seconds) { while (fetches.get() == 0) delay(10) }
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals(1, commands.size)
    }

    @Test
    fun turningAiFeaturesOffEndsTheConversationsAndTheAgent() = test {
        val isAiEnabled = MutableStateFlow(true)
        val controller = controller(isAiEnabled = isAiEnabled)
        controller.startChat().state.first { it == ChatState.Ready }
        isAiEnabled.value = false
        withTimeout(5.seconds) { while (controller.currentChat.value != null || launcher.channels.any { !it.isStopped }) delay(10) }
        assertTrue(controller.chats.value.isEmpty())
    }

    @Test
    fun aConversationRunsClaudeAgentThroughNpxInTheProject() = test {
        val chat = controller().startChat()
        chat.state.first { it == ChatState.Ready }
        assertEquals(File("/opt/tools/bin/npx").path, commands.single().program)
        assertEquals((RegistryAgent.claude.distribution as AgentDistribution.Npx).packageName, commands.single().arguments.last())
        assertEquals(listOf(AgentProfile.claude), profiles)
        assertEquals(projectDirectory.absolutePath, launcher.agents.single().newSessions.single().cwd)
    }

    @Test
    fun aCustomAgentRunsItsCommandWithTheFallbacksAndIsRemembered() = test {
        runner.installed += "goose"
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        onMain { controller.selectAgent(AgentChoice.Custom("GOOSE_MODE=chat goose acp")) }
        controller.currentChat.value!!.state.first { it == ChatState.Ready }
        val command = commands.last()
        assertEquals("/opt/tools/bin/goose", command.program)
        assertEquals(listOf("acp"), command.arguments)
        assertEquals("chat", command.environment["GOOSE_MODE"])
        assertEquals(AgentProfile.fallback, profiles.last())
        assertEquals(AgentChoice.Custom("GOOSE_MODE=chat goose acp"), controller().agent.value)
        onMain { controller.selectAgent(AgentChoice.Registry(RegistryAgent.claude)) }
        assertEquals("GOOSE_MODE=chat goose acp", controller().customAgentCommand)
        assertTrue(controller().agent.value.isClaude)
    }

    @Test
    fun aRememberedRegistryAgentRunsThroughItsDistributionWithTheFallbacks() = test {
        val registry = registry(CODEX)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "codex-acp"))
        val controller = controller(registry)
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals("Codex", controller.agent.value.name)
        assertEquals(listOf("--yes", "@zed-industries/codex-acp@2.4.0"), commands.single().arguments)
        assertEquals(listOf(AgentProfile.fallback), profiles)
        assertEquals(listOf("Claude Agent", "Codex"), controller.agents.value.map { it.name })
    }

    @Test
    fun aBinaryAgentIsDownloadedThenRunFromItsFolder() = test {
        val registry = registry(ANTIGRAVITY)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "antigravity-acp"))
        val served = File(settingsDirectory, "served").also { it.writeText("program") }
        val installer = BinaryAgentInstaller(File(settingsDirectory, "tools"), download = { _, target, _ -> served.copyTo(target, overwrite = true) })
        val controller = controller(registry, installer)
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals(File(settingsDirectory, "tools/antigravity-acp/1.4.5/agy_acp_server.par").path, commands.single().program)
        assertEquals("Google Antigravity", controller.agent.value.name)
    }

    @Test
    fun aBinaryAgentThatCantBeDownloadedSaysWhy() = test {
        val registry = registry(ANTIGRAVITY)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "antigravity-acp"))
        val installer = BinaryAgentInstaller(File(settingsDirectory, "tools"), download = { _, _, _ -> throw java.io.IOException("Offline.") })
        val failed = controller(registry, installer).startChat().state.first { it is ChatState.Failed }
        assertEquals(ChatState.Failed(FailureReason.AGENT_NOT_STARTED, listOf("Offline.")), failed)
    }

    @Test
    fun variablesGivenForSigningInAreKeptAndTheAgentStartsAgainWithThem() = test {
        val registry = registry(CODEX)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "codex-acp"))
        val controller = controller(registry)
        controller.startChat().state.first { it == ChatState.Ready }
        val method = controller.signInMethods.first { methods -> methods.any { it.variables != null } }.single { it.variables != null }
        onMain { controller.signIn(method) }
        assertEquals(method, controller.variablesForm.value)
        onMain { controller.setVariables(method, mapOf("FAKE_API_KEY" to " secret ")) }
        while (commands.size < 2) delay(10)
        assertEquals("secret", commands.last().environment["FAKE_API_KEY"])
        assertEquals(null, controller.variablesForm.value)
        val afterRestart = controller(registry)
        afterRestart.startChat().state.first { it == ChatState.Ready }
        assertEquals("secret", commands.last().environment["FAKE_API_KEY"])
    }

    @Test
    fun rememberedVariablesAreReadOffTheMainThread() = test {
        val registry = registry(CODEX)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "codex-acp", agentVariables = mapOf("codex-acp" to listOf("FAKE_API_KEY"))))
        secrets.secrets["agent-variable:codex-acp:FAKE_API_KEY"] = "secret"
        val readingThreads = CopyOnWriteArrayList<Thread>()
        val recordingSecrets = object : SecretStore by secrets {
            override fun read(name: String) = secrets.read(name).also { readingThreads += Thread.currentThread() }
        }
        val controller = controller(registry, secretStore = recordingSecrets)
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals("secret", commands.single().environment["FAKE_API_KEY"])
        val main = onMain { Thread.currentThread() }
        assertTrue(readingThreads.isNotEmpty())
        assertFalse(main in readingThreads)
    }

    @Test
    fun anAgentRunByUvxNeedsUv() = test {
        val registry = registry(FAST_AGENT)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "fast-agent"))
        val chat = controller(registry).startChat()
        assertEquals(ChatState.Failed(FailureReason.UV_MISSING), chat.state.first { it is ChatState.Failed })
        runner.installed += "uvx"
        onMain { chat.retry() }
        chat.state.first { it == ChatState.Ready }
        assertEquals(File("/opt/tools/bin/uvx").path, commands.single().program)
        assertEquals(listOf("fast-agent-acp==0.10.1"), commands.single().arguments)
    }

    @Test
    fun aConversationClosedInThisRunIsReopenedAsItWas() = test {
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { chat.send("hello") }
        chat.isBusy.first { !it }
        onMain { controller.close(chat) }
        val saved = controller.savedConversations.first { it.isNotEmpty() }.single()
        assertEquals("hello", saved.title)
        onMain { controller.resume(saved) }
        assertSame(chat, controller.currentChat.value)
        assertEquals(emptyList(), controller.savedConversations.value)
    }

    @Test
    fun aConversationOfAnEarlierRunIsContinuedWithItsHistory() = test {
        val earlier = controller()
        val chat = earlier.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { chat.send("hello") }
        chat.isBusy.first { !it }
        val controller = controller()
        onMain { controller.setProject(projectDirectory) }
        val saved = controller.savedConversations.first { it.isNotEmpty() }.single()
        onMain { controller.resume(saved) }
        val resumed = controller.currentChat.value!!
        resumed.state.first { it == ChatState.Ready }
        assertEquals(TranscriptItem.AgentMessage(FakeAgent.HISTORY), resumed.transcript.value.items.single())
        assertEquals(chat.conversationId.value, launcher.agents.last().loadedSessions.single().value)
    }

    @Test
    fun withoutNodeTheConversationSaysSo() = test {
        runner.installed.clear()
        val chat = controller().startChat()
        assertEquals(ChatState.Failed(FailureReason.NODE_MISSING), chat.state.first { it is ChatState.Failed })
    }

    @Test
    fun closingTheProjectEndsItsConversations() = test {
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { controller.setProject(File("/projects/other")) }
        assertEquals(emptyList(), controller.chats.value)
        assertNull(controller.currentChat.value)
    }

    @Test
    fun switchingAgentsWhileAConversationIsClosingStartsTheNewAgentAndListsItsConversations() = test {
        nextAgents += FakeAgent(isSignedIn = isSignedIn, sessionClose = { awaitCancellation() })
        val registry = registry(CODEX)
        registry.refresh()
        val earlier = SavedConversation(projectDirectory.absolutePath, "codex-acp", "earlier", "Earlier", 500)
        SavedConversationStore(File(settingsDirectory, "conversations.json")).save(earlier)
        val controller = controller(registry)
        controller.startChat().state.first { it == ChatState.Ready }
        val codex = controller.agents.first { agents -> agents.any { it.id == "codex-acp" } }.single { it.id == "codex-acp" }
        onMain { controller.selectAgent(AgentChoice.Registry(codex)) }
        controller.currentChat.value!!.state.first { it == ChatState.Ready }
        assertEquals(listOf("--yes", "@zed-industries/codex-acp@2.4.0"), commands.last().arguments)
        assertEquals(listOf(earlier), controller.savedConversations.first { it.isNotEmpty() })
        withTimeout(5.seconds) { while (!launcher.channels.first().isStopped) delay(10) }
    }

    @Test
    fun aConversationOpenedWhileTheProjectChangesStartsOnAFreshAgent() = test {
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        val chat = onMain {
            controller.setProject(File("/projects/other"))
            controller.newChat()
            controller.currentChat.value!!
        }
        chat.state.first { it == ChatState.Ready }
        withTimeout(5.seconds) { while (!launcher.channels.first().isStopped) delay(10) }
        delay(200)
        assertEquals(ChatState.Ready, chat.state.value)
        assertEquals(2, launcher.channels.size)
    }

    @Test
    fun twoSwitchesInARowEndOnTheLastAgent() = test {
        val registry = registry(CODEX, GEMINI)
        registry.refresh()
        val controller = controller(registry)
        controller.startChat().state.first { it == ChatState.Ready }
        val agents = controller.agents.first { it.size == 3 }
        onMain {
            controller.selectAgent(AgentChoice.Registry(agents.single { it.id == "codex-acp" }))
            controller.selectAgent(AgentChoice.Registry(agents.single { it.id == "gemini" }))
        }
        val chat = controller.chats.value.single()
        chat.state.first { it == ChatState.Ready }
        assertTrue("@google/gemini-cli@1.0.0" in launcher.commands.last().arguments)
        withTimeout(5.seconds) { while (launcher.channels.count { !it.isStopped } > 1) delay(10) }
        assertFalse(launcher.channels.last().isStopped)
    }

    @Test
    fun endingAConversationShowsTheOneBefore() = test {
        val controller = controller()
        val first = controller.startChat()
        val second = onMain {
            controller.newChat()
            controller.currentChat.value!!
        }
        onMain { controller.close(second) }
        assertSame(first, controller.currentChat.value)
        assertEquals(listOf(first), controller.chats.value)
    }

    @Test
    fun anAliasesConfigurationIsUsedAndRemembered() = test {
        runner.aliases["claude-personal"] = "CLAUDE_CONFIG_DIR=~/.claude-personal claude"
        val controller = controller()
        onMain { controller.findConfigurations() }
        val personal = controller.configurations.first { it.size == 2 }.last()
        onMain { controller.selectConfiguration(personal) }
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals("/Users/me/.claude-personal", commands.last().environment["CLAUDE_CONFIG_DIR"])
        val restarted = controller()
        assertEquals("claude-personal", restarted.configuration.value.alias)
        restarted.startChat().state.first { it == ChatState.Ready }
        assertEquals(2, commands.size)
        assertEquals("/Users/me/.claude-personal", commands.last().environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun aRememberedConfigurationWhoseAliasIsGoneFallsBackToTheDefault() = test {
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(claudeConfigurationAlias = "claude-gone"))
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals(ClaudeConfiguration.default, controller.configuration.value)
        assertNull(commands.single().environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun switchingTheConfigurationStartsANewConversationOnItsOwnAgent() = test {
        runner.aliases["claude-personal"] = "CLAUDE_CONFIG_DIR=~/.claude-personal claude"
        val controller = controller()
        val first = controller.startChat()
        first.state.first { it == ChatState.Ready }
        onMain { controller.findConfigurations() }
        val personal = controller.configurations.first { it.size == 2 }.last()
        onMain { controller.selectConfiguration(personal) }
        val second = controller.currentChat.value!!
        second.state.first { it == ChatState.Ready }
        assertEquals(listOf(second), controller.chats.value)
        assertEquals(2, commands.size)
        assertNull(commands.first().environment["CLAUDE_CONFIG_DIR"])
        assertEquals("/Users/me/.claude-personal", commands.last().environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun anApiKeyIsKeptAsASecretAndGivenToTheAgentOnly() = test {
        val controller = controller()
        val first = controller.startChat()
        first.state.first { it == ChatState.Ready }
        assertNull(commands.single().environment["ANTHROPIC_API_KEY"])
        val isKept = CompletableDeferred<Unit>()
        onMain { controller.setApiKey(" sk-ant-key ") { isKept.complete(Unit) } }
        isKept.await()
        assertTrue(controller.hasApiKey.value)
        assertEquals("sk-ant-key", secrets.secrets.values.single())
        controller.currentChat.first { it != null && it !== first }!!.state.first { it == ChatState.Ready }
        assertEquals(2, commands.size)
        assertEquals("sk-ant-key", commands.last().environment["ANTHROPIC_API_KEY"])
        assertTrue(settingsDirectory.walk().filter { it.isFile }.none { "sk-ant-key" in it.readText() })
    }

    @Test
    fun aRememberedApiKeyIsUsedAndCanBeRemoved() = test {
        secrets.secrets["anthropic-api-key"] = "sk-ant-old"
        val controller = controller()
        val first = controller.startChat()
        first.state.first { it == ChatState.Ready }
        assertTrue(controller.hasApiKey.value)
        assertEquals("sk-ant-old", commands.single().environment["ANTHROPIC_API_KEY"])
        onMain { controller.removeApiKey() }
        controller.hasApiKey.first { !it }
        controller.currentChat.first { it != null && it !== first }!!.state.first { it == ChatState.Ready }
        assertNull(commands.last().environment["ANTHROPIC_API_KEY"])
        assertEquals(emptyMap(), secrets.secrets)
    }

    @Test
    fun anApiKeyThatCantBeKeptChangesNothing() = test {
        secrets.isWritable = false
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { controller.setApiKey("sk-ant-key") }
        controller.apiKeyFailed.first { it }
        assertFalse(controller.hasApiKey.value)
        assertSame(chat, controller.currentChat.value)
    }

    @Test
    fun aTerminalSignInRunsInTheAgentsConfigurationAndSendsTheRefusedPromptAgain() = test {
        isSignedIn.set(false)
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        launcher.channels.single().pushAuthStatus("none", "Not logged in")
        controller.needsSignIn.first { it }
        onMain { chat.send("hello") }
        chat.transcript.first { it.needsSignIn }
        val method = controller.signInMethods.value.single { it.terminal != null }
        onMain { controller.signIn(method) }
        val session = controller.signIn.first { it != null }!!
        assertEquals(FakeAgent.TERMINAL_LOGIN, terminals.commands.single().program)
        terminals.channels.single().write("Signed in.\n")
        session.output.first { it.text == "Signed in.\n" }
        isSignedIn.set(true)
        terminals.channels.single().exit(0)
        controller.signIn.first { it == null }
        controller.needsSignIn.first { !it }
        chat.transcript.first { it.items.lastOrNull() == TranscriptItem.AgentMessage("Hi there") }
        assertEquals(listOf("hello", "hello"), launcher.agents.single().prompts)
    }

    @Test
    fun aFailedTerminalSignInStaysUntilClosed() = test {
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        val method = controller.signInMethods.first { it.isNotEmpty() }.single { it.terminal != null }
        onMain { controller.signIn(method) }
        val session = controller.signIn.first { it != null }!!
        terminals.channels.single().exit(1)
        assertEquals(1, session.exitCode.first { it != null })
        assertSame(session, controller.signIn.value)
        onMain { controller.closeSignIn() }
        assertNull(controller.signIn.value)
    }

    @Test
    fun aSecondSignInStopsTheFirst() = test {
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        val method = controller.signInMethods.first { it.isNotEmpty() }.single { it.terminal != null }
        onMain {
            controller.signIn(method)
            controller.signIn(method)
        }
        while (terminals.channels.size < 2 || terminals.channels.none { it.isStopped }) delay(10)
        assertEquals(1, terminals.channels.count { it.isStopped })
        delay(100)
        assertNull(controller.signIn.value!!.exitCode.value)
    }

    @Test
    fun anAgentSignInIsAskedOfTheAgent() = test {
        isSignedIn.set(false)
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { chat.send("hello") }
        chat.transcript.first { it.needsSignIn }
        val method = controller.signInMethods.value.single { it.terminal == null && it.variables == null }
        onMain { controller.signIn(method) }
        chat.transcript.first { it.items.lastOrNull() == TranscriptItem.AgentMessage("Hi there") }
        assertEquals(listOf(method.id), launcher.agents.single().authenticated)
    }

    private companion object {
        const val CODEX = """{"id": "codex-acp", "name": "Codex", "version": "2.4.0", "distribution": {"npx": {"package": "@zed-industries/codex-acp@2.4.0"}}}"""
        const val ANTIGRAVITY = """{"id": "antigravity-acp", "name": "Google Antigravity", "version": "1.4.5", "distribution": {"binary": {"darwin-aarch64": {"archive": "https://dl.google.com/agy_acp_server.par", "cmd": "./agy_acp_server.par"}}}}"""
        const val GEMINI = """{"id": "gemini", "name": "Gemini CLI", "version": "1.0.0", "distribution": {"npx": {"package": "@google/gemini-cli@1.0.0"}}}"""
        const val FAST_AGENT = """{"id": "fast-agent", "name": "fast-agent", "version": "0.10.1", "distribution": {"uvx": {"package": "fast-agent-acp==0.10.1"}}}"""
    }
}
