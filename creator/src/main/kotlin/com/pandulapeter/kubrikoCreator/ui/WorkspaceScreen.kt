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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.healthFindings
import com.pandulapeter.kubrikoCreator.context.projectDigest
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.Inheritance
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.git.GitPanel
import com.pandulapeter.kubrikoCreator.ui.mcp.AiActivityBar
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneEditorPane
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneNotice
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettings
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import com.pandulapeter.kubrikoCreator.workspace.toRejection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_upgrade_commit_message
import kubriko.creator.generated.resources.scene_preparing
import org.jetbrains.compose.resources.stringResource

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
    val resources by controller.resources.collectAsState()
    val sessionState by controller.sessionState.collectAsState()
    val blockTypesController = controller.blockTypes
    val blockTypes = blockTypesController?.types?.collectAsState()?.value.orEmpty()
    val selection by controller.selection.current.collectAsState()
    val selectedBlockTypeName = (selection as? WorkspaceSelection.BlockType)?.name
    val selectedBlockType = blockTypes.firstOrNull { it.name == selectedBlockTypeName }
    val blockTypesChangedByHand = blockTypesController?.changedByHand?.collectAsState()?.value.orEmpty()
    val sceneEditing by controller.sceneEditing.state.collectAsState()
    val sceneDescriptions by controller.scenes.descriptions.current.collectAsState()
    var openPanel by remember { mutableStateOf<SidePanel?>(null) }
    var aiPanelView by remember { mutableStateOf(if (mcp.settings.value.isEnabled) AiPanelView.OWN_TOOL else AiPanelView.ASSISTANT) }
    val mcpState by mcp.state.collectAsState()
    val aiSteps by controller.commands.aiSteps.collectAsState()
    val latestAiStep = aiSteps.lastOrNull()
    var revisitedAiStep by remember { mutableStateOf<RevisitedAiStep?>(null) }
    val shownAiStep = shownAiStep(latestAiStep, revisitedAiStep)?.takeIf { isAiEnabled }
    var isFollowingAi by remember { mutableStateOf(true) }
    val projectUpgrade = controller.projectUpgrade.collectAsState().value as? TemplateUpgrade.Needed
    val readOnlyReason = (controller.formatStatus.collectAsState().value as? ProjectFormatStatus.ReadOnly)?.toRejection()
    var isUpgradeNoticeDismissed by remember { mutableStateOf(false) }
    val sidebarDragState = rememberSidebarDragState(controller)
    val folderTreeActions = rememberFolderTreeActions(controller)
    val blockTypeFolders = blockTypesController?.folders?.collectAsState()?.value.orEmpty()
    val sceneFolders by controller.sceneFolders.collectAsState()
    val sceneNames = remember(resources) { resources[ResourceGroup.SCENES].orEmpty().map { SceneStore.nameOf(it.file) }.distinct().sorted() }
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val family = remember(actorTypes) { ActorTypeFamily(actorTypes) }
    val openSceneName = (selection as? WorkspaceSelection.Scene)?.name
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
    var isShowingProjectSettings by remember { mutableStateOf(false) }
    if (isShowingProjectSettings) {
        ProjectSettings(
            rootDirectory = controller.project.rootDirectory,
            history = controller.commands.history,
            onCommand = controller.commands::dispatch,
            onReviewUpgrade = projectUpgrade?.let {
                {
                    isShowingProjectSettings = false
                    isReviewingUpgrade = true
                }
            },
            onDismiss = { isShowingProjectSettings = false },
        )
    }
    val upgradeCommitMessage = stringResource(Res.string.project_upgrade_commit_message)
    if (projectUpgrade != null && isReviewingUpgrade) {
        ProjectUpgradeDialog(
            upgrade = projectUpgrade,
            onApply = {
                controller.commands.dispatch(Invocation(UpgradeProject, NoInput))
                isReviewingUpgrade = false
            },
            onCommitFirst = (controller.git.state.collectAsState().value as? GitState.Ready)?.takeIf { it.changes.isNotEmpty() }?.let {
                { controller.commands.dispatch(Invocation(Commit, Commit.Input(upgradeCommitMessage))) }
            },
            onDismiss = { isReviewingUpgrade = false },
        )
    }
    LaunchedEffect(latestAiStep?.id, latestAiStep?.isRunning) {
        val name = latestAiStep?.actorTypeToFollow() ?: return@LaunchedEffect
        if (isAiEnabled && isFollowingAi && selection != WorkspaceSelection.BlockType(name)) controller.selection.selectType(name)
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
                openPanel = openPanel,
                onPanelToggled = { panel ->
                    openPanel = panel.takeIf { it != openPanel }
                    if (openPanel == SidePanel.VERSION_CONTROL) controller.git.refresh()
                },
                onPlay = controller::play,
                onStop = controller::stopSession,
                onRefresh = controller::refreshResources,
                onOpenProjectSettings = { isShowingProjectSettings = true },
                onOpenSettings = onOpenSettings,
                onCloseProject = onCloseProject,
            )
            HorizontalDivider()
            AiActivityBar(
                steps = aiSteps,
                isFollowing = isFollowingAi,
                onFollowingChange = { isFollowingAi = it },
                onClick = { openPanel = SidePanel.AI },
            )
            readOnlyReason?.let { ReadOnlyProjectNotice(reason = it) }
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
                    modifier = Modifier.sidePanel(PanelWidths.SIDEBAR),
                    resources = resources,
                    folderGroups = folderGroups,
                    folderActions = folderTreeActions,
                    dragState = sidebarDragState,
                    flashedName = shownAiStep?.step?.sidebarName(),
                    flashKey = shownAiStep?.flashKey,
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
                        description = sceneDescriptions[state.sceneName],
                        onPlay = { controller.play(firstScene = state.sceneName) },
                        onReload = controller.sceneEditing::reload,
                        onClose = controller.selection::clear,
                        onViewFailed = { exception, isShowingOlderClasses -> controller.sceneEditing.onViewFailed(state.revision, exception, isShowingOlderClasses) },
                    )
                    is SceneEditingState.Unsupported -> SceneNotice(
                        modifier = centerModifier,
                        sceneName = state.scene.name,
                        message = stringResource(state.reason, *state.arguments.toTypedArray()),
                        onClose = controller.selection::clear,
                    )
                    is SceneEditingState.Failed -> SceneNotice(
                        modifier = centerModifier,
                        sceneName = state.scene.name,
                        message = stringResource(state.reason, *state.arguments.toTypedArray()),
                        onClose = controller.selection::clear,
                        onTryAgain = controller.sceneEditing::reload,
                    )
                    is SceneEditingState.Preparing -> Box(centerModifier) {
                        ProgressScreen(stringResource(Res.string.scene_preparing, state.scene.name))
                    }
                    SceneEditingState.Closed -> Unit
                }
                // The selection follows the Scene state a dispatch later, so the type editor waits for the Scene to close too.
                if (selection is WorkspaceSelection.BlockType && sceneEditing == SceneEditingState.Closed && selectedBlockType != null) {
                    key(selectedBlockType.name) {
                        BlockTypeEditor(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            definition = selectedBlockType,
                            scope = remember(selectedBlockType, blockTypes, resources, family) {
                                BlockScope.of(
                                    definition = selectedBlockType,
                                    types = blockTypes,
                                    sceneNames = sceneNames,
                                    audioFiles = AudioFiles.of(resources),
                                    family = family,
                                )
                            },
                            sprites = remember(resources) { SpriteFiles.of(resources).paths },
                            inheritance = remember(selectedBlockType, blockTypes, family) { Inheritance.of(selectedBlockType, blockTypes, family = family) },
                            isChangedByHand = selectedBlockType.name in blockTypesChangedByHand,
                            onCommand = { controller.commands.dispatch(it) },
                            onRename = { newName ->
                                controller.dispatchIfValid(
                                    when (selectedBlockType) {
                                        is ActorTypeDefinition -> Invocation(RenameActorType, RenameActorType.Input(selectedBlockType.name, newName))
                                        is ManagerTypeDefinition -> Invocation(RenameManagerType, RenameManagerType.Input(selectedBlockType.name, newName))
                                    },
                                )
                            },
                            onTypeSelected = { controller.selection.selectType(it) },
                            highlight = shownAiStep?.let { it.step.highlightIn(selectedBlockType.name, it.flashKey) },
                        )
                    }
                } else if (selection == WorkspaceSelection.None) {
                    WorkspaceHint(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (openPanel == SidePanel.AI && isAiEnabled) {
                    VerticalDivider()
                    val digest by produceState<ProjectDigest?>(null, aiPanelView, blockTypes, resources, sceneDescriptions, latestAiStep?.id, sceneEditing) {
                        value = if (aiPanelView == AiPanelView.GAME_MAP) controller.commands.context.projectDigest() else null
                    }
                    val findings by produceState(emptyList<HealthFinding>(), aiPanelView, blockTypes, resources, latestAiStep?.id, sceneEditing) {
                        value = if (aiPanelView == AiPanelView.GAME_MAP) controller.commands.context.healthFindings() else emptyList()
                    }
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
                        modifier = Modifier.sidePanel(PanelWidths.AI),
                        view = aiPanelView,
                        onViewSelected = { aiPanelView = it },
                        assistant = assistant,
                        mcp = mcp,
                        aiClients = aiClients,
                        aiSteps = aiSteps,
                        onShowStep = { step ->
                            revisitedAiStep = RevisitedAiStep(step, latestStepId = latestAiStep?.id)
                            when (val target = step.target) {
                                is CommandTarget.BlockType -> controller.selection.selectType(target.name)
                                is CommandTarget.Scene -> controller.selection.openScene(target.name)
                                else -> Unit
                            }
                        },
                        digest = digest,
                        findings = findings,
                        onTypeSelected = { controller.selection.selectType(it) },
                        onSceneSelected = { controller.selection.openScene(it) },
                        attention = attention,
                        captureScreen = { (controller.commands.context.runner.captureScreen() as? GameStep.Done)?.png },
                    )
                }
                if (openPanel == SidePanel.VERSION_CONTROL) {
                    VerticalDivider()
                    GitPanel(
                        modifier = Modifier.sidePanel(PanelWidths.VERSION_CONTROL),
                        state = controller.git.state,
                        onRefresh = controller.git::refresh,
                        onStartVersionControl = { controller.commands.dispatch(Invocation(StartVersionControl, NoInput)) },
                        onCommit = { message, onCommitted -> controller.commands.dispatch(Invocation(Commit, Commit.Input(message)), onSuccess = onCommitted) },
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
