/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import com.agentclientprotocol.model.AuthMethodId
import com.agentclientprotocol.model.AvailableCommand
import com.agentclientprotocol.model.SessionConfigGroupId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigSelectGroup
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.agentclientprotocol.model.SessionConfigValueId
import com.agentclientprotocol.model.ToolCallId
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_list_scenes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssistantPanelDecisionsTest {
    private val scope = CoroutineScope(SupervisorJob())
    private val chat = AssistantChat(parentScope = scope, start = { _, _ -> awaitCancellation() }, end = {})

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    private val commands = listOf(
        AvailableCommand("compact", "Compact the conversation"),
        AvailableCommand("context", "Show the context"),
        AvailableCommand("review", "Review"),
    )

    @Test
    fun slashCommandsAreOfferedWhileTheirNameIsTyped() {
        assertEquals(commands.take(2), slashCompletions("/co", commands))
        assertEquals(commands, slashCompletions("/", commands))
        assertEquals(listOf(commands[0]), slashCompletions("/COMP", commands))
        assertEquals(emptyList(), slashCompletions("/compact", commands))
        assertEquals(emptyList(), slashCompletions("/compact now", commands))
        assertEquals(emptyList(), slashCompletions("compact", commands))
    }

    @Test
    fun groupedChoicesAreFlattened() {
        val sonnet = SessionConfigSelectOption(SessionConfigValueId("sonnet"), "Sonnet")
        val opus = SessionConfigSelectOption(SessionConfigValueId("opus"), "Opus")
        val option = SessionConfigOption.select(
            id = "model",
            name = "Model",
            currentValue = "sonnet",
            options = SessionConfigSelectOptions.Grouped(
                listOf(
                    SessionConfigSelectGroup(SessionConfigGroupId("fast"), "Fast", listOf(sonnet)),
                    SessionConfigSelectGroup(SessionConfigGroupId("smart"), "Smart", listOf(opus)),
                ),
            ),
        )
        assertEquals(listOf(sonnet, opus), choicesOf(option))
    }

    @Test
    fun creatorToolCallsAreTitledAsTheirCommands() {
        assertEquals(Res.string.command_list_scenes, creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__list_scenes")))
        assertNull(creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("2"), "mcp__kubriko-creator__no_such_tool")))
        assertNull(creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("3"), "Read Game.kt")))
    }

    @Test
    fun conversationsAreNamedByTheirFirstLineCutAtAWord() {
        assertEquals("Make the Ball faster", conversationTitle("Make the Ball faster\nand red"))
        assertEquals(
            "Make the Ball start twice as fast, and the Paddle a little slower. Keep the…",
            conversationTitle("Make the Ball start twice as fast, and the Paddle a little slower. Keep the answer short."),
        )
        assertEquals("a".repeat(80) + "…", conversationTitle("a".repeat(90)))
    }

    @Test
    fun theAccountMenuOffersSignInOnlyWithoutAnApiKey() {
        val methods = listOf(SignInMethod(AuthMethodId("claude-ai-login"), "Claude Subscription"))
        assertEquals(listOf(AccountAction.SignIn(methods.single()), AccountAction.UseApiKey), accountActions(methods, hasApiKey = false, canKeepApiKey = true))
        assertEquals(listOf(AccountAction.UseApiKey, AccountAction.RemoveApiKey), accountActions(methods, hasApiKey = true, canKeepApiKey = true))
        assertEquals(listOf(AccountAction.SignIn(methods.single())), accountActions(methods, hasApiKey = false, canKeepApiKey = false))
    }

    @Test
    fun theSignInAreaShowsARunningSignInFirst() {
        assertEquals(SignInAreaContent.TERMINAL, signInAreaContent(isSigningIn = true, isEnteringApiKey = true, needsSignIn = true))
        assertEquals(SignInAreaContent.API_KEY, signInAreaContent(isSigningIn = false, isEnteringApiKey = true, needsSignIn = true))
        assertEquals(SignInAreaContent.METHODS, signInAreaContent(isSigningIn = false, isEnteringApiKey = false, needsSignIn = true))
        assertNull(signInAreaContent(isSigningIn = false, isEnteringApiKey = false, needsSignIn = false))
        assertEquals(SignInAreaContent.VARIABLES, signInAreaContent(isSigningIn = false, isEnteringApiKey = true, needsSignIn = true, isEnteringVariables = true))
        assertEquals(SignInAreaContent.TERMINAL, signInAreaContent(isSigningIn = true, isEnteringApiKey = false, needsSignIn = true, isEnteringVariables = true))
    }

    @Test
    fun theAgentMenuOffersTheRegistrysAgentsThenTheCustomOneThenEnteringOne() {
        val claude = RegistryAgent.claude
        assertEquals(
            listOf(AgentMenuItem.Agent(AgentChoice.Registry(claude)), AgentMenuItem.EnterCustom),
            agentMenuItems(listOf(claude), customAgentCommand = null),
        )
        assertEquals(
            listOf(AgentMenuItem.Agent(AgentChoice.Registry(claude)), AgentMenuItem.Agent(AgentChoice.Custom("goose acp")), AgentMenuItem.EnterCustom),
            agentMenuItems(listOf(claude), customAgentCommand = "goose acp"),
        )
        assertEquals(
            listOf(AgentMenuItem.Agent(AgentChoice.Registry(claude)), AgentMenuItem.Agent(AgentChoice.Custom("goose acp")), AgentMenuItem.OpenLog),
            agentMenuItems(listOf(claude), customAgentCommand = "goose acp", canEnterCustom = false, hasLog = true),
        )
    }

    @Test
    fun downloadsShowHowFarTheyGotInMegabytes() {
        assertEquals(0.5f, downloadFraction(50, 100))
        assertEquals(1f, downloadFraction(150, 100))
        assertEquals(0f, downloadFraction(10, 0))
        assertEquals(String.format("%.1f", 12.3), megabytes(12_345_678))
    }

    @Test
    fun usageReadsAsPeopleSayIt() {
        assertEquals(42, contextPercent(84_000, 200_000))
        assertEquals(100, contextPercent(300, 200))
        assertEquals("950", shortTokenCount(950))
        assertEquals("200k", shortTokenCount(200_000))
        assertEquals("1M", shortTokenCount(1_000_000))
        assertEquals("$" + String.format("%.2f", 0.5), formattedCost(0.5, "usd"))
        assertEquals(String.format("%.2f", 2.0) + " EUR", formattedCost(2.0, "EUR"))
    }

    @Test
    fun variablesCanBeSavedOnceEveryOneThatIsntOptionalHasAValue() {
        val variables = listOf(SignInMethod.Variable("KEY"), SignInMethod.Variable("REGION", isOptional = true))
        assertEquals(false, canSaveVariables(variables, mapOf("KEY" to " ")))
        assertEquals(true, canSaveVariables(variables, mapOf("KEY" to "abc")))
    }

    @Test
    fun conversationsAreListedTalkedInLastFirstWithOpenOnesBeforeEarlierOnesOfTheSameMoment() {
        val earlier = listOf(
            SavedConversation(project = "/projects/breakout", agentId = "claude-acp", id = "b", title = "B", updatedAt = 30),
            SavedConversation(project = "/projects/breakout", agentId = "claude-acp", id = "d", title = "D", updatedAt = 10),
        )
        val open = listOf(20L, 30L).map { ConversationChoice.Open(chat = chat, lastMessageAt = it) }
        assertEquals(
            listOf(30L to "open", 30L to "b", 20L to "open", 10L to "d"),
            conversationChoices(open, earlier).map { choice ->
                choice.lastMessageAt to when (choice) {
                    is ConversationChoice.Open -> "open"
                    is ConversationChoice.Earlier -> choice.conversation.id
                }
            },
        )
    }
}
