/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.fileManagerRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.action_new
import kubriko.tools.scene_editor.generated.resources.action_open
import kubriko.tools.scene_editor.generated.resources.action_redo
import kubriko.tools.scene_editor.generated.resources.action_save
import kubriko.tools.scene_editor.generated.resources.action_sync
import kubriko.tools.scene_editor.generated.resources.action_undo
import kubriko.tools.scene_editor.generated.resources.editor_settings
import kubriko.tools.scene_editor.generated.resources.ic_new
import kubriko.tools.scene_editor.generated.resources.ic_open
import kubriko.tools.scene_editor.generated.resources.ic_redo
import kubriko.tools.scene_editor.generated.resources.ic_save
import kubriko.tools.scene_editor.generated.resources.ic_settings
import kubriko.tools.scene_editor.generated.resources.ic_sync
import kubriko.tools.scene_editor.generated.resources.ic_undo
import kubriko.tools.scene_editor.generated.resources.modified_file_name
import org.jetbrains.compose.resources.stringResource

// TODO: Implement confirmation dialogs for actions
@Composable
internal fun FileManagerRow(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    currentFileName: String,
    isSceneModified: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoIconClicked: () -> Unit,
    onRedoIconClicked: () -> Unit,
    onNewIconClicked: () -> Unit,
    onOpenIconClicked: () -> Unit,
    onSaveIconClicked: () -> Unit,
    onSyncIconClicked: () -> Unit,
    onSettingsIconClicked: () -> Unit,
) = EditorSurface(
    modifier = modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditorIcon(
            drawableResource = Res.drawable.ic_new,
            contentDescription = stringResource(Res.string.action_new),
            onClick = onNewIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_open,
            contentDescription = stringResource(Res.string.action_open),
            onClick = onOpenIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_save,
            contentDescription = stringResource(Res.string.action_save),
            isEnabled = isSceneModified,
            onClick = onSaveIconClicked,
        )
        if (isConnected) {
            EditorIcon(
                drawableResource = Res.drawable.ic_sync,
                contentDescription = stringResource(Res.string.action_sync),
                onClick = onSyncIconClicked,
            )
        }
        VerticalDivider(
            modifier = Modifier.height(24.dp),
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_undo,
            contentDescription = stringResource(Res.string.action_undo),
            isEnabled = canUndo,
            onClick = onUndoIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_redo,
            contentDescription = stringResource(Res.string.action_redo),
            isEnabled = canRedo,
            onClick = onRedoIconClicked,
        )
        VerticalDivider(
            modifier = Modifier.height(24.dp),
        )
        EditorText(
            modifier = Modifier.weight(1f),
            text = if (isSceneModified) stringResource(Res.string.modified_file_name, currentFileName) else currentFileName,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_settings,
            contentDescription = stringResource(Res.string.editor_settings),
            onClick = onSettingsIconClicked,
        )
    }
}