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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import org.jetbrains.compose.resources.stringResource

/**
 * The header of a group of [module]'s files that aren't types or Scenes, then, while it is open in [tree], its files: the
 * Kotlin files as a tree, the rest by name. A group with none is only its header, faded, which says where such files go
 * without a line of its own for each.
 */
internal fun LazyListScope.resourceGroupSection(
    module: SidebarModule,
    group: ResourceGroup,
    tree: SidebarTree,
) {
    val groupKey = SidebarKeys.resourceGroup(module.path, group)
    val isExpanded = tree.isExpanded(groupKey)
    val groupResources = module.resources[group].orEmpty()
    item(key = groupKey) {
        ResourceGroupHeader(
            modifier = sidebarItemMotion(),
            group = group,
            indent = module.depth,
            isExpanded = isExpanded,
            isEmpty = groupResources.isEmpty(),
            onToggle = { tree.toggle(groupKey) },
            subfolderActions = tree.subfolderActions(groupKey),
        )
    }
    when {
        !isExpanded || groupResources.isEmpty() -> Unit
        group == ResourceGroup.KOTLIN -> kotlinTreeItems(
            groupKey = groupKey,
            rows = kotlinTreeRows(groupResources, module.kotlinRootPackage, tree.toggledKotlinRows(groupKey), isEverythingOpen = tree.isFiltering),
            indent = module.depth,
            tree = tree,
        )
        else -> resourceItems(group, groupResources, indent = module.depth)
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
    modifier: Modifier = Modifier,
) {
    val hover = rememberHoverRevealState()
    PanelHeader(
        modifier = modifier
            .then(if (isEmpty) Modifier.alpha(EMPTY_GROUP_ALPHA) else Modifier.clickable(onClick = onToggle).hoverReveal(hover))
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
                modifier = Modifier.padding(end = 8.dp).size(16.dp),
                isExpanded = isExpanded,
            )
        }
    }
}

/**
 * The files of an asset [group], by name, [indent] levels in.
 */
private fun LazyListScope.resourceItems(group: ResourceGroup, groupResources: List<ProjectResource>, indent: Int) {
    val duplicateNames = groupResources.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    items(groupResources, key = { it.relativePath }) { resource ->
        SidebarItem(
            modifier = sidebarItemMotion(),
            icon = group.icon,
            accent = Accent.CODE,
            text = resource.name,
            detail = if (resource.name in duplicateNames) ResourceScanner.sourceSetOf(resource) else null,
            depth = indent,
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

/**
 * How strongly the header of a group with nothing in it shows, faded enough to read as empty and still be read.
 */
internal const val EMPTY_GROUP_ALPHA = 0.55f
