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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.ui.actorTypes.icon
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_group
import kubriko.creator.generated.resources.action_expand_group
import kubriko.creator.generated.resources.choice_none_found
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import kubriko.creator.generated.resources.resource_group_empty
import kubriko.creator.generated.resources.sidebar_filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The project's contents: its Actor types, Manager types, and Scenes as trees of folders, then its other files grouped
 * by what they are, below a field that narrows them all to those whose names contain what is typed (opening the folders
 * that hold them; Escape clears it). Clicking a type edits it, and clicking a Scene opens it; clicking the selected one
 * again, or empty space, selects nothing.
 *
 * @param folderGroups The groups with folders it shows (the type groups only for projects that opted in to them).
 * @param dragState Where types and Scenes are dragged to: folders, and the open Scene.
 */
@Composable
internal fun ProjectSidebar(
    modifier: Modifier,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    folderActions: FolderTreeActions,
    dragState: SidebarDragState,
    flashedName: String? = null,
    flashKey: Any? = null,
) = Column(
    modifier = modifier,
) {
    var query by remember { mutableStateOf("") }
    var naming by remember { mutableStateOf<SidebarNaming?>(null) }
    var collapsedFolders by remember { mutableStateOf(emptyMap<FolderGroup, Set<String>>()) }
    var collapsedGroups by remember { mutableStateOf(setOf(ResourceGroup.KOTLIN)) }
    var collapsedKotlinFolders by remember { mutableStateOf(emptySet<String>()) }
    val isFiltering = query.isNotBlank()
    val shownFolderGroups = remember(query, folderGroups) {
        if (isFiltering) folderGroups.mapValues { (_, state) -> state.filtered(query) }.filterValues { it.items.isNotEmpty() } else folderGroups
    }
    val shownResources = remember(query, resources) {
        if (isFiltering) resources.mapValues { (_, groupResources) -> groupResources.filter { matchesSidebarFilter(it.name, query) } } else resources
    }
    val kotlinRootPackage = remember(resources) { rootPackageOf(resources[ResourceGroup.KOTLIN].orEmpty()) }
    val kotlinRows = remember(shownResources, kotlinRootPackage, collapsedKotlinFolders, isFiltering) {
        kotlinTreeRows(shownResources[ResourceGroup.KOTLIN].orEmpty(), kotlinRootPackage, if (isFiltering) emptySet() else collapsedKotlinFolders)
    }
    val resourceGroups = shownResourceGroups(shownResources).filter { !isFiltering || shownResources[it].orEmpty().isNotEmpty() }
    SidebarFilterField(
        query = query,
        onQueryChange = { query = it },
    )
    LazyColumn(
        modifier = Modifier.weight(1f).pointerInput(folderActions) { detectTapGestures { folderActions.onSelectionCleared() } },
    ) {
        FolderGroup.entries.forEach { group ->
            val state = shownFolderGroups[group] ?: return@forEach
            folderTreeSection(
                group = group,
                state = state,
                actions = folderActions,
                dragState = dragState,
                collapsed = if (isFiltering) emptySet() else collapsedFolders[group].orEmpty(),
                onCollapsedChange = { if (!isFiltering) collapsedFolders = collapsedFolders + (group to it) },
                naming = naming,
                onNamingChange = { naming = it },
                flashedName = flashedName,
                flashKey = flashKey,
            )
        }
        resourceGroups.forEach { group ->
            val isCollapsed = !isFiltering && group in collapsedGroups
            val groupResources = shownResources[group].orEmpty()
            resourceGroupSection(
                group = group,
                isEmpty = groupResources.isEmpty(),
                isCollapsed = isCollapsed,
                onToggle = { if (!isFiltering) collapsedGroups = if (isCollapsed) collapsedGroups - group else collapsedGroups + group },
            ) {
                if (group == ResourceGroup.KOTLIN) {
                    kotlinTreeItems(
                        rows = kotlinRows,
                        onToggle = { key -> if (!isFiltering) collapsedKotlinFolders = if (key in collapsedKotlinFolders) collapsedKotlinFolders - key else collapsedKotlinFolders + key },
                    )
                } else {
                    resourceItems(group, groupResources)
                }
            }
        }
        if (isFiltering && shownFolderGroups.isEmpty() && resourceGroups.isEmpty()) {
            item(key = "filter-none") {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = stringResource(Res.string.choice_none_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SidebarFilterField(
    query: String,
    onQueryChange: (String) -> Unit,
) = CompactTextField(
    modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && query.isNotEmpty()) {
                onQueryChange("")
                true
            } else {
                false
            }
        },
    value = query,
    onValueChange = onQueryChange,
    placeholder = stringResource(Res.string.sidebar_filter),
)

/**
 * The header of a group of files that aren't types or Scenes, then, unless it [isCollapsed], its [items], or a line
 * saying it [isEmpty].
 */
private fun LazyListScope.resourceGroupSection(
    group: ResourceGroup,
    isEmpty: Boolean,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    items: LazyListScope.() -> Unit,
) {
    item(key = group) {
        PanelHeader(
            modifier = Modifier.clickable(onClick = onToggle),
            icon = group.icon,
            title = stringResource(group.title),
            accent = Accent.CODE,
        ) {
            Icon(
                modifier = Modifier.padding(end = 8.dp).size(16.dp).rotate(if (isCollapsed) -90f else 0f),
                painter = painterResource(Res.drawable.ic_chevron),
                contentDescription = stringResource(if (isCollapsed) Res.string.action_expand_group else Res.string.action_collapse_group),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (isCollapsed) return
    if (isEmpty) {
        item(key = "$group-empty") {
            Text(
                modifier = Modifier.padding(start = 36.dp, top = 2.dp, bottom = 6.dp),
                text = stringResource(Res.string.resource_group_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        items()
    }
}

/**
 * The files of an asset [group], by name.
 */
private fun LazyListScope.resourceItems(group: ResourceGroup, groupResources: List<ProjectResource>) {
    val duplicateNames = groupResources.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    items(groupResources, key = { it.relativePath }) { resource ->
        SidebarItem(
            icon = group.icon,
            accent = Accent.CODE,
            text = resource.name,
            detail = if (resource.name in duplicateNames) ResourceScanner.sourceSetOf(resource) else null,
            onClick = null,
        )
    }
}

private val ResourceGroup.icon
    get() = when (this) {
        ResourceGroup.SCENES -> Res.drawable.ic_scene
        ResourceGroup.SPRITES -> Res.drawable.ic_sprite
        ResourceGroup.SOUNDS -> Res.drawable.ic_sound
        ResourceGroup.MUSIC -> Res.drawable.ic_music
        ResourceGroup.KOTLIN -> Res.drawable.ic_code
    }

/**
 * The resource groups the sidebar lists below the groups with folders (so not Scenes): Kotlin files always, and the
 * kinds of asset nothing can use yet only when the project has some.
 */
internal fun shownResourceGroups(resources: Map<ResourceGroup, List<ProjectResource>>) = ResourceGroup.entries.filter { group ->
    group != ResourceGroup.SCENES && (group == ResourceGroup.KOTLIN || resources[group].orEmpty().isNotEmpty())
}
