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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiConfigFileClient
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.Inheritance
import com.pandulapeter.kubrikoCreator.ui.git.GitPanel
import com.pandulapeter.kubrikoCreator.ui.mcp.AiActivityBar
import com.pandulapeter.kubrikoCreator.ui.mcp.AiConnectionPanel
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneEditorPane
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneNotice
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.project_upgrade_commit_message
import kubriko.creator.generated.resources.scene_preparing
import kubriko.creator.generated.resources.scene_unsupported
import kubriko.creator.generated.resources.workspace_hint
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * An open project: the toolbar, the sidebar with the project's contents, the editor of what is selected, and the console.
 */
@Composable
internal fun WorkspaceScreen(
    controller: WorkspaceController,
    mcp: McpController,
    aiClients: AiClientsController,
    onCloseProject: () -> Unit,
) {
    val resources by controller.resources.collectAsState()
    val sessionState by controller.sessionState.collectAsState()
    val blockTypesController = controller.blockTypes
    val blockTypes = blockTypesController?.types?.collectAsState()?.value.orEmpty()
    val selectedBlockTypeName = blockTypesController?.selectedName?.collectAsState()?.value
    val selectedBlockType = blockTypes.firstOrNull { it.name == selectedBlockTypeName }
    val blockTypesChangedByHand = blockTypesController?.changedByHand?.collectAsState()?.value.orEmpty()
    val sceneEditing by controller.sceneEditing.state.collectAsState()
    var openPanel by remember { mutableStateOf<SidePanel?>(null) }
    val mcpState by mcp.state.collectAsState()
    val gitState by controller.git.state.collectAsState()
    val history by controller.commands.history.collectAsState()
    val aiSteps by controller.commands.aiSteps.collectAsState()
    val latestAiStep = aiSteps.lastOrNull()
    var isFollowingAi by remember { mutableStateOf(true) }
    val projectUpgrade = controller.projectUpgrade.collectAsState().value as? TemplateUpgrade.Needed
    var isUpgradeNoticeDismissed by remember { mutableStateOf(false) }
    val sidebarDragState = rememberSidebarDragState(controller)
    val folderTreeActions = rememberFolderTreeActions(controller)
    val blockTypeFolders = blockTypesController?.folders?.collectAsState()?.value.orEmpty()
    val sceneFolders by controller.sceneFolders.collectAsState()
    val sceneNames = remember(resources) { resources[ResourceGroup.SCENES].orEmpty().map { SceneStore.nameOf(it.file) }.distinct().sorted() }
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val openSceneName = (sceneEditing as? SceneEditingState.Open)?.sceneName
    val folderGroups = remember(blockTypes, blockTypeFolders, selectedBlockTypeName, sceneNames, sceneFolders, openSceneName, blockTypesController) {
        folderTreeGroups(
            types = blockTypes.takeIf { blockTypesController != null },
            typeFolders = blockTypeFolders,
            selectedTypeName = selectedBlockTypeName,
            sceneNames = sceneNames,
            sceneFolders = sceneFolders,
            openSceneName = openSceneName,
        )
    }
    var isReviewingUpgrade by remember { mutableStateOf(false) }
    val upgradeCommitMessage = stringResource(Res.string.project_upgrade_commit_message)
    if (projectUpgrade != null && isReviewingUpgrade) {
        ProjectUpgradeDialog(
            upgrade = projectUpgrade,
            onApply = {
                controller.commands.dispatch(Invocation(UpgradeProject, NoInput))
                isReviewingUpgrade = false
            },
            onCommitFirst = (gitState as? GitState.Ready)?.takeIf { it.changes.isNotEmpty() }?.let {
                { controller.commands.dispatch(Invocation(Commit, Commit.Input(upgradeCommitMessage))) }
            },
            onDismiss = { isReviewingUpgrade = false },
        )
    }
    LaunchedEffect(latestAiStep?.id, latestAiStep?.isRunning) {
        val name = latestAiStep?.actorTypeToFollow() ?: return@LaunchedEffect
        if (isFollowingAi && blockTypesController?.get(name) != null && blockTypesController.selectedName.value != name) {
            controller.sceneEditing.close()
            blockTypesController.select(name)
        }
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
                history = history,
                onUndo = controller.commands::undo,
                onRedo = controller.commands::redo,
                branch = (gitState as? GitState.Ready)?.branch,
                isAiConnected = mcpState is McpServerState.Running,
                openPanel = openPanel,
                onPanelToggled = { panel ->
                    openPanel = panel.takeIf { it != openPanel }
                    if (openPanel == SidePanel.VERSION_CONTROL) controller.git.refresh()
                },
                onPlay = controller::play,
                onStop = controller::stopSession,
                onRefresh = controller::refreshResources,
                onCloseProject = onCloseProject,
            )
            HorizontalDivider()
            AiActivityBar(
                steps = aiSteps,
                isFollowing = isFollowingAi,
                onFollowingChange = { isFollowingAi = it },
                onClick = { openPanel = SidePanel.AI_CONNECTION },
            )
            if (projectUpgrade != null && !isUpgradeNoticeDismissed) {
                ProjectUpgradeNotice(
                    onReview = { isReviewingUpgrade = true },
                    onDismiss = { isUpgradeNoticeDismissed = true },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                ProjectSidebar(
                    modifier = Modifier.width(260.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                    resources = resources,
                    folderGroups = folderGroups,
                    folderActions = folderTreeActions,
                    dragState = sidebarDragState,
                    flashedName = latestAiStep?.sidebarName(),
                    flashKey = latestAiStep?.flashKey,
                )
                VerticalDivider()
                val centerModifier = Modifier.weight(1f).fillMaxHeight()
                when (val state = sceneEditing) {
                    is SceneEditingState.Open -> SceneEditorPane(
                        modifier = centerModifier,
                        state = state,
                        onCommand = { controller.commands.dispatch(it) },
                        actorTypeDragState = sidebarDragState,
                        actorTypes = actorTypes,
                        music = remember(resources) { AudioFiles.of(resources).music },
                        onReload = controller.sceneEditing::reload,
                        onClose = controller.sceneEditing::close,
                    )
                    is SceneEditingState.Unsupported -> SceneNotice(
                        modifier = centerModifier,
                        sceneName = state.scene.name,
                        message = stringResource(Res.string.scene_unsupported, controller.creatorKubrikoVersion, state.projectKubrikoVersion),
                        onClose = controller.sceneEditing::close,
                    )
                    is SceneEditingState.Failed -> SceneNotice(
                        modifier = centerModifier,
                        sceneName = state.scene.name,
                        message = stringResource(state.reason),
                        onClose = controller.sceneEditing::close,
                        onTryAgain = controller.sceneEditing::reload,
                    )
                    is SceneEditingState.Preparing -> Box(centerModifier) {
                        ProgressScreen(stringResource(Res.string.scene_preparing, state.scene.name))
                    }
                    SceneEditingState.Closed -> Unit
                }
                if (sceneEditing == SceneEditingState.Closed && blockTypesController != null && selectedBlockType != null) {
                    BlockTypeEditor(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        definition = selectedBlockType,
                        scope = remember(selectedBlockType, blockTypes, resources) {
                            BlockScope.of(
                                definition = selectedBlockType,
                                types = blockTypes,
                                sceneNames = sceneNames,
                                audioFiles = AudioFiles.of(resources),
                            )
                        },
                        sprites = remember(resources) { SpriteFiles.of(resources).paths },
                    inheritance = remember(selectedBlockType, blockTypes) { Inheritance.of(selectedBlockType, blockTypes) },
                        isChangedByHand = selectedBlockType.name in blockTypesChangedByHand,
                        onCommand = { controller.commands.dispatch(it) },
                        highlight = latestAiStep?.highlightIn(selectedBlockType.name),
                    )
                } else if (sceneEditing == SceneEditingState.Closed) {
                    WorkspaceHint(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (openPanel == SidePanel.AI_CONNECTION) {
                    VerticalDivider()
                    AiConnectionPanel(
                        modifier = Modifier.width(300.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                        settings = mcp.settings.collectAsState().value,
                        state = mcpState,
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
                }
                if (openPanel == SidePanel.VERSION_CONTROL) {
                    VerticalDivider()
                    GitPanel(
                        modifier = Modifier.width(300.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                        state = gitState,
                        onRefresh = controller.git::refresh,
                        onStartVersionControl = { controller.commands.dispatch(Invocation(StartVersionControl, NoInput)) },
                        onCommit = { message -> controller.commands.dispatch(Invocation(Commit, Commit.Input(message))) },
                        onDiscard = { change -> controller.commands.dispatch(Invocation(DiscardChanges, DiscardChanges.Input(change.paths))) },
                    )
                }
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

@Composable
private fun WorkspaceHint(
    modifier: Modifier,
) = Box(
    modifier = modifier.padding(32.dp),
    contentAlignment = Alignment.Center,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            modifier = Modifier.size(40.dp),
            painter = painterResource(Res.drawable.ic_actor_type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.widthIn(max = 420.dp),
            text = stringResource(Res.string.workspace_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
