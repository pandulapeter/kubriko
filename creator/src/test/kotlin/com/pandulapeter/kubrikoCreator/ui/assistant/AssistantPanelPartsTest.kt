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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.agentclientprotocol.model.AuthMethodId
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentDownload
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationUsage
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.MarkdownText
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The assistant panel's parts, rendered offscreen as the app shows them.
 */
@OptIn(ExperimentalTestApi::class)
class AssistantPanelPartsTest {
    private val antigravity = AgentChoice.Registry(RegistryAgent("antigravity-acp", "Google Antigravity", "1.4.5", RegistryAgent.claude.distribution))

    @Test
    fun whatGoesWithTheMessageIsOneLineAndEachPartIsAsItIsByDefaultUntilTurned() = uiTest {
        showing {
            var toggled by remember { mutableStateOf(emptySet<String>()) }
            AttentionLine(
                chips = listOf(
                    AttentionChip.Scene("level-1"),
                    AttentionChip.SceneScreen,
                    AttentionChip.Problem("Unresolved reference", "Ball.kt:3"),
                    AttentionChip.GameScreen,
                ),
                toggled = toggled,
                onToggle = { chip -> toggled = if (chip.key in toggled) toggled - chip.key else toggled + chip.key },
            )
        }
        onNodeWithText("With your message: Scene level-1 · The latest problem · Game screenshot").assertExists()
        saveSnapshot("attention-line")
        onNodeWithText("With your message", substring = true).performClick()
        saveSnapshot("attention-line-menu")
        onNodeWithText("The latest problem").performClick()
        onNodeWithText("With your message: Scene level-1 · Game screenshot").assertExists()
        onNodeWithText("With your message", substring = true).performClick()
        onNodeWithText("Scene picture").performClick()
        onNodeWithText("With your message: Scene level-1 · Scene picture · Game screenshot").assertExists()
    }

    @Test
    fun theEventShownIsNamedByItsTitle() = uiTest {
        showing {
            AttentionLine(
                chips = listOf(AttentionChip.BlockType("Ball", isManager = false), AttentionChip.Event("Ball", isManager = false, index = 1, type = EventType.EVERY_FRAME)),
                toggled = emptySet(),
                onToggle = {},
            )
        }
        onNodeWithText("With your message: Actor type Ball · Event Every frame").assertExists()
    }

    @Test
    fun usageSaysHowFullTheContextIsWhatItCostAndWhoIsBilled() = uiTest {
        showing { ConversationUsageLine(ConversationUsage(84_000, 200_000, 0.12, "USD"), isBilledToApiKey = true) }
        onNodeWithText("Context 42% of 200k tokens · $0.12 so far · Billed to your API key").assertExists()
        saveSnapshot("usage")
    }

    @Test
    fun aDownloadShowsHowFarItGot() = uiTest {
        showing {
            CompositionLocalProvider(LocalAgent provides antigravity) {
                Column {
                    AssistantStarting(AgentDownload("Google Antigravity", 42_000_000, 120_000_000))
                    AssistantStarting(null)
                }
            }
        }
        onNodeWithText("Downloading Google Antigravity… ${megabytes(42_000_000)} of ${megabytes(120_000_000)} MB").assertExists()
        onNodeWithText("Starting Google Antigravity… The first time, it is downloaded, which can take a minute.").assertExists()
        saveSnapshot("starting")
    }

    @Test
    fun withoutNodeTheFailureOffersToDownloadIt() = uiTest {
        val clicks = mutableListOf<String>()
        showing {
            AssistantFailure(
                state = ChatState.Failed(FailureReason.NODE_MISSING),
                onTryAgain = { clicks += "retry" },
                onOpenLog = { clicks += "log" },
                onInstallNode = { clicks += "node" },
                nodeInstallFailure = "The download of Node.js failed (HTTP 503).",
            )
        }
        onNodeWithText("Claude Agent needs Node.js", substring = true).assertExists()
        onNodeWithText("The download of Node.js failed (HTTP 503).").assertExists()
        onNodeWithText("Download Node.js for Kubriko Creator").performClick()
        onNodeWithText("Open the agent's log").performClick()
        onNodeWithText("Try again").performClick()
        assertEquals(listOf("node", "log", "retry"), clicks)
        saveSnapshot("failure-node-missing")
    }

    @Test
    fun aFailureLeadsWithASentenceAndShowsTheAgentsWordsUnderIt() = uiTest {
        showing {
            CompositionLocalProvider(LocalAgent provides antigravity) {
                AssistantFailure(ChatState.Failed(FailureReason.AGENT_NOT_STARTED, listOf("Internal error")), onTryAgain = {}, onOpenLog = null)
            }
        }
        onNodeWithText("Google Antigravity couldn't be started.").assertExists()
        onNodeWithText("Internal error").assertExists()
        onNodeWithText("Open the agent's log").assertDoesNotExist()
        saveSnapshot("failure-not-started")
    }

    @Test
    fun theAgentMenuMarksAnUntestedAgentAndOffersTheOthers() = uiTest {
        val chosen = mutableListOf<AgentChoice>()
        showing {
            AgentMenu(
                agent = antigravity,
                agents = listOf(RegistryAgent.claude, antigravity.agent),
                customAgentCommand = "goose acp",
                changingFilesUnasked = setOf(AgentChoice.Custom("goose acp").identity),
                onSelected = { chosen += it },
                onEnterCustomAgent = {},
                onOpenLog = {},
            )
        }
        onNodeWithText("Google Antigravity (untested)").performClick()
        onNodeWithText("goose (untested, changes files without asking)").assertExists()
        onNodeWithText("Custom agent…").assertExists()
        onNodeWithText("Open the agent's log").assertExists()
        saveSnapshot("agent-menu")
        onNodeWithText("Claude Agent").performClick()
        assertEquals(listOf<AgentChoice>(AgentChoice.Registry(RegistryAgent.claude)), chosen)
    }

    @Test
    fun variablesAreSavedOnceEveryOneThatIsntOptionalIsGiven() = uiTest {
        val saved = mutableListOf<Map<String, String>>()
        val method = SignInMethod(
            id = AuthMethodId("gemini-api-key"),
            name = "Gemini API key",
            variables = listOf(SignInMethod.Variable("GEMINI_API_KEY", "API key"), SignInMethod.Variable("REGION", isSecret = false, isOptional = true)),
            link = "https://example.com/keys",
        )
        showing { SignInVariablesForm(method = method, hasFailed = false, onSave = { saved += it }, onCancel = {}) }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Where to find them").assertExists()
        onAllNodes(hasSetTextAction())[0].performTextInput("secret")
        saveSnapshot("sign-in-variables")
        onNodeWithText("Save").performClick()
        assertEquals(listOf(mapOf("GEMINI_API_KEY" to "secret")), saved)
    }

    @Test
    fun aCustomAgentIsEnteredAsACommandLine() = uiTest {
        val saved = mutableListOf<String>()
        showing { CustomAgentForm(initialCommand = "goose", onSave = { saved += it }, onCancel = {}) }
        onAllNodes(hasSetTextAction())[0].performTextInput(" acp")
        saveSnapshot("custom-agent")
        onNodeWithText("Save").performClick()
        assertEquals(1, saved.size)
        assertEquals(true, saved.single().contains("acp"))
    }

    @Test
    fun aTableInAReplyIsDrawnAsOne() = uiTest {
        showing { MarkdownText("Types:\n\n| Type | Speed |\n|------|-------|\n| Ball | 240 |\n| Paddle | 400 |") }
        onNodeWithText("Speed").assertExists()
        onNodeWithText("Paddle").assertExists()
        assertEquals(0, onAllNodesWithText("|", substring = true).fetchSemanticsNodes().size)
        saveSnapshot("markdown-table")
    }
}
