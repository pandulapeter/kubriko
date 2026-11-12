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
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.WritersRoomRequest
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.screenplay.unusedImagesOf
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.assistant.LocalWritersRoom
import com.pandulapeter.kubrikoCreator.ui.assistant.StartingTheWritersRoom
import com.pandulapeter.kubrikoCreator.ui.assistant.rememberWritersRoom
import com.pandulapeter.kubrikoCreator.ui.components.AnimatedSidePanel
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import com.pandulapeter.kubrikoCreator.ui.components.rememberPanelWidth
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.git.RefreshingGitOnWindowFocus
import com.pandulapeter.kubrikoCreator.ui.screenplay.LocalMentionedIn
import com.pandulapeter.kubrikoCreator.ui.screenplay.LocalPageReviews
import com.pandulapeter.kubrikoCreator.ui.screenplay.markdownEditOf
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberMarkdownIdentity
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberMentionedIn
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberOpenMarkdownText
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberPageImageAdder
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberPageLinks
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberPageReviews
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettingsPage
import com.pandulapeter.kubrikoCreator.ui.sprites.rememberSpriteEditing
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import com.pandulapeter.kubrikoCreator.workspace.captureGameScreen
import com.pandulapeter.kubrikoCreator.workspace.toRejection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An open project: the toolbar, the sidebar with the project's contents, the editor of what is selected, and the console.
 *
 * @param panelLayout The side panels' widths, which the user drags at their edges.
 * @param writersRoom The idea the project was just created from, which its writers' room starts from.
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
    writersRoom: WritersRoomRequest? = null,
) {
    val uiState = remember(controller) { WorkspaceUiState() }
    StartingTheWritersRoom(writersRoom, uiState::showAssistant, assistant::startWritersRoom)
    CompositionLocalProvider(
        LocalDescriptionWriter provides rememberDescriptionWriter(assistant, uiState::showAssistant),
        LocalMentionedIn provides rememberMentionedIn(controller.commands.context, controller.selection),
        LocalHealthHints provides rememberHealthHints(controller.health.findings, controller.blockTypes?.types),
        LocalWritersRoom provides rememberWritersRoom(uiState::showAssistant, assistant::startWritersRoom),
        LocalPageReviews provides rememberPageReviews(assistant.chats, controller.commands.history),
    ) {
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
    val blockTypes = controller.blockTypes?.types?.collectAsState()?.value.orEmpty()
    val selection by controller.selection.current.collectAsState()
    val selectedBlockTypeName = (selection as? WorkspaceSelection.BlockType)?.name
    val selectedBlockType = blockTypes.firstOrNull { it.name == selectedBlockTypeName }
    val sceneEditing by controller.sceneEditing.state.collectAsState()
    val mcpState by mcp.state.collectAsState()
    val aiSteps by controller.commands.aiSteps.collectAsState()
    val followedAiStep = rememberFollowedAiStep(aiSteps, LocalAiActivitySettings.current.followDelayInMillis)
    val shownAiStep = shownAiStep(followedAiStep, uiState.revisitedAiStep, aiSteps.newestChange()?.id)?.takeIf { isAiEnabled }
    val projectUpgrade = controller.projectUpgrade.collectAsState().value as? TemplateUpgrade.Needed
    val readOnlyReason = (controller.formatStatus.collectAsState().value as? ProjectFormatStatus.ReadOnly)?.toRejection()
    val sidebarDragState = rememberSidebarDragState(controller.commands, controller.creation)
    val sceneNames = remember(resources) { resources[ResourceGroup.SCENES].orEmpty().map { SceneStore.nameOf(it.file) }.distinct().sorted() }
    val openSceneName = (selection as? WorkspaceSelection.Scene)?.name
    val markdownText = rememberOpenMarkdownText(controller.commands.context, controller.selection, selection)
    WorkspaceDialogsOf(controller = controller, uiState = uiState, projectUpgrade = projectUpgrade)
    FollowingAiSteps(controller = controller, followedAiStep = followedAiStep, selection = selection)
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
                .onKeyEvent { event -> event.openSearch(uiState) || event.toHistoryStep()?.let { isUndo -> uiState.spriteEditor.step(isUndo, controller.commands) } != null },
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
                        folderGroups = rememberFolderTreeGroupsOf(controller, blockTypes, selectedBlockTypeName, sceneNames, openSceneName, (selection as? WorkspaceSelection.Page)?.name),
                        folderActions = rememberFolderTreeActions(controller.creation, controller.commands, controller.selection, uiState),
                        dragState = sidebarDragState,
                        documentActions = rememberDocumentActions(controller.creation, controller.commands, controller.selection, uiState),
                        resourceActions = rememberResourceActions(controller.commands, controller.selection),
                        selection = selection,
                        layout = controller.layout.collectAsState().value,
                        gameModule = controller.gameModule.modules.collectAsState().value?.chosen?.let(::moduleFolderOf),
                        spriteFolders = controller.commands.context.spriteFolders.state.collectAsState().value,
                        folderExpansion = controller.sidebarExpansion,
                        flashedName = shownAiStep?.step?.sidebarName(),
                        flashKey = shownAiStep?.flashKey,
                        filterFocusRequests = uiState.sidebarFilterFocusRequests,
                    )
                    PanelSplitter(sidebarWidth, row)
                    WorkspaceCenterOf(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        controller = controller,
                        uiState = uiState,
                        selection = selection,
                        sceneEditing = sceneEditing,
                        blockTypes = blockTypes,
                        resources = resources,
                        sceneNames = sceneNames,
                        shownAiStep = shownAiStep,
                        sidebarDragState = sidebarDragState,
                        panelLayout = panelLayout,
                        markdownText = markdownText,
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
                lines = controller.console,
                onClear = controller::clearConsole,
                panelLayout = panelLayout,
                availableHeight = columnHeight,
            )
        }
        SidebarDragPreview(sidebarDragState)
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
    isBuilding = controller.runningBuilds.collectAsState().value > 0,
    history = controller.commands.history,
    onUndo = { uiState.spriteEditor.step(isUndo = true, controller.commands) },
    onRedo = { uiState.spriteEditor.step(isUndo = false, controller.commands) },
    editorHistory = uiState.spriteEditor,
    gitState = controller.git.state,
    isAiConnected = isAiConnected,
    openPanel = uiState.openPanel,
    onPanelToggled = { panel -> if (uiState.togglePanel(panel)) controller.git.refresh() },
    playTargets = controller.playTargets.targets.collectAsState().value,
    onPlayTargetsOpened = controller.playTargets::startRefresh,
    onPlayTargetChosen = controller.playTargets::choose,
    gameModules = controller.gameModule.modules.collectAsState().value,
    onGameModuleChosen = controller.gameModule::choose,
    onPlay = controller::play,
    onStop = controller::stopSession,
    onOpenProjectSettings = { uiState.showProjectSettings() },
    onOpenTargets = { uiState.showProjectSettings(ProjectSettingsPage.TARGETS) },
    onSetUpMoreDevices = { kind -> uiState.setUpMoreDevices(kind, controller::openAndroidEmulatorGuide) },
    onOpenSettings = onOpenSettings,
    onCloseProject = onCloseProject,
)

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
    openPageName: String?,
): Map<FolderGroup, FolderTreeGroupState> {
    val blockTypesController = controller.blockTypes
    val typeFolders = blockTypesController?.folders?.collectAsState()?.value.orEmpty()
    val sceneFolders = controller.layout.collectAsState().value.sceneFolders
    val screenplay = controller.commands.context.screenplay
    val pages = pageGroupState(screenplay.current.collectAsState().value, screenplay.currentFolders.collectAsState().value, openPageName)
    return rememberFolderTreeGroups(types.takeIf { blockTypesController != null }, typeFolders, selectedTypeName, sceneNames, sceneFolders, openSceneName, pages)
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
    usagesOf = controller.commands::usagesOf,
    unusedImagesOf = { page -> withContext(Dispatchers.IO) { controller.commands.context.unusedImagesOf(page) } },
    onConfirmedDelete = controller.commands::dispatch,
    validate = controller.commands::validate,
    iosSimulatorOptions = controller.iosSimulatorSetup::options,
    createIosSimulator = controller.iosSimulatorSetup::create,
    onUsageSelected = { usage ->
        when (val owner = usage.placeOwner) {
            is RelationSubject.Type -> controller.selection.selectType(owner.name)
            is RelationSubject.Scene -> controller.selection.openScene(owner.name)
            is RelationSubject.Page -> controller.selection.openPage(owner.name)
            is RelationSubject.Property, is RelationSubject.Image, is RelationSubject.Resource, is RelationSubject.Timer, is RelationSubject.Folder, null -> Unit
        }
    },
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
    rootDirectory = controller.project.rootDirectory,
    screenplayRevision = controller.commands.context.screenplay.revision,
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
    onPageSelected = controller.selection::openPage,
    onDocumentSelected = controller.selection::openDocument,
    captureScreen = controller::captureGameScreen,
    width = width,
    screenplayOffer = controller.screenplayOffer,
    questions = controller.commands.context.questions,
    row = row,
)

/**
 * Goes to each Actor type, Manager type, or Scene the AI changes, while the user follows the AI.
 */
@Composable
private fun FollowingAiSteps(
    controller: WorkspaceController,
    followedAiStep: AiStep?,
    selection: WorkspaceSelection,
) {
    val isAiEnabled by rememberUpdatedState(LocalIsAiEnabled.current)
    val isFollowing by rememberUpdatedState(LocalAiActivitySettings.current.isFollowing)
    val currentSelection by rememberUpdatedState(selection)
    val target = followedAiStep?.selectionToFollow()
    LaunchedEffect(followedAiStep?.id, target) {
        if (!isAiEnabled || !isFollowing || target == currentSelection || controller.selection.isLeaveGuarded) return@LaunchedEffect
        when (target) {
            is WorkspaceSelection.BlockType -> controller.selection.selectType(target.name)
            is WorkspaceSelection.Scene -> controller.selection.openScene(target.name)
            is WorkspaceSelection.Page -> controller.selection.openPage(target.name)
            is WorkspaceSelection.Document -> controller.selection.openDocument(target.path)
            is WorkspaceSelection.Sprite -> controller.selection.openSprite(target.path)
            WorkspaceSelection.None, null -> Unit
        }
    }
}

/**
 * Hands the workspace's center what it shows and does from [controller].
 */
@Composable
private fun WorkspaceCenterOf(
    modifier: Modifier,
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    selection: WorkspaceSelection,
    sceneEditing: SceneEditingState,
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    sceneNames: List<String>,
    shownAiStep: ShownAiStep?,
    sidebarDragState: SidebarDragState,
    panelLayout: PanelLayout,
    markdownText: String?,
) = WorkspaceCenter(
    modifier = modifier,
    selection = selection,
    sceneEditing = sceneEditing,
    blockTypes = blockTypes,
    resources = resources,
    sceneNames = sceneNames,
    sceneDescriptions = controller.scenes.descriptions.current.collectAsState().value,
    changedByHand = controller.blockTypes?.changedByHand?.collectAsState()?.value.orEmpty(),
    shownAiStep = shownAiStep,
    actorTypeDragState = sidebarDragState,
    panelLayout = panelLayout,
    onCommand = { invocation, onSuccess -> uiState.dispatch(invocation, onSuccess, controller.commands::dispatch) },
    currentBlockType = { controller.blockTypes?.get(it) },
    onCommandIfValid = controller.commands::dispatchIfValid,
    broughtBack = controller.commands.broughtBack,
    onTypeSelected = { controller.selection.selectType(it) },
    onPlayScene = { controller.play(firstScene = it) },
    onReloadScene = controller.sceneEditing::reload,
    onCloseScene = controller.selection::clear,
    onSceneViewFailed = controller.sceneEditing::onViewFailed,
    editorAttention = uiState.editorAttention,
    markdownText = markdownText,
    onMarkdownTextChange = { text -> markdownEditOf(selection, text)?.let(controller.commands::dispatch) },
    markdownLinks = rememberPageLinks(controller.commands.context, controller.selection, selection, markdownText, blockTypes, resources),
    markdownIdentity = rememberMarkdownIdentity(selection, controller.commands.context.movedFiles),
    onAddPageImage = rememberPageImageAdder(selection) { controller.commands.dispatch(it) },
    sprites = rememberSpriteEditing(controller.commands, controller.selection, uiState.spriteEditor),
)
