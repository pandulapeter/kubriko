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

import com.agentclientprotocol.model.AuthMethodId
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.testing.FakeProcessRunner
import com.pandulapeter.kubrikoCreator.testing.IdleTrackingDispatcher
import com.pandulapeter.kubrikoCreator.testing.awaitIdle
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeFalse
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AssistantControllerTest {
    private val mainExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainThread = IdleTrackingDispatcher(mainExecutor)
    private val io = IdleTrackingDispatcher(Dispatchers.IO)
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
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")

    @AfterTest
    fun tearDown() {
        controllers.forEach { it.close() }
        launcher.close()
        scope.cancel()
        mainExecutor.close()
        settingsDirectory.deleteRecursively()
    }

    private fun controller(
        registry: AgentRegistryStore? = null,
        installer: BinaryAgentInstaller? = null,
        node: NodeRuntime? = null,
        isAiEnabled: StateFlow<Boolean> = MutableStateFlow(true),
        secretStore: SecretStore = secrets,
        scope: CoroutineScope = this.scope,
        ioDispatcher: CoroutineDispatcher = io,
        clock: () -> Long = { 1_000 },
    ) = AssistantController(
        scope = scope,
        runner = runner,
        storage = AssistantStorage(
            settings = AssistantSettingsStore(File(settingsDirectory, "assistant.json")),
            secrets = secretStore,
            conversations = SavedConversationStore(File(settingsDirectory, "conversations.json")),
        ),
        signInLauncher = terminals,
        ioDispatcher = ioDispatcher,
        createAssistant = { command, _, profile ->
            profiles += profile
            commands += command
            Assistant(
                agent = AcpAgent(command, launcher, AcpAgent.clientInfo("1.0.0"), scope, io, conversationCloseWait = 300.milliseconds),
                toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
                ioDispatcher = io,
                profile = profile,
                agentFolders = File(settingsDirectory, "agents"),
            )
        },
        sources = AgentSources(registry = registry, installer = installer, node = node),
        home = "/Users/me",
        clock = clock,
        isAiEnabled = isAiEnabled,
    ).also { controllers += it }

    private fun registry(vararg agents: String) = AgentRegistryStore(File(settingsDirectory, "agent-registry.json"), "darwin-aarch64") {
        """{"agents": [${agents.joinToString(",")}]}"""
    }

    /**
     * Waits for the settings saved off the main thread to satisfy [predicate].
     */
    private suspend fun awaitSavedSettings(predicate: (AssistantSettings) -> Boolean) {
        while (!predicate(AssistantSettingsStore(File(settingsDirectory, "assistant.json")).load())) delay(10)
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
    fun theWritersRoomStartsAConversationOfItsOwnWithItsRulesAndTheIdea() = blockingTest {
        val controller = controller()
        val first = controller.startChat()
        first.state.first { it == ChatState.Ready }
        onMain { controller.startWritersRoom("hello") }
        val room = controller.currentChat.value!!
        assertNotSame(first, room)
        room.transcript.first { it.items.lastOrNull() == TranscriptItem.AgentMessage("Hi there") }
        val agent = launcher.agents.single()
        assertEquals(listOf("hello"), agent.prompts)
        val told = agent.leadingTexts + agent.newSessions.map { it._meta.toString() }
        assertEquals(1, told.count { "This conversation is the writers' room" in it }, "Only the writers' room is told its rules.")
    }

    @Test
    fun conversationsNeedAnOpenProject() = blockingTest {
        val controller = controller()
        onMain { controller.newChat() }
        assertNull(controller.currentChat.value)
    }

    @Test
    fun theRegistryLoadsWhenItsLoadingRunsBeforeTheControllerIsBuilt() = blockingTest {
        val registry = registry(CODEX)
        registry.refresh()
        val failures = CopyOnWriteArrayList<Throwable>()
        val immediateScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, failure -> failures += failure })
        try {
            val controller = controller(registry, scope = immediateScope, ioDispatcher = Dispatchers.Unconfined)
            assertEquals(emptyList(), failures)
            assertEquals(listOf("Claude Agent", "Codex"), controller.agents.value.map { it.name })
        } finally {
            immediateScope.cancel()
        }
    }

    @Test
    fun withAiFeaturesOffNoConversationOpensNoAgentStartsAndTheRegistryIsNotFetched() = blockingTest {
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
        awaitIdle(mainThread, io)
        assertNull(controller.currentChat.value)
        assertTrue(commands.isEmpty())
        assertEquals(0, fetches.get())
        isAiEnabled.value = true
        withTimeout(5.seconds) { while (fetches.get() == 0) delay(10) }
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals(1, commands.size)
    }

    @Test
    fun withAiFeaturesOffChoosingAnAgentOrGivingAKeyChangesNothing() = blockingTest {
        runner.aliases["claude-personal"] = "CLAUDE_CONFIG_DIR=~/.claude-personal claude"
        val registry = registry(CODEX)
        registry.refresh()
        val controller = controller(registry, isAiEnabled = MutableStateFlow(false))
        onMain { controller.findConfigurations() }
        val personal = controller.configurations.first { it.size == 2 }.last()
        val codex = controller.agents.first { it.size == 2 }.last()
        val method = SignInMethod(AuthMethodId("variables"), "Variables", variables = listOf(SignInMethod.Variable("FAKE_API_KEY")))
        onMain {
            controller.setProject(projectDirectory)
            controller.selectAgent(AgentChoice.Registry(codex))
            controller.selectConfiguration(personal)
            controller.setApiKey("sk-ant-key")
            controller.setVariables(method, mapOf("FAKE_API_KEY" to "secret"))
        }
        awaitIdle(mainThread, io)
        assertTrue(controller.agent.value.isClaude)
        assertEquals(ClaudeConfiguration.default, controller.configuration.value)
        assertFalse(File(settingsDirectory, "assistant.json").exists())
        assertEquals(emptyMap(), secrets.secrets)
        assertTrue(launcher.commands.isEmpty())
    }

    @Test
    fun turningAiFeaturesOffEndsTheConversationsAndTheAgent() = blockingTest {
        val isAiEnabled = MutableStateFlow(true)
        val controller = controller(isAiEnabled = isAiEnabled)
        controller.startChat().state.first { it == ChatState.Ready }
        isAiEnabled.value = false
        withTimeout(5.seconds) { while (controller.currentChat.value != null || launcher.channels.any { !it.isStopped }) delay(10) }
        assertTrue(controller.chats.value.isEmpty())
    }

    @Test
    fun aConversationRunsClaudeAgentThroughNpxInTheProject() = blockingTest {
        val chat = controller().startChat()
        chat.state.first { it == ChatState.Ready }
        assertEquals(File("/opt/tools/bin/npx").path, commands.single().program)
        assertEquals((RegistryAgent.claude.distribution as AgentDistribution.Npx).packageName, commands.single().arguments.last())
        assertEquals(listOf(AgentProfile.claude), profiles)
        assertEquals(projectDirectory.absolutePath, launcher.agents.single().newSessions.single().cwd)
    }

    @Test
    fun aCustomAgentRunsItsCommandWithTheFallbacksAndIsRemembered() = blockingTest {
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
        awaitSavedSettings { it.agentId == AgentChoice.Custom.ID }
        assertEquals(AgentChoice.Custom("GOOSE_MODE=chat goose acp"), controller().agent.value)
        onMain { controller.selectAgent(AgentChoice.Registry(RegistryAgent.claude)) }
        awaitSavedSettings { it.agentId == null }
        assertEquals("GOOSE_MODE=chat goose acp", controller().customAgentCommand)
        assertTrue(controller().agent.value.isClaude)
    }

    @Test
    fun aRememberedRegistryAgentRunsThroughItsDistributionWithTheFallbacks() = blockingTest {
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
    fun aBinaryAgentIsDownloadedThenRunFromItsFolder() = blockingTest {
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
    fun aBinaryAgentThatCantBeDownloadedSaysWhy() = blockingTest {
        val registry = registry(ANTIGRAVITY)
        registry.refresh()
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(agentId = "antigravity-acp"))
        val installer = BinaryAgentInstaller(File(settingsDirectory, "tools"), download = { _, _, _ -> throw java.io.IOException("Offline.") })
        val failed = controller(registry, installer).startChat().state.first { it is ChatState.Failed }
        assertEquals(ChatState.Failed(FailureReason.AGENT_NOT_STARTED, listOf("Offline.")), failed)
    }

    @Test
    fun variablesGivenForSigningInAreKeptAndTheAgentStartsAgainWithThem() = blockingTest {
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
        awaitSavedSettings { it.agentVariables["codex-acp"] == listOf("FAKE_API_KEY") }
        val afterRestart = controller(registry)
        afterRestart.startChat().state.first { it == ChatState.Ready }
        assertEquals("secret", commands.last().environment["FAKE_API_KEY"])
    }

    @Test
    fun rememberedVariablesAreReadOffTheMainThread() = blockingTest {
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
    fun anAgentRunByUvxNeedsUv() = blockingTest {
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
    fun aDeletedConversationIsForgottenForGoodAndANewOneTakesItsPlace() = blockingTest {
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { chat.send("hello") }
        chat.isBusy.first { !it }
        val store = SavedConversationStore(File(settingsDirectory, "conversations.json"))
        assertEquals(listOf("hello"), store.of(projectDirectory, "claude-acp").map { it.title })
        onMain { controller.delete(chat) }
        val replacement = controller.currentChat.value
        assertNotNull(replacement)
        assertNotSame(chat, replacement)
        assertEquals(listOf(replacement), controller.chats.value)
        withTimeout(5_000) { while (store.of(projectDirectory, "claude-acp").isNotEmpty()) delay(10) }
    }

    @Test
    fun deletingTheShownConversationShowsTheOneTalkedInLast() = blockingTest {
        val time = AtomicLong()
        val controller = controller(clock = time::incrementAndGet)
        val earlier = controller.startChat()
        earlier.state.first { it == ChatState.Ready }
        onMain { earlier.send("hello") }
        earlier.isBusy.first { !it }
        val later = controller.startChat()
        later.state.first { it == ChatState.Ready }
        onMain { later.send("hi") }
        later.isBusy.first { !it }
        val shown = controller.startChat()
        onMain { controller.delete(shown) }
        assertSame(later, controller.currentChat.value)
    }

    @Test
    fun aNewConversationIsOnlyStartedWhenNoOpenOneIsEmpty() = blockingTest {
        val controller = controller()
        val empty = controller.startChat()
        val talkedIn = controller.startChat()
        assertSame(empty, talkedIn)
        empty.state.first { it == ChatState.Ready }
        onMain { empty.send("hello") }
        empty.isBusy.first { !it }
        val next = controller.startChat()
        assertNotSame(empty, next)
        onMain { controller.select(empty) }
        assertSame(next, controller.startChat())
        assertEquals(listOf(empty, next), controller.chats.value)
    }

    @Test
    fun aContinuedConversationKeepsWhenItsLastMessageWasSaid() = blockingTest {
        val earlier = controller()
        val chat = earlier.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { chat.send("hello") }
        chat.isBusy.first { !it }
        val controller = controller()
        onMain { controller.setProject(projectDirectory) }
        val saved = controller.savedConversations.first { it.isNotEmpty() }.single()
        onMain { controller.resume(saved) }
        assertEquals(saved.updatedAt, controller.currentChat.value?.lastMessageAt?.value)
        assertEquals(emptyList(), controller.savedConversations.value)
    }

    @Test
    fun aConversationOfAnEarlierRunIsContinuedWithItsHistory() = blockingTest {
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
    fun withoutNodeTheConversationSaysSo() = blockingTest {
        runner.installed.clear()
        val chat = controller().startChat()
        assertEquals(ChatState.Failed(FailureReason.NODE_MISSING), chat.state.first { it is ChatState.Failed })
    }

    @Test
    fun downloadingNodeStartsTheConversationsThatHadNoneWithIt() = blockingTest {
        assumeFalse(isWindows)
        runner.installed.clear()
        val releases = TestNodeReleases(settingsDirectory)
        val node = releases.runtime()
        val controller = controller(node = node)
        val chat = controller.startChat()
        assertEquals(ChatState.Failed(FailureReason.NODE_MISSING), chat.state.first { it is ChatState.Failed })
        onMain { controller.installNode() }
        chat.state.first { it == ChatState.Ready }
        assertEquals(node.installedNpx()?.path, commands.last().program)
        assertNull(controller.nodeInstallFailure.value)
    }

    @Test
    fun aFailedNodeDownloadIsSaidAndTheConversationStaysWithoutIt() = blockingTest {
        assumeFalse(isWindows)
        runner.installed.clear()
        val controller = controller(node = TestNodeReleases(settingsDirectory).runtime(sums = { "0000  node-v24.11.1-darwin-arm64.tar.gz" }))
        val chat = controller.startChat()
        chat.state.first { it is ChatState.Failed }
        onMain { controller.installNode() }
        assertTrue(controller.nodeInstallFailure.first { it != null }!!.isNotEmpty())
        assertEquals(ChatState.Failed(FailureReason.NODE_MISSING), chat.state.value)
    }

    @Test
    fun closingTheProjectEndsItsConversations() = blockingTest {
        val controller = controller()
        val chat = controller.startChat()
        chat.state.first { it == ChatState.Ready }
        onMain { controller.setProject(File("/projects/other")) }
        assertEquals(emptyList(), controller.chats.value)
        assertNull(controller.currentChat.value)
    }

    @Test
    fun openingAProjectStartsNoAgentThatHasNotWorkedBefore() = blockingTest {
        SavedConversationStore(File(settingsDirectory, "conversations.json")).save(SavedConversation("/projects/other", "codex-acp", "earlier", "Earlier", 500))
        val controller = controller()
        onMain { controller.setProject(projectDirectory) }
        awaitIdle(mainThread, io)
        assertNull(controller.currentChat.value)
        assertTrue(commands.isEmpty())
    }

    @Test
    fun openingAProjectStartsTheAgentThatWorkedBeforeWithoutThePanel() = blockingTest {
        SavedConversationStore(File(settingsDirectory, "conversations.json")).save(SavedConversation("/projects/other", RegistryAgent.claude.id, "earlier", "Earlier", 500))
        val controller = controller()
        onMain { controller.setProject(projectDirectory) }
        controller.currentChat.first { it != null }!!.state.first { it == ChatState.Ready }
        assertEquals(1, controller.chats.value.size)
        assertEquals(1, commands.size)
        onMain { controller.newChat() }
        assertEquals(1, controller.chats.value.size)
    }

    @Test
    fun withAiFeaturesOffOpeningAProjectStartsNoAgentThatWorkedBefore() = blockingTest {
        SavedConversationStore(File(settingsDirectory, "conversations.json")).save(SavedConversation("/projects/other", RegistryAgent.claude.id, "earlier", "Earlier", 500))
        val controller = controller(isAiEnabled = MutableStateFlow(false))
        onMain { controller.setProject(projectDirectory) }
        awaitIdle(mainThread, io)
        assertNull(controller.currentChat.value)
        assertTrue(commands.isEmpty())
    }

    @Test
    fun switchingAgentsWhileAConversationIsClosingStartsTheNewAgentAndListsItsConversations() = blockingTest {
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
    fun aConversationOpenedWhileTheProjectChangesStartsOnAFreshAgent() = blockingTest {
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        val chat = onMain {
            controller.setProject(File("/projects/other"))
            controller.newChat()
            controller.currentChat.value!!
        }
        chat.state.first { it == ChatState.Ready }
        withTimeout(5.seconds) { while (!launcher.channels.first().isStopped) delay(10) }
        awaitIdle(mainThread, io)
        assertEquals(ChatState.Ready, chat.state.value)
        assertEquals(2, launcher.channels.size)
    }

    @Test
    fun twoSwitchesInARowEndOnTheLastAgent() = blockingTest {
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
    fun anAliasesConfigurationIsUsedAndRemembered() = blockingTest {
        runner.aliases["claude-personal"] = "CLAUDE_CONFIG_DIR=~/.claude-personal claude"
        val controller = controller()
        onMain { controller.findConfigurations() }
        val personal = controller.configurations.first { it.size == 2 }.last()
        onMain { controller.selectConfiguration(personal) }
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals("/Users/me/.claude-personal", commands.last().environment["CLAUDE_CONFIG_DIR"])
        awaitSavedSettings { it.claudeConfigurationAlias == "claude-personal" }
        val restarted = controller()
        assertEquals("claude-personal", restarted.configuration.value.alias)
        restarted.startChat().state.first { it == ChatState.Ready }
        assertEquals(2, commands.size)
        assertEquals("/Users/me/.claude-personal", commands.last().environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun aRememberedConfigurationWhoseAliasIsGoneFallsBackToTheDefault() = blockingTest {
        AssistantSettingsStore(File(settingsDirectory, "assistant.json")).save(AssistantSettings(claudeConfigurationAlias = "claude-gone"))
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        assertEquals(ClaudeConfiguration.default, controller.configuration.value)
        assertNull(commands.single().environment["CLAUDE_CONFIG_DIR"])
    }

    @Test
    fun switchingTheConfigurationStartsANewConversationOnItsOwnAgent() = blockingTest {
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
    fun anApiKeyIsKeptAsASecretAndGivenToTheAgentOnly() = blockingTest {
        val controller = controller()
        val first = controller.startChat()
        first.state.first { it == ChatState.Ready }
        assertNull(commands.single().environment["ANTHROPIC_API_KEY"])
        val isKept = CompletableDeferred<Unit>()
        onMain { controller.setApiKey(" sk-ant-key ") { isKept.complete(Unit) } }
        isKept.await()
        controller.hasApiKey.first { it }
        assertEquals("sk-ant-key", secrets.secrets.values.single())
        controller.currentChat.first { it != null && it !== first }!!.state.first { it == ChatState.Ready }
        assertEquals(2, commands.size)
        assertEquals("sk-ant-key", commands.last().environment["ANTHROPIC_API_KEY"])
        assertTrue(settingsDirectory.walk().filter { it.isFile }.none { "sk-ant-key" in it.readText() })
    }

    @Test
    fun aRememberedApiKeyIsUsedAndCanBeRemoved() = blockingTest {
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
    fun anAgentWithoutAClaudeAccountReportsNoApiKeyOrConfigurations() = blockingTest {
        secrets.secrets["anthropic-api-key"] = "sk-ant-old"
        runner.aliases["claude-personal"] = "CLAUDE_CONFIG_DIR=~/.claude-personal claude"
        val registry = registry(CODEX)
        registry.refresh()
        val controller = controller(registry)
        controller.startChat().state.first { it == ChatState.Ready }
        onMain { controller.findConfigurations() }
        controller.configurations.first { it.size == 2 }
        controller.hasApiKey.first { it }
        val codex = controller.agents.first { it.size == 2 }.last()
        onMain { controller.selectAgent(AgentChoice.Registry(codex)) }
        controller.agent.first { !it.isClaude }
        controller.hasApiKey.first { !it }
        controller.configurations.first { it.isEmpty() }
        assertFalse(controller.canKeepApiKey)
        onMain { controller.selectAgent(AgentChoice.Registry(RegistryAgent.claude)) }
        controller.hasApiKey.first { it }
        assertTrue(controller.canKeepApiKey)
    }

    @Test
    fun anApiKeyThatCantBeKeptChangesNothing() = blockingTest {
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
    fun aTerminalSignInRunsInTheAgentsConfigurationAndSendsTheRefusedPromptAgain() = blockingTest {
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
    fun aFailedTerminalSignInStaysUntilClosed() = blockingTest {
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
    fun aSecondSignInStopsTheFirst() = blockingTest {
        val controller = controller()
        controller.startChat().state.first { it == ChatState.Ready }
        val method = controller.signInMethods.first { it.isNotEmpty() }.single { it.terminal != null }
        onMain {
            controller.signIn(method)
            controller.signIn(method)
        }
        while (terminals.channels.size < 2 || terminals.channels.none { it.isStopped }) delay(10)
        assertEquals(1, terminals.channels.count { it.isStopped })
        awaitIdle(mainThread, io)
        assertNull(controller.signIn.value!!.exitCode.value)
    }

    @Test
    fun anAgentSignInIsAskedOfTheAgent() = blockingTest {
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
