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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.ui.actorTypes.MenuIconButton
import com.pandulapeter.kubrikoCreator.ui.actorTypes.icon
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_group
import kubriko.creator.generated.resources.action_delete_folder
import kubriko.creator.generated.resources.action_expand_group
import kubriko.creator.generated.resources.action_folder_options
import kubriko.creator.generated.resources.action_new_actor_type
import kubriko.creator.generated.resources.action_new_folder
import kubriko.creator.generated.resources.action_new_manager_type
import kubriko.creator.generated.resources.action_new_scene
import kubriko.creator.generated.resources.action_rename_folder
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.ic_more
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.new_actor_type_hint
import kubriko.creator.generated.resources.new_folder_hint
import kubriko.creator.generated.resources.new_manager_type_hint
import kubriko.creator.generated.resources.new_scene_hint
import kubriko.creator.generated.resources.resource_group_actor_types
import kubriko.creator.generated.resources.resource_group_manager_types
import kubriko.creator.generated.resources.resource_group_scenes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
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
 * What the sidebar's groups with folders do. The ones that return a [StringResource] say why a name can't be used, or null.
 */
internal class FolderTreeActions(
    val onSelected: (FolderGroup, name: String) -> Unit,
    val onCreateItem: (FolderGroup, name: String) -> StringResource?,
    val onCreateFolder: (FolderGroup, path: String) -> StringResource?,
    val onRenameFolder: (FolderGroup, path: String, newName: String) -> StringResource?,
    val onDeleteFolder: (FolderGroup, path: String) -> Unit,
)

/**
 * A name being typed in a group of the sidebar: a new item, a new folder (in [parent], empty for none), or a folder's new name.
 */
internal sealed interface SidebarNaming {
    val group: FolderGroup

    data class NewItem(override val group: FolderGroup) : SidebarNaming

    data class NewFolder(override val group: FolderGroup, val parent: String) : SidebarNaming

    data class RenamedFolder(override val group: FolderGroup, val path: String) : SidebarNaming
}

/**
 * The header of a group and its tree of folders and items. An item is dragged into a folder (or onto the header, out of
 * every folder) to move it, and an Actor type into the open Scene to place it.
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
        PanelHeader(
            modifier = Modifier.folderDropTarget(dragState, SidebarDragState.FolderKey(group, "")),
            icon = group.icon,
            title = stringResource(group.title),
        ) {
            IconActionButton(
                icon = Res.drawable.ic_create_new_folder,
                label = Res.string.action_new_folder,
                onClick = { onNamingChange(SidebarNaming.NewFolder(group, "")) },
            )
            IconActionButton(
                icon = Res.drawable.ic_add,
                label = group.newItemLabel,
                onClick = { onNamingChange(SidebarNaming.NewItem(group)) },
            )
        }
    }
    if (naming == SidebarNaming.NewItem(group)) {
        item(key = "${group.name}-new") {
            NewNameField(
                placeholder = group.newItemHint,
                onCreate = { name -> actions.onCreateItem(group, name).also { if (it == null) onNamingChange(null) } },
                onCancel = { onNamingChange(null) },
            )
        }
    }
    if (naming == SidebarNaming.NewFolder(group, "")) {
        item(key = "${group.name}-new-folder") {
            NewFolderField(group, parent = "", depth = 0, actions, onNamingChange)
        }
    }
    items(folderTreeRows(state.items, state.folders, collapsed), key = { row -> "${group.name}-$row" }) { row ->
        when (row) {
            is FolderTreeRow.Folder -> if (naming == SidebarNaming.RenamedFolder(group, row.path)) {
                NewNameField(
                    placeholder = Res.string.new_folder_hint,
                    initialName = row.name,
                    indent = 24.dp + SIDEBAR_INDENT * row.depth,
                    onCreate = { name -> actions.onRenameFolder(group, row.path, name).also { if (it == null) onNamingChange(null) } },
                    onCancel = { onNamingChange(null) },
                )
            } else {
                Column {
                    FolderRow(
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
                    if (naming == SidebarNaming.NewFolder(group, row.path)) {
                        NewFolderField(group, row.path, row.depth + 1, actions, onNamingChange)
                    }
                }
            }
            is FolderTreeRow.Item -> SidebarItem(
                modifier = Modifier.sidebarDragSource(
                    dragState,
                    SidebarDragState.Dragged(row.name, group, state.items.firstOrNull { it.first == row.name }?.second.orEmpty()),
                ),
                icon = group.icon,
                text = FolderPaths.nameOf(row.name),
                depth = row.depth,
                isSelected = row.name == state.selectedName,
                onClick = { if (!dragState.isClickAfterDrag) actions.onSelected(group, row.name) },
                flashKey = flashKey?.takeIf { row.name == flashedName },
            )
        }
    }
}

@Composable
private fun NewFolderField(
    group: FolderGroup,
    parent: String,
    depth: Int,
    actions: FolderTreeActions,
    onNamingChange: (SidebarNaming?) -> Unit,
) = NewNameField(
    placeholder = Res.string.new_folder_hint,
    indent = 24.dp + SIDEBAR_INDENT * depth,
    onCreate = { name -> actions.onCreateFolder(group, FolderPaths.child(parent, name)).also { if (it == null) onNamingChange(null) } },
    onCancel = { onNamingChange(null) },
)

@Composable
private fun FolderRow(
    row: FolderTreeRow.Folder,
    onToggle: () -> Unit,
    onNewFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .height(28.dp)
        .clickable(onClick = onToggle)
        .padding(start = 12.dp + SIDEBAR_INDENT * row.depth, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(12.dp).rotate(if (row.isExpanded) 0f else -90f),
        painter = painterResource(Res.drawable.ic_chevron),
        contentDescription = stringResource(if (row.isExpanded) Res.string.action_collapse_group else Res.string.action_expand_group),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Icon(
        modifier = Modifier.size(14.dp),
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
    MenuIconButton(
        icon = Res.drawable.ic_more,
        label = Res.string.action_folder_options,
        options = listOfNotNull(FolderOption.NEW_FOLDER, FolderOption.RENAME, FolderOption.DELETE.takeIf { row.isEmpty }),
        optionText = { stringResource(it.title) },
        optionIcon = { it.icon },
        onSelected = { option ->
            when (option) {
                FolderOption.NEW_FOLDER -> onNewFolder()
                FolderOption.RENAME -> onRename()
                FolderOption.DELETE -> onDelete()
            }
        },
    )
}

private enum class FolderOption(val title: StringResource, val icon: DrawableResource) {
    NEW_FOLDER(Res.string.action_new_folder, Res.drawable.ic_create_new_folder),
    RENAME(Res.string.action_rename_folder, Res.drawable.ic_text),
    DELETE(Res.string.action_delete_folder, Res.drawable.ic_delete),
}

/**
 * Makes the element where something dragged of [key]'s group moves to [key]'s folder, highlighted while it is over it.
 */
@Composable
private fun Modifier.folderDropTarget(dragState: SidebarDragState, key: SidebarDragState.FolderKey): Modifier {
    DisposableEffect(dragState, key) {
        onDispose { dragState.folderTargets.remove(key) }
    }
    return onGloballyPositioned { dragState.folderTargets[key] = it.boundsInRoot() }
        .then(if (dragState.hoveredFolder == key) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
}

internal val FolderGroup.icon
    get() = when (this) {
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> checkNotNull(kind).icon
        FolderGroup.SCENES -> Res.drawable.ic_scene
    }

private val FolderGroup.title
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.resource_group_actor_types
        FolderGroup.MANAGER_TYPES -> Res.string.resource_group_manager_types
        FolderGroup.SCENES -> Res.string.resource_group_scenes
    }

private val FolderGroup.newItemLabel
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.action_new_actor_type
        FolderGroup.MANAGER_TYPES -> Res.string.action_new_manager_type
        FolderGroup.SCENES -> Res.string.action_new_scene
    }

private val FolderGroup.newItemHint
    get() = when (this) {
        FolderGroup.ACTOR_TYPES -> Res.string.new_actor_type_hint
        FolderGroup.MANAGER_TYPES -> Res.string.new_manager_type_hint
        FolderGroup.SCENES -> Res.string.new_scene_hint
    }
