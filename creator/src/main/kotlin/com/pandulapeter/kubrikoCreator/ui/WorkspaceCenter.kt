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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.Inheritance
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneEditorPane
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneNotice
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.scene_preparing
import org.jetbrains.compose.resources.stringResource

/**
 * The middle of the workspace: the Scene shown, the editor of the selected type, or the hint when nothing is selected.
 *
 * @param modifier Applied to whichever of them is shown, which the workspace's row places.
 * @param changedByHand The names of the types whose generated code was changed by hand.
 * @param shownAiStep The AI's step the type editor lights up, if any.
 * @param onCommand Applies a command, calling its `onSuccess` unless it is rejected.
 * @param currentBlockType The type of the given name as it is now, not as last composed.
 * @param onCommandIfValid Applies a command unless it is rejected, returning why it was.
 */
@Composable
internal fun WorkspaceCenter(
    modifier: Modifier,
    selection: WorkspaceSelection,
    sceneEditing: SceneEditingState,
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    sceneNames: List<String>,
    sceneDescriptions: Map<String, String>,
    changedByHand: Set<String>,
    shownAiStep: ShownAiStep?,
    actorTypeDragState: SidebarDragState,
    panelLayout: PanelLayout,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    currentBlockType: (name: String) -> BlockTypeDefinition?,
    onCommandIfValid: (Invocation<*>) -> Rejection?,
    onTypeSelected: (String) -> Unit,
    onPlayScene: (sceneName: String) -> Unit,
    onReloadScene: () -> Unit,
    onCloseScene: () -> Unit,
    onSceneViewFailed: (revision: Int, exception: Throwable, isShowingOlderClasses: Boolean) -> Unit,
) {
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val family = remember(actorTypes) { ActorTypeFamily(actorTypes) }
    when (sceneEditing) {
        is SceneEditingState.Open -> SceneEditorPane(
            modifier = modifier,
            state = sceneEditing,
            onCommand = { onCommand(it) {} },
            actorTypeDragState = actorTypeDragState,
            panelLayout = panelLayout,
            actorTypes = actorTypes,
            music = remember(resources) { AudioFiles.of(resources).music },
            description = sceneDescriptions[sceneEditing.sceneName],
            onPlay = { onPlayScene(sceneEditing.sceneName) },
            onReload = onReloadScene,
            onClose = onCloseScene,
            onViewFailed = { exception, isShowingOlderClasses -> onSceneViewFailed(sceneEditing.revision, exception, isShowingOlderClasses) },
        )
        is SceneEditingState.Unsupported -> SceneNotice(
            modifier = modifier,
            sceneName = sceneEditing.scene.name,
            message = sceneEditing.reason.text(sceneEditing.arguments),
            onClose = onCloseScene,
        )
        is SceneEditingState.Failed -> SceneNotice(
            modifier = modifier,
            sceneName = sceneEditing.scene.name,
            message = sceneEditing.reason.text(sceneEditing.arguments),
            onClose = onCloseScene,
            onTryAgain = onReloadScene,
        )
        is SceneEditingState.Preparing -> Box(modifier) {
            ProgressScreen(stringResource(Res.string.scene_preparing, sceneEditing.scene.name))
        }
        SceneEditingState.Closed -> Unit
    }
    val selectedBlockType = (selection as? WorkspaceSelection.BlockType)?.let { selected -> blockTypes.firstOrNull { it.name == selected.name } }
    // The selection follows the Scene state a dispatch later, so the type editor waits for the Scene to close too.
    if (sceneEditing == SceneEditingState.Closed && selectedBlockType != null) {
        key(selectedBlockType.name) {
            BlockTypeEditor(
                modifier = modifier,
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
                isChangedByHand = selectedBlockType.name in changedByHand,
                onCommand = onCommand,
                currentDefinition = { currentBlockType(selectedBlockType.name) },
                onRename = { newName ->
                    onCommandIfValid(
                        when (selectedBlockType) {
                            is ActorTypeDefinition -> Invocation(RenameActorType, RenameActorType.Input(selectedBlockType.name, newName))
                            is ManagerTypeDefinition -> Invocation(RenameManagerType, RenameManagerType.Input(selectedBlockType.name, newName))
                        },
                    )
                },
                onTypeSelected = onTypeSelected,
                highlight = shownAiStep?.let { it.step.highlightIn(selectedBlockType.name, it.flashKey) },
            )
        }
    } else if (selection == WorkspaceSelection.None) {
        WorkspaceHint(
            modifier = modifier,
        )
    }
}
