/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_new
import kubriko.tools.scene_editor.generated.resources.ic_open
import kubriko.tools.scene_editor.generated.resources.ic_save
import kubriko.tools.scene_editor.generated.resources.ic_settings
import kubriko.tools.scene_editor.generated.resources.ic_sync

// TODO: Implement confirmation dialogs for actions
@Composable
internal fun FileManagerRow(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    currentFileName: String,
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
            contentDescription = "New",
            onClick = onNewIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_open,
            contentDescription = "Open",
            onClick = onOpenIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_save,
            contentDescription = "Save",
            onClick = onSaveIconClicked,
        )
        if (isConnected) {
            EditorIcon(
                drawableResource = Res.drawable.ic_sync,
                contentDescription = "Sync",
                onClick = onSyncIconClicked,
            )
        }
        EditorText(
            modifier = Modifier.weight(1f),
            text = currentFileName,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_settings,
            contentDescription = "Editor Settings",
            onClick = onSettingsIconClicked,
        )
    }
}