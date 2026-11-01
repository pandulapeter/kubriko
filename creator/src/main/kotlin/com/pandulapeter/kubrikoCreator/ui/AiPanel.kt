/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiConfigFileClient
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantPanel
import com.pandulapeter.kubrikoCreator.ui.gameMap.GameMapPanel
import com.pandulapeter.kubrikoCreator.ui.mcp.AiConnectionPanel

/**
 * Everything about working with AI in one panel, since the two ways of doing it solve the same problem: the in-app
 * assistant, or an AI tool of the user's own connected over MCP. What the AI is told about the game sits behind a button.
 *
 * @param digest What the AI is told about the game, read only while that view is shown.
 */
@Composable
internal fun AiPanel(
    view: AiPanelView,
    onViewSelected: (AiPanelView) -> Unit,
    assistant: AssistantController,
    mcp: McpController,
    aiClients: AiClientsController,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    digest: ProjectDigest?,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    AiPanelHeader(
        view = view,
        onViewSelected = onViewSelected,
    )
    HorizontalDivider()
    val content = Modifier.fillMaxWidth().weight(1f)
    when (view) {
        AiPanelView.ASSISTANT -> AssistantPanel(
            modifier = content,
            controller = assistant,
            aiSteps = aiSteps,
            onShowStep = onShowStep,
        )
        AiPanelView.OWN_TOOL -> AiConnectionPanel(
            modifier = content,
            settings = mcp.settings.collectAsState().value,
            state = mcp.state.collectAsState().value,
            clientSetups = aiClients.setups.collectAsState().value,
            configFileClients = AiConfigFileClient.entries,
            clientStatuses = aiClients.statuses.collectAsState().value,
            calls = mcp.calls.collectAsState().value,
            onEnabledChange = mcp::setEnabled,
            onRefreshClients = aiClients::refresh,
            onConnect = aiClients::connect,
            onDisconnect = aiClients::disconnect,
            onConnectConfigFile = aiClients::connectConfigFile,
            onDisconnectConfigFile = aiClients::disconnectConfigFile,
            onPortChange = mcp::setPort,
            onRegenerateToken = mcp::regenerateToken,
            onCommandChange = aiClients::setCommand,
        )
        AiPanelView.GAME_MAP -> GameMapPanel(
            modifier = content,
            digest = digest,
            onTypeSelected = onTypeSelected,
            onSceneSelected = onSceneSelected,
        )
    }
}
