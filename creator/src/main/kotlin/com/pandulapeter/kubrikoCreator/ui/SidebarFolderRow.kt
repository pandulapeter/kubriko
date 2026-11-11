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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RowActionsButton
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_delete_folder
import kubriko.creator.generated.resources.action_more_options
import kubriko.creator.generated.resources.action_new_folder
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_folder_open
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A folder of the sidebar, opened and closed by a click, drawn with [icon]. Its [actions], if any, are offered by its ⋮
 * (shown while it is pointed at) and its right-click menu, and its [subfolderActions], if it holds folders, by the
 * buttons laid over the end of its name while it is pointed at. While [isTopLevel] (a folder of documents beside the
 * groups), it is laid out as a group header is: lined up with them, its chevron at the end.
 */
@Composable
internal fun SidebarFolderRow(
    name: String,
    depth: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.ic_folder_open,
    actions: List<ContextAction> = emptyList(),
    subfolderActions: SubfolderActions? = null,
    isTopLevel: Boolean = false,
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
            .padding(
                start = (if (isTopLevel) SIDEBAR_HEADER_START else SIDEBAR_ROW_START - CHEVRON_ROOM) + SIDEBAR_INDENT * depth,
                end = if (actions.isEmpty() && subfolderActions == null && !isTopLevel) 12.dp else SIDEBAR_ROW_END,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isTopLevel) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isTopLevel) {
            SidebarChevron(
                modifier = Modifier.size(14.dp),
                isExpanded = isExpanded,
            )
        }
        Box(
            modifier = if (isTopLevel) Modifier.size(SIDEBAR_HEADER_ICON) else Modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SidebarRowTitle(
            modifier = Modifier.padding(start = if (isTopLevel) 0.dp else 4.dp).weight(1f),
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            isPointedAt = hover.isActive || menu.isOpen,
            subfolderActions = subfolderActions,
            background = MaterialTheme.colorScheme.surfaceContainerLow,
        )
        if (actions.isNotEmpty()) {
            RowActionsButton(
                modifier = Modifier.revealedWhen(hover.isActive || menu.isOpen),
                menu = menu,
                actions = actions,
                label = Res.string.action_more_options,
            )
        }
        if (isTopLevel) {
            SidebarChevron(
                modifier = Modifier.padding(end = 8.dp).size(16.dp),
                isExpanded = isExpanded,
            )
        }
    }
}

/**
 * What a folder of a group with folders offers: a new folder inside it, renaming it, and deleting it with what it holds.
 */
@Composable
internal fun folderActions(
    onNewFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) = listOf(
    ContextAction(stringResource(Res.string.action_new_folder), Res.drawable.ic_create_new_folder, onClick = onNewFolder),
    ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit, onClick = onRename),
    ContextAction(stringResource(Res.string.action_delete_folder), Res.drawable.ic_delete, isDestructive = true, onClick = onDelete),
)

/**
 * The room the chevron and the space after it take before a folder's icon, which lines up with an item's.
 */
private val CHEVRON_ROOM = 18.dp
