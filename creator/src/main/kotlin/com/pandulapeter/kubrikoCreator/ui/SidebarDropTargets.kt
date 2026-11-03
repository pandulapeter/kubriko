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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Makes a folder row or a group header (an empty path) where something dragged of [key]'s group moves to [key]'s folder,
 * highlighted while something dragged would move there, from over it or over one of its items.
 */
@Composable
internal fun Modifier.folderDropTarget(dragState: SidebarDragState, key: SidebarDragState.FolderKey): Modifier = dropTarget(dragState, key, id = key)
    .then(if (dragState.hoveredFolder == key) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)

/**
 * Makes an item's row, known by [id], a place where something dragged of [folder]'s group moves into [folder], next to it.
 */
@Composable
internal fun Modifier.itemDropTarget(dragState: SidebarDragState, folder: SidebarDragState.FolderKey, id: Any): Modifier = dropTarget(dragState, folder, id)

@Composable
private fun Modifier.dropTarget(dragState: SidebarDragState, folder: SidebarDragState.FolderKey, id: Any): Modifier {
    DisposableEffect(dragState, id) {
        onDispose { dragState.dropTargets.remove(id) }
    }
    return onGloballyPositioned { dragState.dropTargets[id] = SidebarDragState.DropTarget(folder, it.boundsInRoot()) }
}
