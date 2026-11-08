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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.context.screenplayTexts
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Real conversations with the real Claude Agent on the Breakout fixture, through the assistant's own chat and tools,
 * without a window: what the panel's features do with an agent rather than with [FakeAgent]. Each scenario costs requests,
 * so they only run when `KUBRIKO_CREATOR_CLAUDE_AGENT` is `true`; `KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code
 * configuration other than the default one. Answers vary, so each checks what the project became rather than the words.
 */
class AssistantLiveScenariosTest {
    private val isEnabled = System.getenv("KUBRIKO_CREATOR_CLAUDE_AGENT") == "true"

    @Test
    fun aChangeIsMadeThroughCreatorsToolsAndUndoneFromItsCard() = live { session ->
        val chat = session.chat()
        chat.ask("Make the Ball faster: double the velocity it starts with.")
        val start = session.context.ball().events.first { it.type == EventType.ADDED_TO_SCENE }.blocks.filterIsInstance<BlockDefinition.SetVelocity>().single()
        assertTrue(hypot(start.x, start.y) > hypot(200f, -300f), "The Ball starts at $start.")
        val changes = assertNotNull(chat.transcript.value.items.filterIsInstance<TranscriptItem.TurnChanges>().lastOrNull(), "No card of what changed.")
        withContext(session.mainThread) { chat.undoChanges(changes.sinceRevision) }
        withContext(Dispatchers.Default) {
            withTimeout(1.minutes) { chat.transcript.first { transcript -> transcript.items.any { it is TranscriptItem.TurnReview && it.state != TranscriptItem.TurnReview.State.OFFERED } } }
        }
        assertEquals(BreakoutFixture.ball, session.context.ball())
    }

    @Test
    fun aQuestionIsAnsweredFromTheModelWithoutChangingAnything() = live { session ->
        val chat = session.chat()
        chat.ask("Which types change the score? Answer in one sentence, and change nothing.")
        assertContains(chat.lastAnswer(), "Ball")
        assertEquals(null, session.executor.history.value.nextUndoCommandName)
        assertTrue(session.toolServer.calls.value.isNotEmpty(), "No tool was called.")
    }

    @Test
    fun aConversationIsContinuedAfterTheAgentStartsAgain() = live { session ->
        val chat = session.chat()
        chat.ask("Remember the word marmalade. Reply with one word.")
        val id = assertNotNull(chat.conversationId.value)
        session.restartAgent()
        val resumed = session.chat(resuming = id)
        withContext(Dispatchers.Default) { withTimeout(2.minutes) { resumed.state.first { it == ChatState.Ready } } }
        assertTrue(resumed.transcript.value.items.isNotEmpty(), "Nothing of the conversation came back.")
        resumed.ask("Which word did I ask you to remember? Reply with that word alone.")
        assertContains(resumed.lastAnswer().lowercase(), "marmalade")
    }

    @Test
    fun theHealthChecksAreReadAndAChangeIsCheckedBeforeItIsMade() = live { session ->
        val chat = session.chat()
        chat.ask("Call get_health_checks and tell me what it found in one sentence. Then add a Log Block saying \"hit\" at the end of the Ball's collision with Brick, trying it with check_change before making it.")
        val tools = session.toolServer.calls.value.map { it.toolName }
        println("Tools called: $tools")
        assertContains(tools, "get_health_checks")
        assertContains(tools, "check_change")
        val collision = session.context.ball().events.first { it.type == EventType.COLLISION && it.otherActorType == "Brick" }
        assertEquals(BlockDefinition.Log("hit"), collision.blocks.last())
    }

    @Test
    fun aPitchInTheWritersRoomBecomesAPlayableSliceWithPagesThatKeepTheRules() = live(isBlank = true) { session ->
        val questions = session.context.questions
        // Whatever the room asks is answered with its first suggestion, as a user who agrees with everything would.
        val answering = launch(Dispatchers.Default) {
            val answered = HashSet<Long>()
            questions.pending.collect { pending ->
                pending.filter { answered.add(it.id) }.forEach { questions.answer(it.id, it.answers.firstOrNull() ?: "Your call: whatever fits the game best.") }
            }
        }
        val chat = session.chat(rules = AssistantSystemPrompt.WRITERS_ROOM)
        chat.ask("Let's make a game in the writers' room. My idea: a sheepdog herds sheep into a pen before the sun sets, while foxes try to snatch them.", timeout = 20.minutes)
        repeat(FOLLOW_UPS) {
            if (!session.hasPlayableSlice()) chat.ask("Go on: build the first playable slice.", timeout = 20.minutes)
        }
        answering.cancel()
        val pages = session.context.screenplayTexts().pages
        assertTrue(pages.isNotEmpty(), "No page was written.")
        pages.forEach { (name, text) ->
            assertNotNull(PageText.titleOf(text), "The page $name has no title.")
            assertNotNull(PageText.summaryOf(text), "The page $name has no summary.")
            assertEquals(emptyList(), ScreenplayStyle.problemsOf(text, PageNaming.pathOf(name)), "The page $name breaks the writing rules.")
        }
        assertTrue(session.hasPlayableSlice(), "No type was made, or no Scene places an Actor.")
    }

    /**
     * Whether the project has a type and a Scene placing an Actor: the least a slice that plays needs.
     */
    private suspend fun LiveSession.hasPlayableSlice() = context.blockTypes!!.types.value.isNotEmpty() &&
        withContext(Dispatchers.IO) { context.scenes.names().any { context.scenes.actors(it)?.isNotEmpty() == true } }

    private fun live(isBlank: Boolean = false, block: suspend TestScope.(LiveSession) -> Unit) {
        assumeTrue(isEnabled)
        runTest(timeout = if (isBlank) 90.minutes else 15.minutes) {
            val session = newSession(isBlank)
            try {
                block(session)
            } finally {
                session.close()
            }
        }
    }

    /**
     * Sends [text] and waits for the turn to end, in real time: [runTest]'s own clock would let the timeout pass at once.
     */
    private suspend fun AssistantChat.ask(text: String, timeout: Duration = 5.minutes) = withContext(Dispatchers.Default) {
        withTimeout(timeout) {
            state.first { it == ChatState.Ready }
            send(text)
            isBusy.first { it }
            isBusy.first { !it }
        }
    }

    private fun AssistantChat.lastAnswer() = transcript.value.items.filterIsInstance<TranscriptItem.AgentMessage>().lastOrNull()?.text.orEmpty()

    private fun CommandContext.ball() = blockTypes!!.get("Ball") as ActorTypeDefinition

    /**
     * The real Claude Agent with Kubriko Creator's tools on a fresh Breakout fixture.
     */
    private inner class LiveSession {
        val mainThread = Dispatchers.Default
        private val scope = CoroutineScope(SupervisorJob() + mainThread)
        private val projectDirectory = createTempDirectory().toFile()
        private val npx = File(assertNotNull(ShellProcessRunner().locate("npx"), "Node.js isn't installed."))
        private val configDirectory = System.getenv("KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR")
        lateinit var context: CommandContext
        lateinit var executor: CommandExecutor
        val toolServer = McpToolServer(executor = { executor }, callDispatcher = mainThread, version = "1.0.0")
        private var assistant = newAssistant()

        @OptIn(EditorMutation::class)
        fun TestScope.prepare(isBlank: Boolean) {
            context = commandContext(*if (isBlank) emptyArray() else BreakoutFixture.types.toTypedArray(), rootDirectory = projectDirectory)
            if (!isBlank) context.scenes.write("level-1", BreakoutFixture.level)
            executor = CommandExecutor(context, testScope())
        }

        private fun newAssistant() = Assistant(
            agent = AcpAgent(
                command = RegistryAgent.npxCommand(RegistryAgent.claude.distribution as AgentDistribution.Npx, npx, listOfNotNull(configDirectory?.let { "CLAUDE_CONFIG_DIR" to it }).toMap()),
                launcher = ProcessAgentLauncher(projectDirectory),
                clientInfo = AcpAgent.clientInfo("1.0.0"),
                scope = scope,
                ioDispatcher = Dispatchers.IO,
            ),
            toolServer = toolServer,
            ioDispatcher = Dispatchers.IO,
            profile = AgentProfile.claude,
            systemPrompt = AssistantSystemPrompt::withProject,
        )

        suspend fun chat(resuming: String? = null, rules: String? = null) = withContext(mainThread) {
            val owner = assistant
            AssistantChat(
                parentScope = scope,
                start = { permissions, onHistory ->
                    if (resuming == null) {
                        owner.newConversation(projectDirectory, permissions, rules = rules)
                    } else {
                        owner.resumeConversation(projectDirectory, resuming, permissions, onHistory, rules)
                    }
                },
                end = owner::closeConversation,
                toolsIdOf = owner::toolsIdOf,
                changeLog = { executor.changeLog },
                undoTurn = executor::undoTurn,
            )
        }

        fun restartAgent() {
            assistant.close()
            assistant = newAssistant()
        }

        fun close() {
            assistant.close()
            scope.cancel()
            projectDirectory.deleteRecursively()
        }
    }

    private fun TestScope.newSession(isBlank: Boolean) = LiveSession().apply { prepare(isBlank) }

    private companion object {
        /**
         * How many times the writers' room is asked to go on when it stops before building anything, waiting to be told.
         */
        const val FOLLOW_UPS = 2
    }
}
