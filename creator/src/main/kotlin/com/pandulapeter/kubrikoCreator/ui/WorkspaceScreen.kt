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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.components.AnimatedSidePanel
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import com.pandulapeter.kubrikoCreator.ui.components.rememberPanelWidth
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettingsPage
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import com.pandulapeter.kubrikoCreator.workspace.captureGameScreen
import com.pandulapeter.kubrikoCreator.workspace.toRejection

/**
 * An open project: the toolbar, the sidebar with the project's contents, the editor of what is selected, and the console.
 *
 * @param panelLayout The side panels' widths, which the user drags at their edges.
 */
@Composable
internal fun WorkspaceScreen(
    controller: WorkspaceController,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    panelLayout: PanelLayout,
    onCloseProject: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState = rememberWorkspaceUiState(controller)
    CompositionLocalProvider(LocalDescriptionWriter provides rememberDescriptionWriter(assistant, uiState::showAssistant)) {
        WorkspaceContent(
            controller = controller,
            uiState = uiState,
            mcp = mcp,
            aiClients = aiClients,
            assistant = assistant,
            panelLayout = panelLayout,
            onCloseProject = onCloseProject,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun WorkspaceContent(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    panelLayout: PanelLayout,
    onCloseProject: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isAiEnabled = LocalIsAiEnabled.current
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
    WorkspaceDialogsOf(controller = controller, uiState = uiState, projectUpgrade = projectUpgrade)
    FollowingAiSteps(controller = controller, uiState = uiState, latestAiStep = latestAiStep, selection = selection)
    RefreshingGitOnWindowFocus(controller.git::refresh)
    var columnHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { columnHeight = with(density) { it.height.toDp() } }
                .historyShortcutTarget()
                .onKeyEvent { event -> event.toHistoryStep()?.let { isUndo -> if (isUndo) controller.commands.undo() else controller.commands.redo() } != null },
        ) {
            WorkspaceToolbarOf(
                controller = controller,
                uiState = uiState,
                sessionState = sessionState,
                isAiConnected = mcpState is McpServerState.Running,
                onOpenSettings = onOpenSettings,
                onCloseProject = onCloseProject,
            )
            HorizontalDivider()
            WorkspaceNotices(uiState = uiState, aiSteps = aiSteps, readOnlyReason = readOnlyReason, isUpgradeNeeded = projectUpgrade != null)
            val isAiPanelShown = uiState.openPanel == SidePanel.AI && isAiEnabled
            val isGitPanelShown = uiState.openPanel == SidePanel.VERSION_CONTROL
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                val row = PanelRow(maxWidth, panelsInRow = if (isAiPanelShown || isGitPanelShown) 2 else 1)
                val sidebarWidth = rememberPanelWidth(Panel.SIDEBAR, panelLayout)
                Row(Modifier.fillMaxSize()) {
                    ProjectSidebar(
                        modifier = Modifier.sidePanel(sidebarWidth.widthIn(row)),
                        resources = resources,
                        folderGroups = rememberFolderTreeGroupsOf(controller, blockTypes, selectedBlockTypeName, sceneNames, openSceneName),
                        folderActions = rememberFolderTreeActions(controller),
                        dragState = sidebarDragState,
                        flashedName = shownAiStep?.step?.sidebarName(),
                        flashKey = shownAiStep?.flashKey,
                    )
                    PanelSplitter(sidebarWidth, row)
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
                        panelLayout = panelLayout,
                        onCommand = controller.commands::dispatch,
                        currentBlockType = { blockTypesController?.get(it) },
                        onCommandIfValid = controller::dispatchIfValid,
                        onTypeSelected = { controller.selection.selectType(it) },
                        onPlayScene = { controller.play(firstScene = it) },
                        onReloadScene = controller.sceneEditing::reload,
                        onCloseScene = controller.selection::clear,
                        onSceneViewFailed = controller.sceneEditing::onViewFailed,
                    )
                    WorkspaceSidePanel(
                        panel = uiState.openPanel?.takeIf { it != SidePanel.AI || isAiEnabled },
                        controller = controller,
                        uiState = uiState,
                        mcp = mcp,
                        aiClients = aiClients,
                        assistant = assistant,
                        aiSteps = aiSteps,
                        openSceneName = openSceneName,
                        selectedBlockType = selectedBlockType,
                        sessionState = sessionState,
                        panelLayout = panelLayout,
                        row = row,
                    )
                }
            }
            WorkspaceConsole(
                entries = controller.console,
                onClear = controller::clearConsole,
                panelLayout = panelLayout,
                availableHeight = columnHeight,
            )
        }
        SidebarDragPreview(sidebarDragState)
    }
}

/**
 * Reads the repository again whenever the window gets the focus back, as IntelliJ does, since the project watcher leaves
 * `.git` out and a commit or a checkout made in a terminal would otherwise go unseen.
 */
@Composable
private fun RefreshingGitOnWindowFocus(
    refresh: () -> Unit,
) {
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    val currentRefresh by rememberUpdatedState(refresh)
    LaunchedEffect(isWindowFocused) {
        if (isWindowFocused) currentRefresh()
    }
}

@Composable
private fun WorkspaceToolbarOf(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    sessionState: SessionState,
    isAiConnected: Boolean,
    onOpenSettings: () -> Unit,
    onCloseProject: () -> Unit,
) = WorkspaceToolbar(
    sessionState = sessionState,
    history = controller.commands.history,
    onUndo = controller.commands::undo,
    onRedo = controller.commands::redo,
    gitState = controller.git.state,
    isAiConnected = isAiConnected,
    openPanel = uiState.openPanel,
    onPanelToggled = { panel -> if (uiState.togglePanel(panel)) controller.git.refresh() },
    playTargets = controller.playTargets.targets.collectAsState().value,
    onPlayTargetsOpened = controller.playTargets::startRefresh,
    onPlayTargetChosen = controller.playTargets::choose,
    onPlay = controller::play,
    onStop = controller::stopSession,
    onOpenProjectSettings = { uiState.showProjectSettings() },
    onOpenTargets = { uiState.showProjectSettings(ProjectSettingsPage.TARGETS) },
    onSetUpMoreDevices = controller::openDeviceSetup,
    onOpenSettings = onOpenSettings,
    onCloseProject = onCloseProject,
)

/**
 * The workspace screen's own state for [controller]'s project, which a project opened next starts afresh.
 */
@Composable
private fun rememberWorkspaceUiState(controller: WorkspaceController) = remember(controller) { WorkspaceUiState() }

/**
 * Where the sidebar's types and Scenes are dragged: a dropped one moves to its folder through a command.
 */
@Composable
private fun rememberSidebarDragState(controller: WorkspaceController) = remember(controller) {
    SidebarDragState().apply {
        onMoveToFolder = { group, name, folder ->
            controller.commands.dispatch(
                when (group) {
                    FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(folder, FolderPaths.nameOf(name))))
                    else -> Invocation(MoveToFolder, MoveToFolder.Input(name, folder))
                },
            )
        }
    }
}

/**
 * What the sidebar's groups with folders do, each through the command for it.
 */
@Composable
private fun rememberFolderTreeActions(controller: WorkspaceController) = remember(controller) {
    FolderTreeActions(
        onSelected = { group, name -> if (group == FolderGroup.SCENES) controller.selection.openScene(name) else controller.selection.selectType(name) },
        onSelectionCleared = controller.selection::clear,
        onCreateItem = { group, name ->
            when (group) {
                FolderGroup.ACTOR_TYPES -> controller.createActorType(name)
                FolderGroup.MANAGER_TYPES -> controller.createManagerType(name)
                FolderGroup.SCENES -> controller.createScene(name)
            }
        },
        onCreateFolder = { group, path -> controller.dispatchIfValid(Invocation(CreateFolder, CreateFolder.Input(group, path))) },
        onRenameFolder = { group, path, newName -> controller.dispatchIfValid(Invocation(RenameFolder, RenameFolder.Input(group, path, newName))) },
        onDeleteFolder = { group, path -> controller.commands.dispatch(Invocation(DeleteFolder, DeleteFolder.Input(group, path))) },
        onRenameItem = { group, name, newName ->
            controller.dispatchIfValid(
                when (group) {
                    FolderGroup.ACTOR_TYPES -> Invocation(RenameActorType, RenameActorType.Input(name, newName))
                    FolderGroup.MANAGER_TYPES -> Invocation(RenameManagerType, RenameManagerType.Input(name, newName))
                    FolderGroup.SCENES -> Invocation(RenameScene, RenameScene.Input(name, FolderPaths.child(FolderPaths.parentOf(name), newName)))
                },
            )
        },
        onDeleteItem = { group, name ->
            controller.commands.dispatch(
                when (group) {
                    FolderGroup.ACTOR_TYPES -> Invocation(DeleteActorType, DeleteActorType.Input(name))
                    FolderGroup.MANAGER_TYPES -> Invocation(DeleteManagerType, DeleteManagerType.Input(name))
                    FolderGroup.SCENES -> Invocation(DeleteScene, DeleteScene.Input(name))
                },
            )
        },
    )
}

/**
 * What the sidebar's groups with folders show for [controller]'s project, from its folders.
 */
@Composable
private fun rememberFolderTreeGroupsOf(
    controller: WorkspaceController,
    types: List<BlockTypeDefinition>,
    selectedTypeName: String?,
    sceneNames: List<String>,
    openSceneName: String?,
): Map<FolderGroup, FolderTreeGroupState> {
    val blockTypesController = controller.blockTypes
    val typeFolders = blockTypesController?.folders?.collectAsState()?.value.orEmpty()
    val sceneFolders by controller.sceneFolders.collectAsState()
    return rememberFolderTreeGroups(types.takeIf { blockTypesController != null }, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName)
}

/**
 * Hands the workspace's dialogs what they show and do from [controller].
 */
@Composable
private fun WorkspaceDialogsOf(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    projectUpgrade: TemplateUpgrade.Needed?,
) = WorkspaceDialogs(
    uiState = uiState,
    projectUpgrade = projectUpgrade,
    rootDirectory = controller.project.rootDirectory,
    blockTypes = controller.blockTypes?.types,
    scenes = controller.scenes,
    targets = controller.targets,
    history = controller.commands.history,
    gitState = controller.git.state,
    onCommand = controller.commands::dispatch,
)

/**
 * The side panel at the end of the workspace's row, if one is open, sliding in as it opens and fading as another takes
 * its place.
 */
@Composable
private fun WorkspaceSidePanel(
    panel: SidePanel?,
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    aiSteps: List<AiStep>,
    openSceneName: String?,
    selectedBlockType: BlockTypeDefinition?,
    sessionState: SessionState,
    panelLayout: PanelLayout,
    row: PanelRow,
) = AnimatedSidePanel(panel) { shown ->
    when (shown) {
        SidePanel.AI -> WorkspaceAiSidePanelOf(
            controller = controller,
            uiState = uiState,
            mcp = mcp,
            aiClients = aiClients,
            assistant = assistant,
            aiSteps = aiSteps,
            openSceneName = openSceneName,
            selectedBlockType = selectedBlockType,
            sessionState = sessionState,
            width = rememberPanelWidth(Panel.AI, panelLayout),
            row = row,
        )
        SidePanel.VERSION_CONTROL -> WorkspaceGitSidePanelOf(controller, rememberPanelWidth(Panel.VERSION_CONTROL, panelLayout), row)
    }
}

/**
 * Hands the Version control panel what it shows and does from [controller].
 */
@Composable
private fun WorkspaceGitSidePanelOf(
    controller: WorkspaceController,
    width: PanelWidthState,
    row: PanelRow,
) = WorkspaceGitSidePanel(
    gitState = controller.git.state,
    onCommand = controller.commands::dispatch,
    width = width,
    row = row,
)

/**
 * Hands the AI panel what it shows and does from [controller] and [assistant].
 */
@Composable
private fun WorkspaceAiSidePanelOf(
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
) = WorkspaceAiSidePanel(
    uiState = uiState,
    mcp = mcp,
    aiClients = aiClients,
    assistant = assistant,
    acceptsImages = assistant.acceptsImages,
    gameMap = controller.gameMap.gameMap,
    console = controller.console,
    aiSteps = aiSteps,
    openSceneName = openSceneName,
    selectedBlockType = selectedBlockType,
    sessionState = sessionState,
    onTypeSelected = { controller.selection.selectType(it) },
    onSceneSelected = { controller.selection.openScene(it) },
    captureScreen = controller::captureGameScreen,
    width = width,
    row = row,
)

/**
 * Selects each Actor type the AI changes, while the user follows the AI.
 */
@Composable
private fun FollowingAiSteps(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    latestAiStep: AiStep?,
    selection: WorkspaceSelection,
) {
    val isAiEnabled = LocalIsAiEnabled.current
    LaunchedEffect(latestAiStep?.id, latestAiStep?.isRunning) {
        val name = latestAiStep?.actorTypeToFollow() ?: return@LaunchedEffect
        if (isAiEnabled && uiState.isFollowingAi && selection != WorkspaceSelection.BlockType(name)) controller.selection.selectType(name)
    }
}
