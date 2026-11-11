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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.ui.components.DragPreviewChip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_file

/**
 * What is being dragged from the sidebar, following the pointer.
 */
@Composable
internal fun SidebarDragPreview(state: SidebarDragState) {
    val dragged = state.dragged ?: return
    DragPreviewChip(
        pointer = { state.pointer },
        icon = dragged.group?.icon ?: Res.drawable.ic_file,
        tint = MaterialTheme.colorScheme.tertiary,
        text = if (dragged.group?.resourceKind != null) FolderPaths.nameOf(dragged.name) else dragged.name,
    )
}
