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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import com.pandulapeter.kubrikoCreator.workspace.toRejection

/**
 * An open project: the toolbar, the sidebar with the project's contents, the editor of what is selected, and the console.
 */
@Composable
internal fun WorkspaceScreen(
    controller: WorkspaceController,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    onCloseProject: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isAiEnabled = LocalIsAiEnabled.current
    val uiState = rememberWorkspaceUiState(controller, mcp)
    val resources by controller.resources.collectAsState()
    val sessionState by controller.sessionState.collectAsState()
    val blockTypesController = controller.blockTypes
    val blockTypes = blockTypesController?.types?.collectAsState()?.value.orEmpty()
    val selection by controller.selection.current.collectAsState()
    val selectedBlockTypeName = (selection as? WorkspaceSelection.BlockType)?.name
    val selectedBlockType = blockTypes.firstOrNull { it.name == selectedBlockTypeName }
    val sceneEditing by controller.sceneEditing.state.collectAsState()
    val mcpState by mcp.state.collectAsState()
    val aiSteps by controller.commands.aiSteps.collectAsState()
    val latestAiStep = aiSteps.lastOrNull()
    val shownAiStep = shownAiStep(latestAiStep, uiState.revisitedAiStep)?.takeIf { isAiEnabled }
    val projectUpgrade = controller.projectUpgrade.collectAsState().value as? TemplateUpgrade.Needed
    val readOnlyReason = (controller.formatStatus.collectAsState().value as? ProjectFormatStatus.ReadOnly)?.toRejection()
    val sidebarDragState = rememberSidebarDragState(controller)
    val sceneNames = remember(resources) { resources[ResourceGroup.SCENES].orEmpty().map { SceneStore.nameOf(it.file) }.distinct().sorted() }
    val openSceneName = (selection as? WorkspaceSelection.Scene)?.name
    WorkspaceDialogs(controller = controller, uiState = uiState, projectUpgrade = projectUpgrade)
    LaunchedEffect(latestAiStep?.id, latestAiStep?.isRunning) {
        val name = latestAiStep?.actorTypeToFollow() ?: return@LaunchedEffect
        if (isAiEnabled && uiState.isFollowingAi && selection != WorkspaceSelection.BlockType(name)) controller.selection.selectType(name)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .historyShortcutTarget()
                .onKeyEvent { event -> event.toHistoryStep()?.let { isUndo -> if (isUndo) controller.commands.undo() else controller.commands.redo() } != null },
        ) {
            WorkspaceToolbar(
                sessionState = sessionState,
                history = controller.commands.history,
                onUndo = controller.commands::undo,
                onRedo = controller.commands::redo,
                gitState = controller.git.state,
                isAiConnected = mcpState is McpServerState.Running,
                openPanel = uiState.openPanel,
                onPanelToggled = { panel -> if (uiState.togglePanel(panel)) controller.git.refresh() },
                onPlay = controller::play,
                onStop = controller::stopSession,
                onRefresh = controller::refreshResources,
                onOpenProjectSettings = uiState::showProjectSettings,
                onOpenSettings = onOpenSettings,
                onCloseProject = onCloseProject,
            )
            HorizontalDivider()
            WorkspaceNotices(uiState = uiState, aiSteps = aiSteps, readOnlyReason = readOnlyReason, isUpgradeNeeded = projectUpgrade != null)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                ProjectSidebar(
                    modifier = Modifier.sidePanel(PanelWidths.SIDEBAR),
                    resources = resources,
                    folderGroups = rememberFolderTreeGroups(controller, blockTypes, selectedBlockTypeName, sceneNames, openSceneName),
                    folderActions = rememberFolderTreeActions(controller),
                    dragState = sidebarDragState,
                    flashedName = shownAiStep?.step?.sidebarName(),
                    flashKey = shownAiStep?.flashKey,
                )
                VerticalDivider()
                WorkspaceCenter(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selection = selection,
                    sceneEditing = sceneEditing,
                    blockTypes = blockTypes,
                    resources = resources,
                    sceneNames = sceneNames,
                    sceneDescriptions = controller.scenes.descriptions.current.collectAsState().value,
                    changedByHand = blockTypesController?.changedByHand?.collectAsState()?.value.orEmpty(),
                    shownAiStep = shownAiStep,
                    actorTypeDragState = sidebarDragState,
                    onCommand = { controller.commands.dispatch(it) },
                    onCommandIfValid = controller::dispatchIfValid,
                    onTypeSelected = { controller.selection.selectType(it) },
                    onPlayScene = { controller.play(firstScene = it) },
                    onReloadScene = controller.sceneEditing::reload,
                    onCloseScene = controller.selection::clear,
                    onSceneViewFailed = controller.sceneEditing::onViewFailed,
                )
                if (uiState.openPanel == SidePanel.AI && isAiEnabled) {
                    WorkspaceAiSidePanel(
                        controller = controller,
                        uiState = uiState,
                        mcp = mcp,
                        aiClients = aiClients,
                        assistant = assistant,
                        aiSteps = aiSteps,
                        openSceneName = openSceneName,
                        selectedBlockType = selectedBlockType,
                        sessionState = sessionState,
                    )
                }
                if (uiState.openPanel == SidePanel.VERSION_CONTROL) WorkspaceGitSidePanel(controller)
            }
            HorizontalDivider()
            WorkspaceConsole(
                entries = controller.console,
                onClear = controller::clearConsole,
            )
        }
        SidebarDragPreview(sidebarDragState)
    }
}
