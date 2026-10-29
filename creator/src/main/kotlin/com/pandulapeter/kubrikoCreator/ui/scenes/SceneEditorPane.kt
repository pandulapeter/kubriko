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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.ui.SidebarDragState
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_scene
import kubriko.creator.generated.resources.action_play_from_scene
import kubriko.creator.generated.resources.action_reload_scene
import kubriko.creator.generated.resources.action_zoom_in
import kubriko.creator.generated.resources.action_zoom_out
import kubriko.creator.generated.resources.action_zoom_to_fit
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_fit_screen
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.ic_refresh
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_zoom_in
import kubriko.creator.generated.resources.ic_zoom_out
import kubriko.creator.generated.resources.scene_outdated
import kubriko.creator.generated.resources.scene_updating
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A Scene, edited inside Kubriko Creator's window: a header naming it, with the camera's zoom, saying when its classes
 * are being built again after a change or couldn't be, offering to play the game from it, to reload it, and to close it, above the Scene itself and the inspector of what is selected in it.
 * Every change is a command.
 *
 * @param actorTypes The project's block-built Actor types.
 * @param music The project's music files, by their path from `composeResources`.
 */
@Composable
internal fun SceneEditorPane(
    state: SceneEditingState.Open,
    onCommand: (Invocation<*>) -> Unit,
    actorTypeDragState: SidebarDragState,
    actorTypes: List<ActorTypeDefinition>,
    music: List<String>,
    onPlay: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    val view = rememberSceneView(state)
    var selectedId by remember(state.scene.file) { mutableStateOf<String?>(null) }
    SceneHeader(
        sceneName = state.scene.name,
        isOutdated = state.isOutdated,
        isUpdating = state.isUpdating,
        onZoomIn = { view.viewportManager.multiplyScaleFactor(ZOOM_STEP) },
        onZoomOut = { view.viewportManager.multiplyScaleFactor(1f / ZOOM_STEP) },
        onZoomToFit = view::zoomToFit,
        onPlay = onPlay,
        onReload = onReload,
        onClose = onClose,
    )
    HorizontalDivider()
    val onRemove = { actorId: String ->
        selectedId = null
        onCommand(Invocation(RemoveActor, RemoveActor.Input(state.sceneName, actorId)))
    }
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        SceneCanvas(
            modifier = Modifier.weight(1f).fillMaxHeight(),
        view = view,
        actors = state.actors,
        previews = state.previews,
        selectedId = selectedId,
        onSelect = { selectedId = it },
        onMove = { actorId, x, y -> onCommand(Invocation(MoveActor, MoveActor.Input(state.sceneName, actorId, x, y))) },
        onRemove = onRemove,
        dragState = actorTypeDragState,
        onPlace = { actorTypeName, x, y ->
            val actorId = SceneDocument.newId(view.entries.map { it.id })
            selectedId = actorId
            onCommand(Invocation(PlaceActor, PlaceActor.Input(state.sceneName, actorTypeName, x, y, actorId)))
        },
    )
        VerticalDivider()
        SceneInspector(
            modifier = Modifier.width(260.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
            view = view,
            sceneName = state.sceneName,
            selectedId = selectedId,
            actorTypes = actorTypes,
            music = music,
            onSelect = { selectedId = it },
            onCommand = onCommand,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun SceneHeader(
    sceneName: String,
    isOutdated: Boolean,
    isUpdating: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    onPlay: () -> Unit,
    onReload: () -> Unit,
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
    IconActionButton(icon = Res.drawable.ic_play, label = Res.string.action_play_from_scene, onClick = onPlay)
    IconActionButton(icon = Res.drawable.ic_zoom_out, label = Res.string.action_zoom_out, onClick = onZoomOut)
    IconActionButton(icon = Res.drawable.ic_zoom_in, label = Res.string.action_zoom_in, onClick = onZoomIn)
    IconActionButton(icon = Res.drawable.ic_fit_screen, label = Res.string.action_zoom_to_fit, onClick = onZoomToFit)
    IconActionButton(icon = Res.drawable.ic_refresh, label = Res.string.action_reload_scene, onClick = onReload)
    IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_close_scene, onClick = onClose)
}

private const val ZOOM_STEP = 1.25f
