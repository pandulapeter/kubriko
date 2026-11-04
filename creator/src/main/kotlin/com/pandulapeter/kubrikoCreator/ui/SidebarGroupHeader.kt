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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.RowActionsButton
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_more_options
import kubriko.creator.generated.resources.action_new_folder
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_create_new_folder
import org.jetbrains.compose.resources.stringResource

/**
 * The header of a sidebar group with folders in [module]: while it [isCreating], its + (at the end, as every panel
 * header's is) adds an item, and its ⋮ (shown while it is pointed at) and its right-click menu offer that and a new
 * folder, as a folder row's do; in a module Kubriko Creator doesn't add to, it offers neither. Its [subfolderActions], if
 * it holds folders, are the buttons before them. Dropping an item on it moves it out of its folder.
 */
@Composable
internal fun SidebarGroupHeader(
    group: FolderGroup,
    module: String,
    isCreating: Boolean,
    dragState: SidebarDragState,
    onNewItem: () -> Unit,
    onNewFolder: () -> Unit,
    subfolderActions: SubfolderActions?,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    PanelHeader(
        modifier = Modifier
            .folderDropTarget(dragState, SidebarDragState.FolderKey(group, "", module))
            .hoverReveal(hover)
            .then(if (isCreating) Modifier.rightClickMenu(menu) else Modifier),
        icon = group.icon,
        title = stringResource(group.title),
        accent = group.accent,
    ) {
        if (subfolderActions != null) {
            ExpandCollapseButtons(
                isShown = hover.isActive || menu.isOpen,
                actions = subfolderActions,
            )
        }
        if (!isCreating) return@PanelHeader
        RowActionsButton(
            modifier = Modifier.revealedWhen(hover.isActive || menu.isOpen),
            menu = menu,
            actions = listOf(
                ContextAction(stringResource(group.newItemLabel), Res.drawable.ic_add, onClick = onNewItem),
                ContextAction(stringResource(Res.string.action_new_folder), Res.drawable.ic_create_new_folder, onClick = onNewFolder),
            ),
            label = Res.string.action_more_options,
        )
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = group.newItemLabel,
            onClick = onNewItem,
        )
    }
}
