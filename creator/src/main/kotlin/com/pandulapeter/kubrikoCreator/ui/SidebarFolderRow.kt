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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.RowActionsButton
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_group
import kubriko.creator.generated.resources.action_delete_folder
import kubriko.creator.generated.resources.action_expand_group
import kubriko.creator.generated.resources.action_more_options
import kubriko.creator.generated.resources.action_new_folder
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_folder_open
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A folder of the sidebar, opened and closed by a click. Its ⋮ (shown while it is pointed at) and its right-click menu
 * offer a new folder inside it, renaming it, and, while it is empty, deleting it.
 */
@Composable
internal fun SidebarFolderRow(
    row: FolderTreeRow.Folder,
    onToggle: () -> Unit,
    onNewFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(onClick = onToggle)
            .hoverReveal(hover)
            .rightClickMenu(menu)
            .padding(start = SIDEBAR_ROW_START - CHEVRON_ROOM + SIDEBAR_INDENT * row.depth, end = SIDEBAR_ROW_END),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val chevronRotation by animateFloatAsState(if (row.isExpanded) 0f else -90f, CreatorMotion.medium())
        Icon(
            modifier = Modifier.size(14.dp).rotate(chevronRotation),
            painter = painterResource(Res.drawable.ic_chevron),
            contentDescription = stringResource(if (row.isExpanded) Res.string.action_collapse_group else Res.string.action_expand_group),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Res.drawable.ic_folder_open),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp).weight(1f),
            text = row.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        RowActionsButton(
            modifier = Modifier.revealedWhen(hover.isActive || menu.isOpen),
            menu = menu,
            label = Res.string.action_more_options,
        )
        RightClickMenu(
            menu,
            listOfNotNull(
                ContextAction(stringResource(Res.string.action_new_folder), Res.drawable.ic_create_new_folder, onClick = onNewFolder),
                ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit, onClick = onRename),
                ContextAction(stringResource(Res.string.action_delete_folder), Res.drawable.ic_delete, isDestructive = true, onClick = onDelete).takeIf { row.isEmpty },
            ),
        )
    }
}

/**
 * The room the chevron and the space after it take before a folder's icon, which lines up with an item's.
 */
private val CHEVRON_ROOM = 18.dp
