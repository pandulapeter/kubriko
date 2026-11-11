/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.DialogPage
import com.pandulapeter.kubrikoCreator.ui.mcp.AiConnectionSettings
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_send
import kubriko.creator.generated.resources.ic_web
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CreatorSettingsAiPagesTest {
    private val antigravity = RegistryAgent("antigravity-acp", "Google Antigravity", "1.4.5", RegistryAgent.claude.distribution)
    private val personal = AgentConfiguration(AiClient.CLAUDE_CODE, alias = "claude-personal", variant = "personal")

    @Test
    fun theAssistantPageChoosesTheAgentAndItsConfigurationWithoutEnteringACustomOne() = uiTest(width = 960, height = 640) {
        val chosen = mutableListOf<Any>()
        showing {
            dialog(
                isAiEnabled = true,
                initialPage = CreatorSettingsPage.ASSISTANT,
                onAgentSelected = { chosen += it },
                onConfigurationSelected = { chosen += it },
            )
        }
        onNodeWithText("Which of your Claude Code configurations the agent runs as, such as a shell alias with an account of its own.").assertExists()
        saveSnapshot("creator-settings-assistant")
        onNodeWithText("Claude Agent").performClick()
        onNodeWithText("Custom agent…").assertDoesNotExist()
        onNodeWithText("Google Antigravity (untested)").performClick()
        onNodeWithText("Claude Code").performClick()
        onNodeWithText("Claude Code (personal)").performClick()
        assertEquals(listOf<Any>(AgentChoice.Registry(antigravity), personal), chosen)
    }

    @Test
    fun theAiConnectionPageHoldsThePortTheTokenAndTheCommands() = uiTest(width = 960, height = 640) {
        showing { dialog(isAiEnabled = true, initialPage = CreatorSettingsPage.AI_CONNECTION) }
        onNodeWithText("Port").assertExists()
        onNodeWithText("Token").assertExists()
        saveSnapshot("creator-settings-ai-connection")
    }

    @Test
    fun withAiFeaturesOffNeitherPageIsListed() = uiTest(width = 960, height = 640) {
        showing { dialog(isAiEnabled = false, initialPage = CreatorSettingsPage.ASSISTANT) }
        onNodeWithText("Assistant").assertDoesNotExist()
        onNodeWithText("AI connection").assertDoesNotExist()
        onNodeWithText("Theme").assertExists()
    }

    @Composable
    private fun dialog(
        isAiEnabled: Boolean,
        initialPage: CreatorSettingsPage,
        onAgentSelected: (AgentChoice) -> Unit = {},
        onConfigurationSelected: (AgentConfiguration) -> Unit = {},
    ) = CreatorSettingsDialog(
        settings = CreatorSettings(isAiEnabled = isAiEnabled),
        homeFolder = File("/Users/me"),
        onAiEnabledChange = {},
        onThemeChange = {},
        onInterfaceScaleChange = {},
        onDefaultProjectFolderChange = {},
        onFollowingAiChange = {},
        onAiFollowDelayChange = {},
        onTurnReviewShownChange = {},
        onDismiss = {},
        initialPage = initialPage,
        aiPages = listOf(
            DialogPage(key = CreatorSettingsPage.ASSISTANT.key, icon = Res.drawable.ic_send, title = "Assistant") {
                AssistantSettingsPage(
                    agent = AgentChoice.Registry(RegistryAgent.claude),
                    agents = listOf(RegistryAgent.claude, antigravity),
                    customAgentCommand = null,
                    changingFilesUnasked = emptySet(),
                    configuration = AgentConfiguration(AiClient.CLAUDE_CODE),
                    configurations = listOf(AgentConfiguration(AiClient.CLAUDE_CODE), personal),
                    onAgentSelected = onAgentSelected,
                    onConfigurationSelected = onConfigurationSelected,
                    onOpenLog = null,
                )
            },
            DialogPage(key = CreatorSettingsPage.AI_CONNECTION.key, icon = Res.drawable.ic_web, title = "AI connection") {
                AiConnectionSettings(
                    settings = McpSettings(token = "secret"),
                    onPortChange = { true },
                    onRegenerateToken = {},
                    onCommandChange = { _, _ -> },
                )
            },
        ),
    )
}
