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
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.workspace.GameMap
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * The workspace's AI panel, after the splitter that resizes it, with the Game map computed only while it is shown.
 *
 * @param acceptsImages Whether the assistant's agent takes images, so the game screen can be offered.
 * @param gameMap The Game map, collected only while that view is shown.
 * @param console The workspace's console, which the assistant offers problems from.
 * @param openSceneName The Scene shown in the center, if any.
 * @param selectedBlockType The type shown in the center, if any.
 * @param captureScreen Takes a screenshot of the running game.
 */
@Composable
internal fun WorkspaceAiSidePanel(
    uiState: WorkspaceUiState,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantPanelModel,
    acceptsImages: StateFlow<Boolean>,
    gameMap: StateFlow<GameMap?>,
    console: StateFlow<List<ConsoleEntry>>,
    aiSteps: List<AiStep>,
    openSceneName: String?,
    selectedBlockType: BlockTypeDefinition?,
    sessionState: SessionState,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onPageSelected: (String) -> Unit,
    captureScreen: suspend () -> ByteArray?,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    val shownGameMap = if (uiState.aiPanelView == AiPanelView.GAME_MAP) gameMap.collectAsState().value else null
    val consoleEntries = console.collectAsState()
    val isAcceptingImages = acceptsImages.collectAsState()
    val attention by remember(openSceneName, selectedBlockType?.name, selectedBlockType is ManagerTypeDefinition, sessionState) {
        derivedStateOf(structuralEqualityPolicy()) {
            AttentionChip.of(
                openScene = openSceneName,
                selectedType = selectedBlockType?.name,
                isSelectedTypeManager = selectedBlockType is ManagerTypeDefinition,
                console = consoleEntries.value,
                canSendGameScreen = sessionState is SessionState.Running && sessionState.target == PlayTarget.DESKTOP && isAcceptingImages.value,
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
            uiState.revisit(step, latestStepId = aiSteps.newestChange()?.id)
            when (val target = step.target) {
                is CommandTarget.BlockType -> onTypeSelected(target.name)
                is CommandTarget.Scene -> onSceneSelected(target.name)
                is CommandTarget.Page -> onPageSelected(target.name)
                else -> Unit
            }
        },
        digest = shownGameMap?.digest,
        findings = shownGameMap?.findings.orEmpty(),
        onTypeSelected = onTypeSelected,
        onSceneSelected = onSceneSelected,
        attention = attention,
        captureScreen = captureScreen,
    )
}
