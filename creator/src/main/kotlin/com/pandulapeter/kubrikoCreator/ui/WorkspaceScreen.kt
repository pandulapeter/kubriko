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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeEditor
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.workspace_hint
import org.jetbrains.compose.resources.stringResource

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
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        WorkspaceToolbar(
            projectName = controller.project.name,
            sessionState = sessionState,
            onPlay = controller::play,
            onStop = controller::stopSession,
            onCloseProject = onCloseProject,
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            ResourceTree(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                resources = resources,
                actorTypes = actorTypesController?.let { ActorTypeTreeState(actorTypes.map { it.name }, selectedActorTypeName) },
                onSceneSelected = controller::editScene,
                onActorTypeSelected = { actorTypesController?.select(it) },
                onCreateActorType = { actorTypesController?.create(it) },
            )
            VerticalDivider()
            if (actorTypesController != null && selectedActorType != null) {
                ActorTypeEditor(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    definition = selectedActorType,
                    onChange = actorTypesController::update,
                    onDelete = { actorTypesController.delete(selectedActorType.name) },
                )
            } else {
                WorkspaceHint(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        HorizontalDivider()
        Console(
            modifier = Modifier.fillMaxWidth().weight(0.35f),
            entries = console,
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
    Text(
        text = stringResource(Res.string.workspace_hint),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}
