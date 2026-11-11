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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import org.jetbrains.compose.resources.stringResource

/**
 * The sprites, sounds, or music of [module] ([group]) as a tree of their folders ([SidebarModule.resourceTrees]),
 * [indent] levels in, after the field naming a new folder at the group's top while one is. While [isChangeable] (the
 * module new files go to), its folders offer a new folder, Rename, and Delete, and its files Rename and Delete, and a
 * file dragged onto a folder, or onto any row, moves into that row's folder, as the groups with folders do; another
 * module's are only shown, and so are the images the game doesn't take for sprites, with the folder they are in.
 *
 * @param rows The rows of every tree ([SidebarRows]), worked out outside the list's builder.
 */
internal fun LazyListScope.resourceTreeItems(
    module: SidebarModule,
    group: ResourceGroup,
    indent: Int,
    isChangeable: Boolean,
    actions: FolderTreeActions,
    dragState: SidebarDragState,
    tree: SidebarTree,
    rows: SidebarRows,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
) {
    val folderGroup = group.folderGroup ?: return
    val state = module.resourceTrees[group] ?: return
    val groupKey = SidebarKeys.resourceGroup(module.path, group)
    if (isChangeable && naming == SidebarNaming.NewFolder(folderGroup, state.root)) {
        item(key = "$groupKey-new-folder") {
            SidebarNameField(
                modifier = sidebarItemMotion(),
                group = folderGroup,
                isFolder = true,
                depth = indent,
                onName = { name -> actions.onCreateFolder(folderGroup, FolderPaths.child(state.root, name)) },
                onNamingChange = onNamingChange,
            )
        }
    }
    val groupRows = rows.resourceTreeOf(module, group, tree) ?: return
    items(
        groupRows.rows,
        key = { row ->
            when (row) {
                is FolderTreeRow.Folder -> "$groupKey-folder-${row.path}"
                is FolderTreeRow.Item -> "$groupKey-item-${row.name}"
            }
        },
    ) { row ->
        when (row) {
            is FolderTreeRow.Folder -> FolderTreeFolder(
                modifier = sidebarItemMotion(),
                module = module.path,
                group = folderGroup,
                groupKey = groupKey,
                row = row,
                indent = indent,
                isCreating = isChangeable,
                actions = actions,
                dragState = dragState,
                tree = tree,
                naming = naming,
                onNamingChange = onNamingChange,
            )
            is FolderTreeRow.Item -> ResourceTreeItem(
                modifier = sidebarItemMotion(),
                module = module.path,
                group = folderGroup,
                row = row,
                indent = indent,
                folder = groupRows.folderOfItem[row.name].orEmpty(),
                isChangeable = isChangeable,
                isRenaming = naming == SidebarNaming.RenamedItem(folderGroup, row.name),
                actions = actions,
                dragState = dragState,
                onNamingChange = onNamingChange,
            )
        }
    }
}

/**
 * A file of [group]'s tree in [module], by its path from `composeResources` (or for an image the game doesn't take for a
 * sprite, from the project's root, which is only shown), in [folder], [indent] levels further in, or the field renaming
 * it while [isRenaming].
 */
@Composable
private fun ResourceTreeItem(
    module: String,
    group: FolderGroup,
    row: FolderTreeRow.Item,
    indent: Int,
    folder: String,
    isChangeable: Boolean,
    isRenaming: Boolean,
    actions: FolderTreeActions,
    dragState: SidebarDragState,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOwnFile = ResourceKind.of(row.name) != null
    val canChange = isChangeable && isOwnFile
    if (canChange && isRenaming) {
        SidebarNameField(
            modifier = modifier,
            group = group,
            isFolder = false,
            depth = row.depth + indent,
            initialName = ResourceKind.nameOf(row.name),
            onName = { name -> actions.onRenameItem(group, row.name, name) },
            onNamingChange = onNamingChange,
        )
    } else {
        SidebarItem(
            modifier = if (canChange) {
                modifier
                    .itemDropTarget(dragState, SidebarDragState.FolderKey(group, folder, module), id = Triple(module, group, row.name))
                    .sidebarDragSource(dragState, SidebarDragState.Dragged(row.name, group, folder, module))
            } else {
                modifier
            },
            icon = group.icon,
            accent = Accent.CODE,
            text = FolderPaths.nameOf(row.name),
            detail = if (!isOwnFile) otherImageDetailOf(row.name) else null,
            depth = row.depth + indent,
            onClick = null,
            actions = if (canChange) {
                listOf(
                    ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit) { onNamingChange(SidebarNaming.RenamedItem(group, row.name)) },
                    ContextAction(stringResource(group.deleteItemLabel), Res.drawable.ic_delete, isDestructive = true) { actions.onDeleteItem(group, row.name) },
                )
            } else {
                emptyList()
            },
        )
    }
}
