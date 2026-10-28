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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.ui.actorTypes.icon
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_group
import kubriko.creator.generated.resources.action_expand_group
import kubriko.creator.generated.resources.action_new_actor_type
import kubriko.creator.generated.resources.action_new_manager_type
import kubriko.creator.generated.resources.action_new_scene
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import kubriko.creator.generated.resources.new_actor_type_hint
import kubriko.creator.generated.resources.new_manager_type_hint
import kubriko.creator.generated.resources.new_scene_hint
import kubriko.creator.generated.resources.resource_group_actor_types
import kubriko.creator.generated.resources.resource_group_empty
import kubriko.creator.generated.resources.resource_group_manager_types
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The project's contents: its Actor types and Manager types, then its files grouped by what they are. Clicking a Scene
 * edits it.
 *
 * @param blockTypes The project's types, or null if it hasn't opted in to them.
 * @param dragState Where types are dragged to: folders, and the open Scene.
 */
@Composable
internal fun ProjectSidebar(
    modifier: Modifier,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    blockTypes: BlockTypeTreeState?,
    blockTypeActions: BlockTypeTreeActions,
    dragState: BlockTypeDragState,
    onSceneSelected: (ProjectResource) -> Unit,
    onCreateScene: (String) -> StringResource?,
    flashedName: String? = null,
    flashKey: Any? = null,
) {
    var naming by remember { mutableStateOf<BlockTypeNaming?>(null) }
    var collapsedFolders by remember { mutableStateOf(emptyMap<BlockTypeKind, Set<String>>()) }
    var isCreatingScene by remember { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf(setOf(ResourceGroup.KOTLIN)) }
    LazyColumn(
        modifier = modifier,
    ) {
        if (blockTypes != null) {
            BlockTypeKind.entries.forEach { kind ->
                blockTypeSection(
                    kind = kind,
                    state = blockTypes,
                    actions = blockTypeActions,
                    dragState = dragState,
                    collapsed = collapsedFolders[kind].orEmpty(),
                    onCollapsedChange = { collapsedFolders = collapsedFolders + (kind to it) },
                    naming = naming,
                    onNamingChange = { naming = it },
                    flashedName = flashedName,
                    flashKey = flashKey,
                )
            }
        }
        shownResourceGroups(resources).forEach { group ->
            val isCollapsed = group in collapsedGroups
            item(key = group) {
                PanelHeader(
                    modifier = Modifier.clickable { collapsedGroups = if (isCollapsed) collapsedGroups - group else collapsedGroups + group },
                    icon = group.icon,
                    title = stringResource(group.title),
                ) {
                    if (group == ResourceGroup.SCENES) {
                        IconActionButton(
                            icon = Res.drawable.ic_add,
                            label = Res.string.action_new_scene,
                            onClick = { isCreatingScene = !isCreatingScene },
                        )
                    }
                    Icon(
                        modifier = Modifier.padding(end = 8.dp).size(16.dp).rotate(if (isCollapsed) -90f else 0f),
                        painter = painterResource(Res.drawable.ic_chevron),
                        contentDescription = stringResource(if (isCollapsed) Res.string.action_expand_group else Res.string.action_collapse_group),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isCollapsed) return@forEach
            if (group == ResourceGroup.SCENES && isCreatingScene) {
                item(key = "newScene") {
                    NewNameField(
                        placeholder = Res.string.new_scene_hint,
                        onCreate = { name -> onCreateScene(name).also { if (it == null) isCreatingScene = false } },
                        onCancel = { isCreatingScene = false },
                    )
                }
            }
            val groupResources = resources[group].orEmpty()
            if (groupResources.isEmpty() && !(group == ResourceGroup.SCENES && isCreatingScene)) {
                item(key = "$group-empty") {
                    Text(
                        modifier = Modifier.padding(start = 36.dp, top = 2.dp, bottom = 6.dp),
                        text = stringResource(Res.string.resource_group_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val duplicateNames = groupResources.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
                items(groupResources, key = { it.relativePath }) { resource ->
                    SidebarItem(
                        icon = group.icon,
                        text = if (group == ResourceGroup.SCENES) resource.file.nameWithoutExtension else resource.name,
                        detail = if (resource.name in duplicateNames) ResourceScanner.sourceSetOf(resource) else null,
                        onClick = if (group == ResourceGroup.SCENES) ({ onSceneSelected(resource) }) else null,
                        flashKey = flashKey?.takeIf { group == ResourceGroup.SCENES && resource.file.nameWithoutExtension == flashedName },
                    )
                }
            }
        }
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
 * The resource groups the sidebar lists: Scenes and Kotlin files always, and the kinds of asset nothing can use yet only
 * when the project has some.
 */
internal fun shownResourceGroups(resources: Map<ResourceGroup, List<ProjectResource>>) = ResourceGroup.entries.filter { group ->
    group == ResourceGroup.SCENES || group == ResourceGroup.KOTLIN || resources[group].orEmpty().isNotEmpty()
}
