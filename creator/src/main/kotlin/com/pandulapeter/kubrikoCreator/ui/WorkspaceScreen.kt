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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeEditor
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneEditorPane
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.scene_preparing
import kubriko.creator.generated.resources.workspace_hint
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * An open project: the toolbar, the sidebar with the project's contents, the editor of what is selected, and the console.
 */
@Composable
internal fun WorkspaceScreen(
    controller: WorkspaceController,
    onCloseProject: () -> Unit,
) {
    val resources by controller.resources.collectAsState()
    val console by controller.console.collectAsState()
    val sessionState by controller.sessionState.collectAsState()
    val actorTypesController = controller.actorTypes
    val actorTypes = actorTypesController?.actorTypes?.collectAsState()?.value.orEmpty()
    val selectedActorTypeName = actorTypesController?.selectedName?.collectAsState()?.value
    val selectedActorType = actorTypes.firstOrNull { it.name == selectedActorTypeName }
    val sceneEditing by controller.sceneEditing.state.collectAsState()
    var isConsoleExpanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        WorkspaceToolbar(
            sessionState = sessionState,
            onPlay = controller::play,
            onStop = controller::stopSession,
            onRefresh = controller::refreshResources,
            onCloseProject = onCloseProject,
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            ProjectSidebar(
                modifier = Modifier.width(260.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                resources = resources,
                actorTypes = actorTypesController?.let { ActorTypeTreeState(actorTypes.map { it.name }, selectedActorTypeName) },
                onSceneSelected = { scene ->
                    actorTypesController?.select(null)
                    controller.sceneEditing.open(scene)
                },
                onActorTypeSelected = {
                    controller.sceneEditing.close()
                    actorTypesController?.select(it)
                },
                onCreateActorType = { actorTypesController?.create(it) },
            )
            VerticalDivider()
            val centerModifier = Modifier.weight(1f).fillMaxHeight()
            when (val state = sceneEditing) {
                is SceneEditingState.Open -> SceneEditorPane(
                    modifier = centerModifier,
                    state = state,
                    onReload = controller.sceneEditing::reload,
                    onOpenInWindow = {
                        controller.sceneEditing.close()
                        controller.editSceneInWindow(state.scene)
                    },
                    onClose = controller.sceneEditing::close,
                )
                is SceneEditingState.Preparing -> Box(centerModifier) {
                    ProgressScreen(stringResource(Res.string.scene_preparing, state.scene.name))
                }
                SceneEditingState.Closed -> Unit
            }
            if (sceneEditing == SceneEditingState.Closed && actorTypesController != null && selectedActorType != null) {
                ActorTypeEditor(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    definition = selectedActorType,
                    actorTypeNames = actorTypes.map { it.name },
                    onChange = actorTypesController::update,
                    onDelete = { actorTypesController.delete(selectedActorType.name) },
                )
            } else if (sceneEditing == SceneEditingState.Closed) {
                WorkspaceHint(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        HorizontalDivider()
        Console(
            entries = console,
            isExpanded = isConsoleExpanded,
            onExpandedChange = { isConsoleExpanded = it },
            onClear = controller::clearConsole,
        )
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
