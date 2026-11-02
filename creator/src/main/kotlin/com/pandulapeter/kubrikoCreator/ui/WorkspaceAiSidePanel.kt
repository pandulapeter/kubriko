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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.captureGameScreen

/**
 * The workspace's AI panel, after the splitter that resizes it, with the Game map computed only while it is shown.
 *
 * @param openSceneName The Scene shown in the center, if any.
 * @param selectedBlockType The type shown in the center, if any.
 */
@Composable
internal fun WorkspaceAiSidePanel(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    aiSteps: List<AiStep>,
    openSceneName: String?,
    selectedBlockType: BlockTypeDefinition?,
    sessionState: SessionState,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    val gameMap = if (uiState.aiPanelView == AiPanelView.GAME_MAP) controller.gameMap.gameMap.collectAsState().value else null
    val console = controller.console.collectAsState()
    val acceptsImages = assistant.acceptsImages.collectAsState()
    val attention by remember(openSceneName, selectedBlockType?.name, selectedBlockType is ManagerTypeDefinition, sessionState) {
        derivedStateOf(structuralEqualityPolicy()) {
            AttentionChip.of(
                openScene = openSceneName,
                selectedType = selectedBlockType?.name,
                isSelectedTypeManager = selectedBlockType is ManagerTypeDefinition,
                console = console.value,
                canSendGameScreen = sessionState is SessionState.Running && acceptsImages.value,
            )
        }
    }
    AiPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        view = uiState.aiPanelView,
        onViewSelected = uiState::selectAiPanelView,
        assistant = assistant,
        mcp = mcp,
        aiClients = aiClients,
        aiSteps = aiSteps,
        onShowStep = { step ->
            uiState.revisit(step, latestStepId = aiSteps.lastOrNull()?.id)
            when (val target = step.target) {
                is CommandTarget.BlockType -> controller.selection.selectType(target.name)
                is CommandTarget.Scene -> controller.selection.openScene(target.name)
                else -> Unit
            }
        },
        digest = gameMap?.digest,
        findings = gameMap?.findings.orEmpty(),
        onTypeSelected = { controller.selection.selectType(it) },
        onSceneSelected = { controller.selection.openScene(it) },
        attention = attention,
        captureScreen = controller::captureGameScreen,
    )
}
