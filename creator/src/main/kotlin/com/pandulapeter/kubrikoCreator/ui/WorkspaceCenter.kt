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
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.TypeEditorContextCache
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.components.fadeInOnChange
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneEditorPane
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneNotice
import com.pandulapeter.kubrikoCreator.ui.screenplay.LocalPageReviews
import com.pandulapeter.kubrikoCreator.ui.screenplay.MarkdownIdentity
import com.pandulapeter.kubrikoCreator.ui.screenplay.PageEditor
import com.pandulapeter.kubrikoCreator.ui.screenplay.PageImageSource
import com.pandulapeter.kubrikoCreator.ui.screenplay.PageLinksState
import com.pandulapeter.kubrikoCreator.ui.screenplay.markdownTitleOf
import com.pandulapeter.kubrikoCreator.ui.screenplay.rememberReviewOf
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.scene_preparing
import org.jetbrains.compose.resources.stringResource

/**
 * The middle of the workspace: the Scene shown, the editor of the selected type, the page editor showing a page or a
 * document, or the hint when nothing is selected.
 *
 * @param modifier Applied to whichever of them is shown, which the workspace's row places.
 * @param changedByHand The names of the types whose generated code was changed by hand.
 * @param shownAiStep The AI's step the type editor lights up, if any.
 * @param onCommand Applies a command, calling its `onSuccess` unless it is rejected.
 * @param currentBlockType The type of the given name as it is now, not as last composed.
 * @param onCommandIfValid Applies a command unless it is rejected, returning why it was.
 * @param markdownText The text of the page or document shown, once it is read, or null.
 * @param onMarkdownTextChange Saves the page or document shown.
 * @param markdownLinks What the links of the page or document shown point at.
 * @param onAddPageImage Puts an image on the page shown, or null if a document is shown.
 * @param markdownIdentity What tells the page or document shown apart from the one before ([MarkdownIdentity]).
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
    markdownText: String? = null,
    onMarkdownTextChange: (text: String) -> Unit = {},
    markdownLinks: PageLinksState? = null,
    markdownIdentity: Any? = null,
    onAddPageImage: ((PageImageSource, section: String?) -> Unit)? = null,
) {
    val contentModifier = modifier.fadeInOnChange(shownContentKey(selection, sceneEditing))
    val markdownTitle = markdownTitleOf(selection)
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val audioFiles = remember(resources) { AudioFiles.of(resources) }
    val typeEditorContexts = remember { TypeEditorContextCache() }
    when (sceneEditing) {
        is SceneEditingState.Open -> SceneEditorPane(
            modifier = contentModifier,
            state = sceneEditing,
            onCommand = { onCommand(it) {} },
            actorTypeDragState = actorTypeDragState,
            panelLayout = panelLayout,
            actorTypes = actorTypes,
            music = audioFiles.music,
            description = sceneDescriptions[sceneEditing.sceneName],
            onPlay = { onPlayScene(sceneEditing.sceneName) },
            onReload = onReloadScene,
            onClose = onCloseScene,
            onViewFailed = { exception, isShowingOlderClasses -> onSceneViewFailed(sceneEditing.revision, exception, isShowingOlderClasses) },
        )
        is SceneEditingState.Unsupported -> SceneNotice(
            modifier = contentModifier,
            sceneName = sceneEditing.scene.name,
            message = sceneEditing.reason.text(sceneEditing.arguments),
            onClose = onCloseScene,
        )
        is SceneEditingState.Failed -> SceneNotice(
            modifier = contentModifier,
            sceneName = sceneEditing.scene.name,
            message = sceneEditing.reason.text(sceneEditing.arguments),
            onClose = onCloseScene,
            onTryAgain = onReloadScene,
        )
        is SceneEditingState.Preparing -> Box(contentModifier) {
            ProgressScreen(stringResource(Res.string.scene_preparing, sceneEditing.scene.name))
        }
        SceneEditingState.Closed -> Unit
    }
    val selectedBlockType = (selection as? WorkspaceSelection.BlockType)?.let { selected -> blockTypes.firstOrNull { it.name == selected.name } }
    // The selection follows the Scene state a dispatch later, so the type editor waits for the Scene to close too.
    if (sceneEditing == SceneEditingState.Closed && selectedBlockType != null) {
        key(selectedBlockType.name) {
            val context = remember(selectedBlockType, blockTypes, sceneNames, audioFiles) { typeEditorContexts.contextOf(selectedBlockType, blockTypes, sceneNames, audioFiles) }
            BlockTypeEditor(
                modifier = contentModifier,
                definition = selectedBlockType,
                scope = context.scope,
                sprites = remember(resources) { SpriteFiles.of(resources).paths },
                inheritance = context.inheritance,
                isChangedByHand = selectedBlockType.name in changedByHand,
                onCommand = onCommand,
                currentDefinition = { currentBlockType(selectedBlockType.name) },
                onCommandIfValid = onCommandIfValid,
                onTypeSelected = onTypeSelected,
                highlight = shownAiStep?.let { it.step.highlightIn(selectedBlockType.name, it.flashKey) },
            )
        }
    } else if (markdownTitle != null && sceneEditing == SceneEditingState.Closed) {
        if (markdownText != null) {
            PageEditor(
                modifier = contentModifier,
                name = markdownTitle,
                text = markdownText,
                onTextChange = onMarkdownTextChange,
                links = markdownLinks,
                identity = markdownIdentity ?: markdownTitle,
                sprites = remember(resources) { SpriteFiles.of(resources).paths },
                onAddImage = onAddPageImage,
                showsWritingHints = selection is WorkspaceSelection.Page,
                review = (selection as? WorkspaceSelection.Page)?.let { LocalPageReviews.current.rememberReviewOf(it.name) },
            )
        }
    } else if (selection == WorkspaceSelection.None) {
        WorkspaceHint(
            modifier = contentModifier,
        )
    }
}

/**
 * What the center shows, as far as a change of it should fade the new content in: a Scene by its name whatever its state
 * (so its preview fades in after its progress), a type by its name, or the hint.
 */
internal fun shownContentKey(selection: WorkspaceSelection, sceneEditing: SceneEditingState): Any = when (sceneEditing) {
    is SceneEditingState.Open -> sceneEditing.sceneName
    is SceneEditingState.Unsupported -> sceneEditing.scene.name
    is SceneEditingState.Failed -> sceneEditing.scene.name
    is SceneEditingState.Preparing -> sceneEditing.scene.name
    SceneEditingState.Closed -> selection
}
