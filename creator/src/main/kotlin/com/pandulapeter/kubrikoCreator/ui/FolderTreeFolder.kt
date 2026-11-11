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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup

/**
 * A folder of [group]'s tree in [module], [indent] levels further in, or the field renaming it, followed by the field
 * naming a new folder in it while one is. While [isCreating], it is dragged into another folder of the group with
 * everything in it.
 *
 * @param groupKey The key of the group's tree ([SidebarKeys]), which the folder's own key is built from.
 */
@Composable
internal fun FolderTreeFolder(
    module: String,
    group: FolderGroup,
    groupKey: String,
    row: FolderTreeRow.Folder,
    indent: Int,
    isCreating: Boolean,
    actions: FolderTreeActions,
    dragState: SidebarDragState,
    tree: SidebarTree,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    val key = SidebarKeys.folder(groupKey, row.path)
    if (isCreating && naming == SidebarNaming.RenamedFolder(group, row.path)) {
        SidebarNameField(
            group = group,
            isFolder = true,
            depth = row.depth + indent,
            initialName = row.name,
            onName = { name ->
                actions.onRenameFolder(group, row.path, name) { tree.followFolder(groupKey, row.path, FolderPaths.child(FolderPaths.parentOf(row.path), name)) }
            },
            onNamingChange = onNamingChange,
        )
    } else {
        SidebarFolderRow(
            modifier = Modifier.folderDropTarget(dragState, SidebarDragState.FolderKey(group, row.path, module)).then(
                if (isCreating) {
                    Modifier.sidebarDragSource(dragState, SidebarDragState.Dragged(row.path, group, FolderPaths.parentOf(row.path), module, movedFolderOf = groupKey))
                } else {
                    Modifier
                },
            ),
            name = row.name,
            depth = row.depth + indent,
            isExpanded = row.isExpanded,
            onToggle = { if (!dragState.isClickAfterDrag) tree.toggle(key) },
            actions = if (isCreating) {
                folderActions(
                    onNewFolder = {
                        tree.expand(key)
                        onNamingChange(SidebarNaming.NewFolder(group, row.path))
                    },
                    onRename = { onNamingChange(SidebarNaming.RenamedFolder(group, row.path)) },
                    onDelete = { actions.onDeleteFolder(group, row.path, row.isEmpty) },
                )
            } else {
                emptyList()
            },
            subfolderActions = tree.subfolderActions(key),
        )
    }
    if (isCreating && naming == SidebarNaming.NewFolder(group, row.path)) {
        SidebarNameField(
            group = group,
            isFolder = true,
            depth = row.depth + indent + 1,
            onName = { name -> actions.onCreateFolder(group, FolderPaths.child(row.path, name)) },
            onNamingChange = onNamingChange,
        )
    }
}
