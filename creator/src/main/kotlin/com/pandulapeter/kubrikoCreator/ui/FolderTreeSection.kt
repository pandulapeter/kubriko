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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.ui.actorTypes.accent
import com.pandulapeter.kubrikoCreator.ui.actorTypes.icon
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.NewNameField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.action_delete_manager_type
import kubriko.creator.generated.resources.action_delete_scene
import kubriko.creator.generated.resources.action_new_actor_type
import kubriko.creator.generated.resources.action_new_manager_type
import kubriko.creator.generated.resources.action_new_scene
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.new_actor_type_hint
import kubriko.creator.generated.resources.new_folder_hint
import kubriko.creator.generated.resources.new_manager_type_hint
import kubriko.creator.generated.resources.new_scene_hint
import kubriko.creator.generated.resources.resource_group_actor_types
import kubriko.creator.generated.resources.resource_group_manager_types
import kubriko.creator.generated.resources.resource_group_scenes
import org.jetbrains.compose.resources.stringResource
/**
 * What a sidebar group with folders shows.
 *
 * @param items The names of its types or Scenes, each with the folder it is in (empty for none).
 * @param folders Every folder of the group, empty ones too.
 * @param selectedName The item shown in the center, if it is one of these.
 */
internal data class FolderTreeGroupState(
    val items: List<Pair<String, String>>,
    val folders: Set<String>,
    val selectedName: String? = null,
)

/**
 * What the sidebar's groups with folders do. The ones that return a [Rejection] say why a name can't be used, or null.
 */
internal class FolderTreeActions(
    val onSelected: (FolderGroup, name: String) -> Unit,
    val onSelectionCleared: () -> Unit,
    val onCreateItem: (FolderGroup, name: String) -> Rejection?,
    val onCreateFolder: (FolderGroup, path: String) -> Rejection?,
    val onRenameFolder: (FolderGroup, path: String, newName: String) -> Rejection?,
    val onDeleteFolder: (FolderGroup, path: String) -> Unit,
    val onRenameItem: (FolderGroup, name: String, newName: String) -> Rejection?,
    val onDeleteItem: (FolderGroup, name: String) -> Unit,
)

/**
 * A name being typed in a group of the sidebar: a new item, a new folder (in [parent], empty for none), or a new name for a
 * folder or an item.
 */
internal sealed interface SidebarNaming {
    val group: FolderGroup

    data class NewItem(override val group: FolderGroup) : SidebarNaming

    data class NewFolder(override val group: FolderGroup, val parent: String) : SidebarNaming

    data class RenamedFolder(override val group: FolderGroup, val path: String) : SidebarNaming

    data class RenamedItem(override val group: FolderGroup, val name: String) : SidebarNaming
}

/**
 * The header of a group and its tree of folders and items. Every row offers what can be done to it the same two ways: a ⋮
 * button that shows while it is pointed at, and its right-click menu. What is being named appears where it will be, as a
 * field that looks like its row. An item dragged onto a folder, or onto any row, moves into that row's folder (onto the
 * header or an item outside every folder, out of them all), and an Actor type dragged into the open Scene is placed there.
 *
 * @param collapsed The folders whose contents are hidden.
 * @param naming What is being named in the sidebar, if anything.
 */
internal fun LazyListScope.folderTreeSection(
    group: FolderGroup,
    state: FolderTreeGroupState,
    actions: FolderTreeActions,
    dragState: SidebarDragState,
    collapsed: Set<String>,
    onCollapsedChange: (Set<String>) -> Unit,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    flashedName: String?,
    flashKey: Any?,
) {
    item(key = "${group.name}-header") {
        SidebarGroupHeader(
            group = group,
            dragState = dragState,
            onNewItem = { onNamingChange(SidebarNaming.NewItem(group)) },
            onNewFolder = { onNamingChange(SidebarNaming.NewFolder(group, "")) },
        )
    }
    if (naming == SidebarNaming.NewItem(group)) {
        item(key = "${group.name}-new") {
            SidebarNameField(
                group = group,
                isFolder = false,
                depth = 0,
                onName = { name -> actions.onCreateItem(group, name) },
                onNamingChange = onNamingChange,
            )
        }
    }
    if (naming == SidebarNaming.NewFolder(group, "")) {
        item(key = "${group.name}-new-folder") {
            SidebarNameField(
                group = group,
                isFolder = true,
                depth = 0,
                onName = { name -> actions.onCreateFolder(group, name) },
                onNamingChange = onNamingChange,
            )
        }
    }
    val folderOfItem = state.items.toMap()
    items(
        folderTreeRows(state.items, state.folders, collapsed),
        key = { row ->
            when (row) {
                is FolderTreeRow.Folder -> "${group.name}-folder-${row.path}"
                is FolderTreeRow.Item -> "${group.name}-item-${row.name}"
            }
        },
    ) { row ->
        when (row) {
            is FolderTreeRow.Folder -> Column(Modifier.animateItem()) {
                if (naming == SidebarNaming.RenamedFolder(group, row.path)) {
                    SidebarNameField(
                        group = group,
                        isFolder = true,
                        depth = row.depth,
                        initialName = row.name,
                        onName = { name -> actions.onRenameFolder(group, row.path, name) },
                        onNamingChange = onNamingChange,
                    )
                } else {
                    SidebarFolderRow(
                        modifier = Modifier.folderDropTarget(dragState, SidebarDragState.FolderKey(group, row.path)),
                        row = row,
                        onToggle = { onCollapsedChange(if (row.isExpanded) collapsed + row.path else collapsed - row.path) },
                        onNewFolder = {
                            onCollapsedChange(collapsed - row.path)
                            onNamingChange(SidebarNaming.NewFolder(group, row.path))
                        },
                        onRename = { onNamingChange(SidebarNaming.RenamedFolder(group, row.path)) },
                        onDelete = { actions.onDeleteFolder(group, row.path) },
                    )
                }
                if (naming == SidebarNaming.NewFolder(group, row.path)) {
                    SidebarNameField(
                        group = group,
                        isFolder = true,
                        depth = row.depth + 1,
                        onName = { name -> actions.onCreateFolder(group, FolderPaths.child(row.path, name)) },
                        onNamingChange = onNamingChange,
                    )
                }
            }
            is FolderTreeRow.Item -> FolderTreeItem(
                modifier = Modifier.animateItem(),
                group = group,
                row = row,
                folder = folderOfItem[row.name].orEmpty(),
                isSelected = row.name == state.selectedName,
                isRenaming = naming == SidebarNaming.RenamedItem(group, row.name),
                actions = actions,
                dragState = dragState,
                onNamingChange = onNamingChange,
                flashKey = flashKey?.takeIf { row.name == flashedName },
            )
        }
    }
}

/**
 * An item of [group]'s tree in [folder], or the field renaming it while [isRenaming].
 */
@Composable
private fun FolderTreeItem(
    group: FolderGroup,
    row: FolderTreeRow.Item,
    folder: String,
    isSelected: Boolean,
    isRenaming: Boolean,
    actions: FolderTreeActions,
    dragState: SidebarDragState,
    onNamingChange: (SidebarNaming?) -> Unit,
    flashKey: Any?,
    modifier: Modifier = Modifier,
) = if (isRenaming) {
    SidebarNameField(
        modifier = modifier,
        group = group,
        isFolder = false,
        depth = row.depth,
        initialName = FolderPaths.nameOf(row.name),
        onName = { name -> actions.onRenameItem(group, row.name, name) },
        onNamingChange = onNamingChange,
    )
} else {
    SidebarItem(
        modifier = modifier
            .itemDropTarget(dragState, SidebarDragState.FolderKey(group, folder), id = group to row.name)
            .sidebarDragSource(dragState, SidebarDragState.Dragged(row.name, group, folder)),
        icon = group.icon,
        accent = group.accent,
        text = FolderPaths.nameOf(row.name),
        depth = row.depth,
        isSelected = isSelected,
        onClick = {
            when {
                dragState.isClickAfterDrag -> Unit
                isSelected -> actions.onSelectionCleared()
                else -> actions.onSelected(group, row.name)
            }
        },
        flashKey = flashKey,
        actions = listOf(
            ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit) { onNamingChange(SidebarNaming.RenamedItem(group, row.name)) },
            ContextAction(stringResource(group.deleteItemLabel), Res.drawable.ic_delete, isDestructive = true) { actions.onDeleteItem(group, row.name) },
        ),
    )
}

/**
 * A name being typed in [group]'s tree, at [depth], looking like the row it names: a folder's when [isFolder], an item's
 * otherwise. [onName] says why a name can't be used, or null once it is used, which ends the naming.
 */
@Composable
private fun SidebarNameField(
    group: FolderGroup,
    isFolder: Boolean,
    depth: Int,
    onName: (String) -> Rejection?,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
) = Box(modifier) {
    NewNameField(
        placeholder = if (isFolder) Res.string.new_folder_hint else group.newItemHint,
        initialName = initialName,
        indent = SIDEBAR_ROW_START + SIDEBAR_INDENT * depth,
        confirmLabel = if (initialName.isEmpty()) Res.string.action_add else Res.string.action_rename,
        icon = if (isFolder) Res.drawable.ic_folder_open else group.icon,
        iconTint = if (isFolder) Color.Unspecified else group.accent.color,
        onCreate = { name -> onName(name).also { if (it == null) onNamingChange(null) }?.toFieldError() },
        onCancel = { onNamingChange(null) },
    )
}

internal val FolderGroup.icon
    get() = when (this) {
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> checkNotNull(kind).icon
        FolderGroup.SCENES -> Res.drawable.ic_scene
    }

internal val FolderGroup.accent
    get() = when (this) {
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> checkNotNull(kind).accent
        FolderGroup.SCENES -> Accent.SCENE
    }

internal val FolderGroup.title
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.resource_group_actor_types
        FolderGroup.MANAGER_TYPES -> Res.string.resource_group_manager_types
        FolderGroup.SCENES -> Res.string.resource_group_scenes
    }

internal val FolderGroup.newItemLabel
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.action_new_actor_type
        FolderGroup.MANAGER_TYPES -> Res.string.action_new_manager_type
        FolderGroup.SCENES -> Res.string.action_new_scene
    }

internal val FolderGroup.deleteItemLabel
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.action_delete_actor_type
        FolderGroup.MANAGER_TYPES -> Res.string.action_delete_manager_type
        FolderGroup.SCENES -> Res.string.action_delete_scene
    }

internal val FolderGroup.newItemHint
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.new_actor_type_hint
        FolderGroup.MANAGER_TYPES -> Res.string.new_manager_type_hint
        FolderGroup.SCENES -> Res.string.new_scene_hint
    }
