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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.NewNameField
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.RowActionsButton
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_music
import kubriko.creator.generated.resources.action_add_sound
import kubriko.creator.generated.resources.action_add_sprite
import kubriko.creator.generated.resources.action_delete_resource
import kubriko.creator.generated.resources.action_more_options
import kubriko.creator.generated.resources.action_new_folder
import kubriko.creator.generated.resources.action_paste
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_create_new_folder
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_paste
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import kubriko.creator.generated.resources.new_resource_hint
import org.jetbrains.compose.resources.stringResource

/**
 * The header of a group of [module]'s files that aren't types or Scenes, then, while it is open in [tree], its files: the
 * Kotlin files, sounds, and music as trees ([audioTreeItems]), sprites by name. A group with none is only its header,
 * faded, which says where such files go without a line of its own for each. In the module new resources go to (the one
 * making Scenes), the sprites', sounds', and music's header offers adding and pasting more (and the sounds' and music's
 * a new folder, and taking a file dropped on it out of its folder), and each of their files Rename (typed in place) and
 * Delete, in its ⋮ and right-click menu; another module's are another `Res`, which the game's commands never change.
 *
 * @param rows The trees' rows ([SidebarRows]), worked out outside the list's builder.
 * @param folderActions What the sounds' and music's folders and files do.
 * @param naming What is being named in the sidebar, if anything.
 */
internal fun LazyListScope.resourceGroupSection(
    module: SidebarModule,
    group: ResourceGroup,
    tree: SidebarTree,
    rows: SidebarRows,
    actions: ResourceActions = ResourceActions(),
    folderActions: FolderTreeActions? = null,
    dragState: SidebarDragState = SidebarDragState(),
    naming: SidebarNaming? = null,
    onNamingChange: (SidebarNaming?) -> Unit = {},
) {
    val groupKey = SidebarKeys.resourceGroup(module.path, group)
    val isExpanded = tree.isExpanded(groupKey)
    val groupResources = module.resources[group].orEmpty()
    val kind = group.resourceKind?.takeIf { FolderGroup.SCENES in module.creatingGroups }
    val audioTree = module.audioTrees[group]?.takeIf { folderActions != null }
    val folderGroup = group.audioFolderGroup?.takeIf { audioTree != null }
    val isEmpty = groupResources.isEmpty() && audioTree?.hasShownFolders != true
    item(key = groupKey) {
        ResourceGroupHeader(
            modifier = sidebarItemMotion().then(
                if (kind != null && folderGroup != null) Modifier.folderDropTarget(dragState, SidebarDragState.FolderKey(folderGroup, audioTree?.root.orEmpty(), module.path)) else Modifier,
            ),
            group = group,
            indent = module.depth,
            isExpanded = isExpanded,
            isEmpty = isEmpty,
            onToggle = { tree.toggle(groupKey) },
            subfolderActions = tree.subfolderActions(groupKey),
            actions = kind?.let {
                resourceGroupActions(it, actions) + listOfNotNull(
                    folderGroup?.let { folders ->
                        ContextAction(stringResource(Res.string.action_new_folder), Res.drawable.ic_create_new_folder) {
                            tree.expand(groupKey)
                            onNamingChange(SidebarNaming.NewFolder(folders, audioTree?.root.orEmpty()))
                        }
                    },
                )
            }.orEmpty(),
        )
    }
    when {
        !isExpanded || (isEmpty && naming !is SidebarNaming.NewFolder) -> Unit
        audioTree != null && folderActions != null -> audioTreeItems(
            module = module,
            group = group,
            indent = module.depth,
            isChangeable = kind != null,
            actions = folderActions,
            dragState = dragState,
            tree = tree,
            rows = rows,
            naming = naming,
            onNamingChange = onNamingChange,
        )
        group == ResourceGroup.KOTLIN -> kotlinTreeItems(
            groupKey = groupKey,
            rows = rows.kotlinOf(module, tree),
            indent = module.depth,
            tree = tree,
        )
        else -> resourceItems(group, groupResources, indent = module.depth, actions = actions.takeIf { kind != null }, naming = naming, onNamingChange = onNamingChange)
    }
}

/**
 * The header of a group of files, opened and closed by a click, with [subfolderActions] before its chevron while it
 * holds folders, and its content [indent] levels in, as the module it belongs to is. While it [isEmpty], it is faded and
 * opens nothing.
 */
@Composable
private fun ResourceGroupHeader(
    group: ResourceGroup,
    indent: Int,
    isExpanded: Boolean,
    isEmpty: Boolean,
    onToggle: () -> Unit,
    subfolderActions: SubfolderActions?,
    actions: List<ContextAction>,
    modifier: Modifier = Modifier,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    PanelHeader(
        modifier = modifier
            .then(if (isEmpty) Modifier.alpha(EMPTY_GROUP_ALPHA) else Modifier.clickable(onClick = onToggle))
            .hoverReveal(hover)
            .then(if (actions.isEmpty()) Modifier else Modifier.rightClickMenu(menu))
            .padding(start = SIDEBAR_INDENT * indent),
        icon = group.icon,
        title = stringResource(group.title),
        accent = Accent.CODE,
    ) {
        if (subfolderActions != null) {
            ExpandCollapseButtons(
                isShown = hover.isActive,
                actions = subfolderActions,
            )
        }
        if (!isEmpty) {
            SidebarChevron(
                modifier = Modifier.padding(end = if (actions.isEmpty()) 8.dp else 0.dp).size(16.dp),
                isExpanded = isExpanded,
            )
        }
        if (actions.isNotEmpty()) {
            RowActionsButton(
                modifier = Modifier.revealedWhen(hover.isActive || menu.isOpen),
                menu = menu,
                actions = actions,
                label = Res.string.action_more_options,
            )
        }
    }
}

/**
 * What the header of the group of [kind]'s files offers: choosing more of them, and pasting them.
 */
@Composable
private fun resourceGroupActions(kind: ResourceKind, actions: ResourceActions) = listOf(
    ContextAction(stringResource(kind.addLabel), Res.drawable.ic_add) { actions.onChoose(kind) },
    ContextAction(stringResource(Res.string.action_paste), Res.drawable.ic_paste, onClick = actions.onPaste),
)

internal val ResourceKind.icon
    get() = when (this) {
        ResourceKind.SPRITE -> Res.drawable.ic_sprite
        ResourceKind.SOUND -> Res.drawable.ic_sound
        ResourceKind.MUSIC -> Res.drawable.ic_music
    }

/**
 * The label of the action choosing files of this kind to add.
 */
internal val ResourceKind.addLabel
    get() = when (this) {
        ResourceKind.SPRITE -> Res.string.action_add_sprite
        ResourceKind.SOUND -> Res.string.action_add_sound
        ResourceKind.MUSIC -> Res.string.action_add_music
    }

/**
 * The files of an asset [group], by name, [indent] levels in, each a sprite, sound, or music file offering Rename and
 * Delete through [actions] (none for a module whose files the commands don't change), or the field renaming it.
 */
private fun LazyListScope.resourceItems(
    group: ResourceGroup,
    groupResources: List<ProjectResource>,
    indent: Int,
    actions: ResourceActions?,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
) {
    val duplicateNames = groupResources.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    items(groupResources, key = { it.relativePath }) { resource ->
        val path = resourcePathOf(resource)
        if (actions != null && path != null && naming == SidebarNaming.RenamedResource(resource.relativePath)) {
            Box(sidebarItemMotion()) {
                NewNameField(
                    placeholder = Res.string.new_resource_hint,
                    initialName = ResourceKind.nameOf(path),
                    indent = SIDEBAR_ROW_START + SIDEBAR_INDENT * indent,
                    confirmLabel = Res.string.action_rename,
                    icon = group.icon,
                    iconTint = Accent.CODE.color,
                    onCreate = { name -> actions.onRename(path, name).also { if (it == null) onNamingChange(null) }?.toFieldError() },
                    onCancel = { onNamingChange(null) },
                )
            }
        } else {
            SidebarItem(
                modifier = sidebarItemMotion(),
                icon = group.icon,
                accent = Accent.CODE,
                text = resource.name,
                detail = if (resource.name in duplicateNames) ResourceScanner.sourceSetOf(resource) else null,
                depth = indent,
                onClick = null,
                actions = if (actions == null || path == null) {
                    emptyList()
                } else {
                    listOf(
                        ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit) { onNamingChange(SidebarNaming.RenamedResource(resource.relativePath)) },
                        ContextAction(stringResource(Res.string.action_delete_resource), Res.drawable.ic_delete, isDestructive = true) { actions.onDelete(path) },
                    )
                },
            )
        }
    }
}

private val ResourceGroup.icon
    get() = when (this) {
        ResourceGroup.SCENES -> Res.drawable.ic_scene
        ResourceGroup.DOCUMENTS -> Res.drawable.ic_file
        ResourceGroup.SPRITES -> Res.drawable.ic_sprite
        ResourceGroup.SOUNDS -> Res.drawable.ic_sound
        ResourceGroup.MUSIC -> Res.drawable.ic_music
        ResourceGroup.KOTLIN -> Res.drawable.ic_code
    }

/**
 * The resource groups the sidebar lists below the groups with folders (so not Scenes, nor documents, which are listed by
 * their folders): Kotlin files always, and the kinds of asset nothing can use yet only when the project has some.
 */
internal fun shownResourceGroups(resources: Map<ResourceGroup, List<ProjectResource>>) = ResourceGroup.entries.filter { group ->
    group != ResourceGroup.SCENES && group != ResourceGroup.DOCUMENTS && (group == ResourceGroup.KOTLIN || resources[group].orEmpty().isNotEmpty())
}

/**
 * How strongly the header of a group with nothing in it shows, faded enough to read as empty and still be read.
 */
internal const val EMPTY_GROUP_ALPHA = 0.55f
