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
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.scenes.ListScenes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssistantPanelDecisionsTest {
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
        assertEquals(ListScenes.title, creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("1"), "mcp__kubriko-creator__list_scenes")))
        assertNull(creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("2"), "mcp__kubriko-creator__no_such_tool")))
        assertNull(creatorToolTitleOf(TranscriptItem.ToolCall(ToolCallId("3"), "Read Game.kt")))
    }

    @Test
    fun conversationsAreNamedByTheirFirstLineCutAtAWord() {
        assertEquals("Make the Ball faster", conversationTitle("Make the Ball faster\nand red"))
        assertEquals("Make the Ball start twice as fast. Keep…", conversationTitle("Make the Ball start twice as fast. Keep the answer short."))
        assertEquals("a".repeat(40) + "…", conversationTitle("a".repeat(50)))
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
    }
}
