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
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.sprites.sheetOfSprite
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeHighlight
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.SpriteChoices
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
import com.pandulapeter.kubrikoCreator.ui.sprites.SpriteEditing
import com.pandulapeter.kubrikoCreator.ui.sprites.SpriteEditor
import com.pandulapeter.kubrikoCreator.ui.sprites.SpriteEditorState
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlinx.coroutines.flow.Flow
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
 * @param broughtBack The Actors each undo or redo put back, which the Scene editor selects.
 * @param editorAttention Told what the editors show, which the assistant offers.
 * @param markdownText The text of the page or document shown, once it is read, or null.
 * @param onMarkdownTextChange Saves the page or document shown.
 * @param markdownLinks What the links of the page or document shown point at.
 * @param onAddPageImage Puts an image on the page shown, or null if a document is shown.
 * @param markdownIdentity What tells the page or document shown apart from the one before ([MarkdownIdentity]).
 * @param sprites What the sprite editor needs of the workspace.
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
    broughtBack: Flow<ActorsBroughtBack>,
    onTypeSelected: (String) -> Unit,
    onPlayScene: (sceneName: String) -> Unit,
    onReloadScene: () -> Unit,
    onCloseScene: () -> Unit,
    onSceneViewFailed: (revision: Int, exception: Throwable, isShowingOlderClasses: Boolean) -> Unit,
    editorAttention: EditorAttention? = null,
    markdownText: String? = null,
    onMarkdownTextChange: (text: String) -> Unit = {},
    markdownLinks: PageLinksState? = null,
    markdownIdentity: Any? = null,
    onAddPageImage: ((PageImageSource, section: String?) -> Unit)? = null,
    sprites: SpriteEditing = SpriteEditing(),
) {
    val spriteEditorState = remember { SpriteEditorState() }
    val contentModifier = modifier.fadeInOnChange(shownContentKey(selection, sceneEditing))
    val markdownTitle = markdownTitleOf(selection)
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val audioFiles = remember(resources) { AudioFiles.ofScanned(resources) }
    SceneContent(
        modifier = contentModifier,
        sceneEditing = sceneEditing,
        actorTypes = actorTypes,
        music = audioFiles?.music,
        sceneDescriptions = sceneDescriptions,
        shownAiStep = shownAiStep,
        actorTypeDragState = actorTypeDragState,
        panelLayout = panelLayout,
        onCommand = onCommand,
        broughtBack = broughtBack,
        onPlayScene = onPlayScene,
        onReloadScene = onReloadScene,
        onCloseScene = onCloseScene,
        onSceneViewFailed = onSceneViewFailed,
        editorAttention = editorAttention,
    )
    val selectedBlockType = (selection as? WorkspaceSelection.BlockType)?.let { selected -> blockTypes.firstOrNull { it.name == selected.name } }
    // The selection follows the Scene state a dispatch later, so the type editor waits for the Scene to close too.
    if (sceneEditing == SceneEditingState.Closed && selectedBlockType != null) {
        TypeEditorOf(
            modifier = contentModifier,
            definition = selectedBlockType,
            blockTypes = blockTypes,
            sceneNames = sceneNames,
            audioFiles = audioFiles,
            sprites = remember(resources, sprites.sheets) { SpriteChoices(SpriteFiles.of(resources), sprites.sheets) },
            isChangedByHand = selectedBlockType.name in changedByHand,
            onCommand = onCommand,
            currentBlockType = currentBlockType,
            onCommandIfValid = onCommandIfValid,
            onTypeSelected = onTypeSelected,
            highlight = shownAiStep?.let { it.step.highlightIn(selectedBlockType.name, it.flashKey) },
            editorAttention = editorAttention,
        )
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
    } else if (selection is WorkspaceSelection.Sprite && sceneEditing == SceneEditingState.Closed) {
        key(selection.path) {
            SpriteEditor(
                modifier = contentModifier,
                path = selection.path,
                file = remember(resources, selection.path) { SpriteFiles.of(resources).files[selection.path] },
                sheet = sprites.sheets.sheetOfSprite(selection.path),
                revision = sprites.imagesRevision to resources,
                state = spriteEditorState,
                editing = sprites,
            )
        }
    } else if (selection == WorkspaceSelection.None) {
        WorkspaceHint(
            modifier = contentModifier,
        )
    }
}

/**
 * The Scene [sceneEditing] shows, as far as it got: open in the Scene editor, being prepared, or a notice of why it can't be.
 */
@Composable
private fun SceneContent(
    modifier: Modifier,
    sceneEditing: SceneEditingState,
    actorTypes: List<ActorTypeDefinition>,
    music: List<String>?,
    sceneDescriptions: Map<String, String>,
    shownAiStep: ShownAiStep?,
    actorTypeDragState: SidebarDragState,
    panelLayout: PanelLayout,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    broughtBack: Flow<ActorsBroughtBack>,
    onPlayScene: (sceneName: String) -> Unit,
    onReloadScene: () -> Unit,
    onCloseScene: () -> Unit,
    onSceneViewFailed: (revision: Int, exception: Throwable, isShowingOlderClasses: Boolean) -> Unit,
    editorAttention: EditorAttention?,
) {
    // Kept here rather than in the Scene's pane, so that a Scene opened again goes on with its flash instead of starting it anew.
    val flashStartedAt = remember(shownAiStep?.flashKey) { System.currentTimeMillis() }
    when (sceneEditing) {
        is SceneEditingState.Open -> SceneEditorPane(
            modifier = modifier,
            state = sceneEditing,
            onCommand = { onCommand(it) {} },
            actorTypeDragState = actorTypeDragState,
            panelLayout = panelLayout,
            actorTypes = actorTypes,
            music = music,
            description = sceneDescriptions[sceneEditing.sceneName],
            broughtBack = broughtBack,
            onPlay = { onPlayScene(sceneEditing.sceneName) },
            onReload = onReloadScene,
            onClose = onCloseScene,
            onViewFailed = { exception, isShowingOlderClasses -> onSceneViewFailed(sceneEditing.revision, exception, isShowingOlderClasses) },
            pictureHost = editorAttention,
            highlight = shownAiStep?.let { it.step.highlightInScene(sceneEditing.sceneName, it.flashKey, flashStartedAt) },
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
}

/**
 * The type editor of [definition], with what it reads worked out once per change of the project ([TypeEditorContextCache]).
 */
@Composable
private fun TypeEditorOf(
    modifier: Modifier,
    definition: BlockTypeDefinition,
    blockTypes: List<BlockTypeDefinition>,
    sceneNames: List<String>,
    audioFiles: AudioFiles?,
    sprites: SpriteChoices,
    isChangedByHand: Boolean,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    currentBlockType: (name: String) -> BlockTypeDefinition?,
    onCommandIfValid: (Invocation<*>) -> Rejection?,
    onTypeSelected: (String) -> Unit,
    highlight: ActorTypeHighlight?,
    editorAttention: EditorAttention?,
) = key(definition.name) {
    val typeEditorContexts = remember { TypeEditorContextCache() }
    val context = remember(definition, blockTypes, sceneNames, audioFiles) { typeEditorContexts.contextOf(definition, blockTypes, sceneNames, audioFiles) }
    BlockTypeEditor(
        modifier = modifier,
        definition = definition,
        scope = context.scope,
        sprites = sprites,
        inheritance = context.inheritance,
        isChangedByHand = isChangedByHand,
        onCommand = onCommand,
        currentDefinition = { currentBlockType(definition.name) },
        onCommandIfValid = onCommandIfValid,
        onTypeSelected = onTypeSelected,
        highlight = highlight,
        onEventShown = { index, type -> editorAttention?.showEvent(definition, index, type) },
    )
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
