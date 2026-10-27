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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.ui.LocalAppWindow
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_scene
import kubriko.creator.generated.resources.action_open_scene_in_window
import kubriko.creator.generated.resources.action_reload_scene
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_open_in_new
import kubriko.creator.generated.resources.ic_refresh
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.scene_outdated
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A Scene, edited inside Kubriko Creator's window, with a header naming it and offering to reload it (when its Actor
 * types changed), to open it in a window of its own, or to close it.
 */
@Composable
internal fun SceneEditorPane(
    state: SceneEditingState.Open,
    onReload: () -> Unit,
    onOpenInWindow: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Res.drawable.ic_scene),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = state.scene.name,
            style = MaterialTheme.typography.titleSmall,
        )
        if (state.isOutdated) {
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
        Spacer(Modifier.weight(1f))
        IconActionButton(icon = Res.drawable.ic_refresh, label = Res.string.action_reload_scene, onClick = onReload)
        IconActionButton(icon = Res.drawable.ic_open_in_new, label = Res.string.action_open_scene_in_window, onClick = onOpenInWindow)
        IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_close_scene, onClick = onClose)
    }
    HorizontalDivider()
    key(state.revision) {
        val serializationManager = remember { state.entryPoint.createSerializationManager() }
        val customManagers = remember { state.entryPoint.createCustomManagers() }
        SceneEditor.Content(
            defaultSceneFilename = state.scene.name,
            defaultSceneFolderPath = state.scene.file.parentFile.absolutePath,
            serializationManager = serializationManager,
            customManagers = customManagers,
            sceneEditorMode = SceneEditorMode.Normal,
            parentWindow = LocalAppWindow.current,
            onCloseRequest = onClose,
            // Creator's own layout already keeps clear of the title bar, which the editor would otherwise pad for again.
            modifier = Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.safeDrawing),
        )
    }
}
