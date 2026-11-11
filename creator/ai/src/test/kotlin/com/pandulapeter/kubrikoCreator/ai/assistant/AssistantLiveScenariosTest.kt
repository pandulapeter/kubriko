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

import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.context.screenplayTexts
import com.pandulapeter.kubrikoCreator.project.GameNotes
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageText
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Real conversations with the real Claude Agent on the Breakout fixture, through the assistant's own chat and tools,
 * without a window: what the panel's features do with an agent rather than with [FakeAgent]. Each scenario costs requests,
 * so they only run when `KUBRIKO_CREATOR_CLAUDE_AGENT` is `true`; `KUBRIKO_CREATOR_CLAUDE_CONFIG_DIR` picks a Claude Code
 * configuration other than the default one. Answers vary, so each checks what the project became rather than the words.
 */
class AssistantLiveScenariosTest {

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
    fun aRequestOfManyChangesIsPlannedFirstAndCarriedOutOnceApprovedWithASmallerModel() = live { session ->
        val chat = session.chat()
        chat.ask(
            "Give the player three lives: keep them on a Manager type, show them on screen, take one away whenever the Ball " +
                "leaves the bottom edge of the screen, and start the Scene again once none are left.",
        )
        val models = chat.configOptions.value.filterIsInstance<SessionConfigOption.Select>().map { option -> option.currentValue.value to option.choices().map { "${it.value.value}: ${it.name} (${it.description})" } }
        println("Plan: ${chat.transcript.value.plan.map { "${it.status} ${it.content}" }}\nAnswer: ${chat.lastAnswer()}\nOptions: $models")
        println("Tool calls: ${chat.transcript.value.items.filterIsInstance<TranscriptItem.ToolCall>().map { it.title }}")
        assertTrue(chat.transcript.value.isPlanAwaitingApproval, "The turn didn't end on a plan to approve.")
        assertEquals(null, session.executor.history.value.nextUndoCommandName, "Something changed before the plan was approved.")
        val smaller = SmallerModel.of(chat.configOptions.value)
        println("Smaller model: $smaller")
        chat.ask("The plan is approved: go ahead.", model = smaller, timeout = 15.minutes)
        println("Answer: ${chat.lastAnswer()}")
        assertTrue(session.executor.history.value.nextUndoCommandName != null, "Nothing changed once the plan was approved.")
        smaller?.let { model ->
            val current = chat.configOptions.value.filterIsInstance<SessionConfigOption.Select>().first { it.id == model.option }.currentValue
            assertEquals(model.value, current)
        }
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
    fun aFinishedRequestIsNotedForTheNextConversation() = live { session ->
        session.chat().ask("Make the Ball faster: double the velocity it starts with.")
        val summary = assertNotNull(session.lastSummary, "Nothing was noted for the next conversation.")
        println("Noted for next time: $summary")
        val last = SavedConversation(session.projectPath, RegistryAgent.claude.id, "earlier", "Make the Ball faster", 0, summary = summary)
        val next = session.chat(projectContext = LastConversation.withProject(null, last))
        next.ask("What did we change last time? Answer in one sentence, and change nothing.")
        println("Heard: ${next.lastAnswer()}")
        assertTrue(Regex("fast|speed|velocity").containsMatchIn(next.lastAnswer().lowercase()), "The next conversation didn't hear what was done.")
    }

    @Test
    fun aQuestionAnsweredAfterMoreThanAMinuteReachesTheAgent() = live { session ->
        val questions = session.context.questions
        val answering = launch(Dispatchers.Default) {
            val question = questions.pending.first { it.isNotEmpty() }.first()
            delay(ANSWER_DELAY)
            questions.answer(question.id, "Teal")
        }
        val chat = session.chat()
        chat.ask("Ask me with ask_user which color I like, offering red and blue, then reply with my answer alone.")
        answering.cancel()
        assertContains(chat.lastAnswer().lowercase(), "teal")
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
    fun kubrikosApiIsLookedUpThroughCreatorsToolRatherThanReadFromItsSources() = live { session ->
        val chat = session.chat()
        chat.ask(
            "Write a Kotlin Block at the end of the Ball's collision with Brick that shakes the camera: move it by a random offset of up to " +
                "four scene units with the ViewportManager.",
        )
        val calls = chat.transcript.value.items.filterIsInstance<TranscriptItem.ToolCall>()
        calls.forEach { println("Tool call: ${it.kind} ${it.creatorToolName ?: it.title}") }
        assertContains(calls.mapNotNull { it.creatorToolName }, "get_kubriko_api")
        val ownReadsOfKubriko = calls.filter { it.creatorToolName == null && it.title.contains("kubriko", ignoreCase = true) && !it.title.contains(session.projectPath) }
        assertEquals(emptyList(), ownReadsOfKubriko.map { it.title }, "Kubriko's sources were read with the agent's own tools.")
        val collision = session.context.ball().events.first { it.type == EventType.COLLISION && it.otherActorType == "Brick" }
        assertContains((collision.blocks.last() as BlockDefinition.Kotlin).code, "CameraPosition")
    }

    @Test
    fun theChipsLinksAreFollowedAndTheScenesPictureIsRead() = live { session ->
        val chat = session.chat()
        val event = AttentionChip.Event("Ball", isManager = false, index = BRICK_COLLISION, type = EventType.COLLISION)
        chat.ask("Add a Log Block saying \"here\" at the end of this Event.", chips = listOf(event))
        assertEquals(BlockDefinition.Log("here"), session.context.ball().events[BRICK_COLLISION].blocks.last())
        val logs = session.context.blockTypes!!.types.value.sumOf { type -> type.events.sumOf { it.blocks.count { block -> block is BlockDefinition.Log } } }
        assertEquals(1, logs, "The Log Block went elsewhere too.")
        chat.ask("How many gold bricks does this Scene place? Answer with the number alone, and change nothing.", chips = listOf(AttentionChip.Scene("level-1")))
        println("Gold bricks: ${chat.lastAnswer()}")
        assertTrue(Regex("\\b(4|four)\\b", RegexOption.IGNORE_CASE).containsMatchIn(chat.lastAnswer()), "The Scene wasn't read.")
        chat.ask(
            "How many white squares does the picture of this Scene show? Answer with the number alone, and change nothing.",
            chips = listOf(AttentionChip.SceneScreen("level-1")),
            picture = squares(SQUARES),
        )
        println("Squares: ${chat.lastAnswer()}")
        assertTrue(Regex("\\b($SQUARES|seven)\\b", RegexOption.IGNORE_CASE).containsMatchIn(chat.lastAnswer()), "The picture wasn't read.")
    }

    @Test
    fun aSettledConventionIsAddedToTheGamesNotes() = live { session ->
        val chat = session.chat()
        chat.ask(
            "Let's settle this for the whole game: every Log Block's message starts with its type's name in brackets, like \"[Ball] hit\". " +
                "Add one saying hit at the end of the Ball's collision with Brick.",
        )
        val notes = withContext(Dispatchers.IO) { GameNotes.read(session.projectDirectory) }
        println("Notes: $notes")
        assertNotNull(notes, "Nothing was added to the game's notes.")
        assertTrue(Regex("\\[Ball]|bracket", RegexOption.IGNORE_CASE).containsMatchIn(notes), "The convention isn't in the game's notes.")
        val collision = session.context.ball().events[BRICK_COLLISION]
        assertEquals(BlockDefinition.Log("[Ball] hit"), collision.blocks.last())
    }

    @Test
    fun theGameExplorerAnswersThroughCreatorsReadTools() = live { session ->
        val chat = session.chat()
        val readsWhileExploring = launch(Dispatchers.Default) {
            chat.transcript.first { transcript -> transcript.items.filterIsInstance<TranscriptItem.ToolCall>().hasReadsDuringAHandOver() }
        }
        // Named outright: whether a question takes enough reads to hand over is the model's call, while what can break here is the subagent's wiring.
        chat.ask(
            "Ask the ${ClaudeAgentOptions.EXPLORER_NAME} subagent to list every way the player gains points or loses a life, naming the type and " +
                "Event of each, then pass its answer on. Change nothing.",
        )
        chat.transcript.value.items.filterIsInstance<TranscriptItem.ToolCall>().forEach { println("Tool call: ${it.kind} ${it.status} ${it.title}") }
        assertTrue(readsWhileExploring.isCompleted, "The subagent wasn't asked, or read nothing through Creator's tools while it ran.")
        readsWhileExploring.cancel()
        assertContains(chat.lastAnswer(), "Ball")
        assertEquals(null, session.executor.history.value.nextUndoCommandName)
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

    private fun live(isBlank: Boolean = false, block: suspend TestScope.(LiveSession) -> Unit) = liveTest(if (isBlank) 90.minutes else 25.minutes) {
        withLiveSession(isBlank) { block(it) }
    }

    /**
     * A PNG of [count] white squares apart on black, standing in for a picture of the Scene.
     */
    private fun squares(count: Int): ByteArray {
        val image = BufferedImage(SQUARE_SIZE * (count * 2 + 1), SQUARE_SIZE * 3, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        repeat(count) { graphics.fillRect(SQUARE_SIZE * (it * 2 + 1), SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE) }
        graphics.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    /**
     * Whether Creator's reads ran while a hand-over to a subagent (an agent's own call of the thinking kind) hadn't ended:
     * the main agent waits for the subagent meanwhile, so those reads are the subagent's.
     */
    private fun List<TranscriptItem.ToolCall>.hasReadsDuringAHandOver(): Boolean {
        val handOver = indexOfFirst { it.kind == ToolKind.THINK && it.creatorToolName == null && it.status != ToolCallStatus.COMPLETED && it.status != ToolCallStatus.FAILED }
        return handOver >= 0 && drop(handOver + 1).any { it.creatorToolName?.startsWith("read_") == true }
    }

    private fun CommandContext.ball() = blockTypes!!.get("Ball") as ActorTypeDefinition

    private companion object {
        /**
         * How many times the writers' room is asked to go on when it stops before building anything, waiting to be told.
         */
        const val FOLLOW_UPS = 2

        /**
         * The index of the Ball's collision with Brick among its Events in [BreakoutFixture].
         */
        val BRICK_COLLISION = BreakoutFixture.ball.events.indexOfFirst { it.type == EventType.COLLISION && it.otherActorType == "Brick" }

        /**
         * How many squares the stand-in picture of the Scene shows, and how wide each is, in pixels.
         */
        const val SQUARES = 7
        const val SQUARE_SIZE = 40

        /**
         * How long the user takes to answer a question: longer than Claude Code waits for a tool call by default.
         */
        val ANSWER_DELAY = 70.seconds
    }
}
