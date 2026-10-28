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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
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
import kubriko.creator.generated.resources.action_rename_folder
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.ic_more
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.new_actor_type_hint
import kubriko.creator.generated.resources.new_folder_hint
import kubriko.creator.generated.resources.new_manager_type_hint
import kubriko.creator.generated.resources.resource_group_actor_types
import kubriko.creator.generated.resources.resource_group_manager_types
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Actor types and Manager types shown at the top of the sidebar, for projects that opted in to them.
 *
 * @param actorTypes Each Actor type's name and folder.
 * @param folders Every folder of each kind of type.
 */
internal data class BlockTypeTreeState(
    val actorTypes: List<Pair<String, String>>,
    val managerTypes: List<Pair<String, String>>,
    val folders: Map<BlockTypeKind, Set<String>>,
    val selectedName: String?,
) {
    fun typesOf(kind: BlockTypeKind) = if (kind == BlockTypeKind.ACTOR) actorTypes else managerTypes
}

/**
 * What the sidebar's type groups do. The ones that return a [StringResource] say why a name can't be used, or null.
 */
internal class BlockTypeTreeActions(
    val onSelected: (String) -> Unit,
    val onCreateType: (BlockTypeKind, String) -> StringResource?,
    val onCreateFolder: (BlockTypeKind, path: String) -> StringResource?,
    val onRenameFolder: (BlockTypeKind, path: String, newName: String) -> StringResource?,
    val onDeleteFolder: (BlockTypeKind, path: String) -> Unit,
)

/**
 * A name being typed in a type group: a new type, a new folder (in [parent], empty for none), or a folder's new name.
 */
internal sealed interface BlockTypeNaming {
    val kind: BlockTypeKind

    data class NewType(override val kind: BlockTypeKind) : BlockTypeNaming

    data class NewFolder(override val kind: BlockTypeKind, val parent: String) : BlockTypeNaming

    data class RenamedFolder(override val kind: BlockTypeKind, val path: String) : BlockTypeNaming
}

/**
 * The header of the Actor types or the Manager types, and their tree of folders and types. A type is dragged into a
 * folder (or onto the header, out of every folder) to move it, and an Actor type into the open Scene to place it.
 *
 * @param collapsed The folders whose contents are hidden.
 * @param naming What is being named in the sidebar, if anything.
 */
internal fun LazyListScope.blockTypeSection(
    kind: BlockTypeKind,
    state: BlockTypeTreeState,
    actions: BlockTypeTreeActions,
    dragState: BlockTypeDragState,
    collapsed: Set<String>,
    onCollapsedChange: (Set<String>) -> Unit,
    naming: BlockTypeNaming?,
    onNamingChange: (BlockTypeNaming?) -> Unit,
    flashedName: String?,
    flashKey: Any?,
) {
    item(key = "${kind.name}-header") {
        PanelHeader(
            modifier = Modifier.folderDropTarget(dragState, BlockTypeDragState.FolderKey(kind, "")),
            icon = kind.icon,
            title = stringResource(if (kind == BlockTypeKind.ACTOR) Res.string.resource_group_actor_types else Res.string.resource_group_manager_types),
        ) {
            IconActionButton(
                icon = Res.drawable.ic_create_new_folder,
                label = Res.string.action_new_folder,
                onClick = { onNamingChange(BlockTypeNaming.NewFolder(kind, "")) },
            )
            IconActionButton(
                icon = Res.drawable.ic_add,
                label = if (kind == BlockTypeKind.ACTOR) Res.string.action_new_actor_type else Res.string.action_new_manager_type,
                onClick = { onNamingChange(BlockTypeNaming.NewType(kind)) },
            )
        }
    }
    if (naming == BlockTypeNaming.NewType(kind)) {
        item(key = "${kind.name}-new") {
            NewNameField(
                placeholder = if (kind == BlockTypeKind.ACTOR) Res.string.new_actor_type_hint else Res.string.new_manager_type_hint,
                onCreate = { name -> actions.onCreateType(kind, name).also { if (it == null) onNamingChange(null) } },
                onCancel = { onNamingChange(null) },
            )
        }
    }
    if (naming == BlockTypeNaming.NewFolder(kind, "")) {
        item(key = "${kind.name}-new-folder") {
            NewFolderField(kind, parent = "", depth = 0, actions, onNamingChange)
        }
    }
    items(folderTreeRows(state.typesOf(kind), state.folders[kind].orEmpty(), collapsed), key = { row -> "${kind.name}-$row" }) { row ->
        when (row) {
            is FolderTreeRow.Folder -> if (naming == BlockTypeNaming.RenamedFolder(kind, row.path)) {
                NewNameField(
                    placeholder = Res.string.new_folder_hint,
                    initialName = row.name,
                    indent = 24.dp + SIDEBAR_INDENT * row.depth,
                    onCreate = { name -> actions.onRenameFolder(kind, row.path, name).also { if (it == null) onNamingChange(null) } },
                    onCancel = { onNamingChange(null) },
                )
            } else {
                Column {
                FolderRow(
                    modifier = Modifier.folderDropTarget(dragState, BlockTypeDragState.FolderKey(kind, row.path)),
                    row = row,
                    onToggle = { onCollapsedChange(if (row.isExpanded) collapsed + row.path else collapsed - row.path) },
                    onNewFolder = {
                        onCollapsedChange(collapsed - row.path)
                        onNamingChange(BlockTypeNaming.NewFolder(kind, row.path))
                    },
                    onRename = { onNamingChange(BlockTypeNaming.RenamedFolder(kind, row.path)) },
                    onDelete = { actions.onDeleteFolder(kind, row.path) },
                )
                if (naming == BlockTypeNaming.NewFolder(kind, row.path)) {
                    NewFolderField(kind, row.path, row.depth + 1, actions, onNamingChange)
                }
                }
            }
            is FolderTreeRow.Item -> SidebarItem(
                modifier = Modifier.blockTypeDragSource(
                    dragState,
                    BlockTypeDragState.Dragged(row.name, kind, state.typesOf(kind).firstOrNull { it.first == row.name }?.second.orEmpty()),
                ),
                icon = kind.icon,
                text = row.name,
                depth = row.depth,
                isSelected = row.name == state.selectedName,
                onClick = { if (!dragState.isClickAfterDrag) actions.onSelected(row.name) },
                flashKey = flashKey?.takeIf { row.name == flashedName },
            )
        }
    }
}

@Composable
private fun NewFolderField(
    kind: BlockTypeKind,
    parent: String,
    depth: Int,
    actions: BlockTypeTreeActions,
    onNamingChange: (BlockTypeNaming?) -> Unit,
) = NewNameField(
    placeholder = Res.string.new_folder_hint,
    indent = 24.dp + SIDEBAR_INDENT * depth,
    onCreate = { name -> actions.onCreateFolder(kind, FolderPaths.child(parent, name)).also { if (it == null) onNamingChange(null) } },
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
 * Makes the element where a dragged type of [key]'s kind moves to [key]'s folder, highlighted while one is over it.
 */
@Composable
private fun Modifier.folderDropTarget(dragState: BlockTypeDragState, key: BlockTypeDragState.FolderKey): Modifier {
    DisposableEffect(dragState, key) {
        onDispose { dragState.folderTargets.remove(key) }
    }
    return onGloballyPositioned { dragState.folderTargets[key] = it.boundsInRoot() }
        .then(if (dragState.hoveredFolder == key) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
}
