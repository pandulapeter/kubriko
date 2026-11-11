/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.project.SetDescription
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.ui.HealthHintsList
import com.pandulapeter.kubrikoCreator.ui.SidebarDragState
import com.pandulapeter.kubrikoCreator.ui.WriteDescriptionButton
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import com.pandulapeter.kubrikoCreator.ui.components.DescriptionPreview
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import com.pandulapeter.kubrikoCreator.ui.components.rememberPanelWidth
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.screenplay.LocalMentionedIn
import com.pandulapeter.kubrikoCreator.ui.screenplay.MentionedInRow
import kotlinx.coroutines.flow.Flow
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_scene
import kubriko.creator.generated.resources.action_delete_scene
import kubriko.creator.generated.resources.action_hide_scene_actors
import kubriko.creator.generated.resources.action_play_from_scene
import kubriko.creator.generated.resources.action_reload_scene
import kubriko.creator.generated.resources.action_show_scene_actors
import kubriko.creator.generated.resources.action_zoom_in
import kubriko.creator.generated.resources.action_zoom_out
import kubriko.creator.generated.resources.action_zoom_to_fit
import kubriko.creator.generated.resources.assistant_prompt_describe_scene
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_fit_screen
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.ic_refresh
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_zoom_in
import kubriko.creator.generated.resources.ic_zoom_out
import kubriko.creator.generated.resources.scene_outdated
import kubriko.creator.generated.resources.scene_preparing
import kubriko.creator.generated.resources.scene_updating
import org.jetbrains.compose.resources.stringResource

/**
 * A Scene, edited inside Kubriko Creator's window: a header naming it, with the camera's zoom, saying when its classes
 * are being built again after a change or couldn't be, offering to play the game from it, to reload it, to delete it, and to close it, above the Scene itself and the inspector of what is selected in it.
 * Every change is a command.
 *
 * @param actorTypes The project's block-built Actor types.
 * @param music The project's music files, by their path from `composeResources`.
 * @param description What the Scene is for.
 * @param broughtBack The Actors each undo or redo put back: one put back into this Scene is selected once the Scene
 *   shows it, unless it is the Scene settings.
 * @param onViewFailed Called when the project's code threw while the view of the Scene was being created.
 * @param pictureHost Offered a picture of the Scene while it is shown.
 */
@Composable
internal fun SceneEditorPane(
    state: SceneEditingState.Open,
    onCommand: (Invocation<*>) -> Unit,
    actorTypeDragState: SidebarDragState,
    panelLayout: PanelLayout,
    actorTypes: List<ActorTypeDefinition>,
    music: List<String>?,
    description: String?,
    broughtBack: Flow<ActorsBroughtBack>,
    onPlay: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
    onViewFailed: (exception: Throwable, isShowingOlderClasses: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pictureHost: ScenePictureHost? = null,
) = Column(
    modifier = modifier,
) {
    val selection = rememberSceneSelection(state, broughtBack)
    val view = rememberSceneView(state, onViewFailed) ?: return@Column ProgressScreen(stringResource(Res.string.scene_preparing, state.scene.name))
    SelectingRestoredActor(selection, view)
    var isActorListShown by remember { mutableStateOf(false) }
    var isSettingsShown by remember(state.scene.file) { mutableStateOf(false) }
    SceneHeader(
        sceneName = state.scene.name,
        isOutdated = state.isOutdated,
        isUpdating = state.isUpdating,
        onZoomIn = { view.viewportManager.multiplyScaleFactor(ZOOM_STEP) },
        onZoomOut = { view.viewportManager.multiplyScaleFactor(1f / ZOOM_STEP) },
        onZoomToFit = view::zoomToFit,
        isActorListShown = isActorListShown,
        onActorListToggled = { isActorListShown = !isActorListShown },
        onPlay = onPlay,
        onReload = onReload,
        onDelete = { onCommand(Invocation(DeleteScene, DeleteScene.Input(state.sceneName))) },
        onClose = onClose,
    )
    SceneDetails(state = state, description = description, onCommand = onCommand)
    HorizontalDivider()
    if (isSettingsShown) {
        SceneSettingsDialogOf(view = view, sceneName = state.sceneName, music = music, onCommand = onCommand, onDismiss = { isSettingsShown = false })
    }
    state.problem?.let { problem -> InvalidSceneNotice(problem = problem, onOpenFile = { openInSystemEditor(state.scene.file) }) }
    val onRemove = { actorId: String ->
        selection.select(null)
        onCommand(Invocation(RemoveActor, RemoveActor.Input(state.sceneName, actorId)))
    }
    val actorListWidth = rememberPanelWidth(Panel.SCENE_ACTORS, panelLayout)
    val inspectorWidth = rememberPanelWidth(Panel.SCENE_INSPECTOR, panelLayout)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val row = PanelRow(maxWidth, panelsInRow = if (isActorListShown) 2 else 1)
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isActorListShown) {
                // Read so that the list follows the Actors created from the Scene's content.
                view.loadRevision.collectAsState().value
                SceneActorList(
                    modifier = Modifier.sidePanel(actorListWidth.widthIn(row)),
                    entries = view.entries,
                    uncreatedIds = view.uncreatedIds,
                    selectedId = selection.selectedId,
                    onSelect = { actorId ->
                        if (view.isBackground(actorId)) {
                            isSettingsShown = true
                        } else {
                            selection.select(actorId)
                            view.centerOn(actorId)
                        }
                    },
                )
                PanelSplitter(actorListWidth, row)
            }
            SceneCanvas(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                view = view,
                actors = state.actors,
                previews = state.previews,
                isSceneReadable = state.problem == null,
                selectedId = selection.selectedId,
                onSelect = { actorId -> selection.select(actorId?.takeUnless(view::isBackground)) },
                onMove = { actorId, x, y -> onCommand(Invocation(MoveActor, MoveActor.Input(state.sceneName, actorId, x, y))) },
                onRemove = onRemove,
                dragState = actorTypeDragState,
                pictureHost = pictureHost,
                onPlace = { actorTypeName, x, y ->
                    val actorId = SceneDocument.newId(view.entries.map { it.id })
                    selection.select(actorId)
                    onCommand(Invocation(PlaceActor, PlaceActor.Input(state.sceneName, actorTypeName, x, y, actorId)))
                },
            )
            PanelSplitter(inspectorWidth, row)
            SceneInspector(
                modifier = Modifier.sidePanel(inspectorWidth.widthIn(row)),
                view = view,
                sceneName = state.sceneName,
                selectedId = selection.selectedId,
                actorTypes = actorTypes,
                music = music,
                onOpenSceneSettings = { isSettingsShown = true },
                onCommand = onCommand,
                onRemove = onRemove,
            )
        }
    }
}

/**
 * The Scene settings of [view]'s Scene in a dialog, following the Scene as it is loaded again.
 */
@Composable
private fun SceneSettingsDialogOf(
    view: SceneView,
    sceneName: String,
    music: List<String>?,
    onCommand: (Invocation<*>) -> Unit,
    onDismiss: () -> Unit,
) {
    view.loadRevision.collectAsState().value
    SceneSettingsDialog(
        settings = view.entries.getOrNull(SceneSettingsState.indexIn(view.entries)),
        music = music,
        onChange = { change -> onCommand(Invocation(SetSceneSettings, SetSceneSettings.Input(sceneName).change())) },
        onRestoreDefaults = { settings -> onCommand(Invocation(RemoveActor, RemoveActor.Input(sceneName, settings.id))) },
        onDismiss = onDismiss,
    )
}

@Composable
private fun SceneHeader(
    sceneName: String,
    isOutdated: Boolean,
    isUpdating: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    isActorListShown: Boolean,
    onActorListToggled: () -> Unit,
    onPlay: () -> Unit,
    onReload: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().height(40.dp).padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    AccentIcon(
        icon = Res.drawable.ic_scene,
        accent = Accent.SCENE,
    )
    Text(
        text = sceneName,
        style = MaterialTheme.typography.titleSmall,
    )
    if (isOutdated) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(Res.string.scene_outdated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        TextButton(onClick = onReload) {
            Text(stringResource(Res.string.action_reload_scene))
        }
    }
    if (isUpdating) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(Res.string.scene_updating),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.weight(1f))
    IconActionButton(
        icon = Res.drawable.ic_actor_type,
        label = if (isActorListShown) Res.string.action_hide_scene_actors else Res.string.action_show_scene_actors,
        onClick = onActorListToggled,
        tint = if (isActorListShown) MaterialTheme.colorScheme.primary else LocalContentColor.current,
    )
    IconActionButton(icon = Res.drawable.ic_play, label = Res.string.action_play_from_scene, onClick = onPlay)
    IconActionButton(icon = Res.drawable.ic_zoom_out, label = Res.string.action_zoom_out, onClick = onZoomOut)
    IconActionButton(icon = Res.drawable.ic_zoom_in, label = Res.string.action_zoom_in, onClick = onZoomIn)
    IconActionButton(icon = Res.drawable.ic_fit_screen, label = Res.string.action_zoom_to_fit, onClick = onZoomToFit)
    IconActionButton(icon = Res.drawable.ic_refresh, label = Res.string.action_reload_scene, onClick = onReload)
    IconActionButton(icon = Res.drawable.ic_delete, label = Res.string.action_delete_scene, onClick = onDelete)
    IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_close_scene, onClick = onClose)
}

private const val ZOOM_STEP = 1.25f

/**
 * What the Scene is for, the pages mentioning it, and what about it is worth a look, under its header.
 */
@Composable
private fun SceneDetails(
    state: SceneEditingState.Open,
    description: String?,
    onCommand: (Invocation<*>) -> Unit,
) {
    DescriptionPreview(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        name = state.sceneName,
        description = description,
        maximumLength = SetDescription.MAXIMUM_LENGTH,
        onDescriptionChange = { onCommand(Invocation(SetDescription, SetDescription.Input(scene = state.sceneName, description = it))) },
        action = { WriteDescriptionButton(prompt = stringResource(Res.string.assistant_prompt_describe_scene, state.sceneName)) },
    )
    MentionedInRow(
        modifier = Modifier.padding(start = 8.dp, end = 12.dp, bottom = 6.dp),
        path = LocalMentionedIn.current?.pathOf(state.scene.file),
    )
    HealthHintsList(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        subject = RelationSubject.Scene(state.sceneName),
    )
}
