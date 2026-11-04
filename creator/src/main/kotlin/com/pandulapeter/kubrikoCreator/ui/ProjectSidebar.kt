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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.choice_none_found
import kubriko.creator.generated.resources.sidebar_filter
import org.jetbrains.compose.resources.stringResource

/**
 * The project's contents: its Actor types, Manager types, and Scenes as trees of folders, then its other files grouped
 * by what they are, below a field that narrows them all to those whose names contain what is typed (opening the folders
 * that hold them; Escape clears it). In a project of several Gradle modules, each module's are under a band of its own
 * ([sidebarModules]). Clicking a type edits it, and clicking a Scene opens it; clicking the selected one again, or empty
 * space, selects nothing. Opening a closed folder, group, or module shows the folders in it closed, and the buttons a
 * row holding folders shows while it is pointed at open or close every one of them.
 *
 * @param folderGroups The groups with folders it shows (the type groups only for projects that opted in to them).
 * @param layout Where the types and new Scenes are kept, which decides the modules they are shown in.
 * @param projectName What the root project is called in a project of several modules.
 * @param dragState Where types and Scenes are dragged to: folders, and the open Scene.
 */
@Composable
internal fun ProjectSidebar(
    modifier: Modifier,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    folderActions: FolderTreeActions,
    dragState: SidebarDragState,
    layout: ProjectLayout = ProjectLayout(),
    projectName: String = "",
    flashedName: String? = null,
    flashKey: Any? = null,
) = Column(
    modifier = modifier,
) {
    var query by remember { mutableStateOf("") }
    var naming by remember { mutableStateOf<SidebarNaming?>(null) }
    val expansion = remember { SidebarExpansion() }
    val isFiltering = query.isNotBlank()
    val modules = remember(resources, folderGroups, layout) { sidebarModules(resources, folderGroups, layout) }
    val isGrouped = modules.size > 1
    val structure = remember(modules) { modules.map { it.withoutSelection() } }
    val outline = remember(structure) { SidebarOutline(structure.flatMap { it.outlineNodes(isGrouped) }) }
    LaunchedEffect(outline) { expansion.forgetAllBut(outline) }
    val shownModules = remember(query, modules) { if (isFiltering) modules.mapNotNull { it.filtered(query) } else modules }
    val tree = remember(outline, expansion, isFiltering) { SidebarTree(outline, expansion, isFiltering) }
    SidebarFilterField(
        query = query,
        onQueryChange = { query = it },
    )
    LazyColumn(
        modifier = Modifier.weight(1f).pointerInput(folderActions) { detectTapGestures { folderActions.onSelectionCleared() } },
    ) {
        shownModules.forEach { module ->
            val moduleKey = SidebarKeys.module(module.path)
            if (isGrouped) {
                item(key = moduleKey) {
                    SidebarModuleHeader(
                        modifier = Modifier.animateItem(),
                        title = moduleTitle(module.path, projectName),
                        isExpanded = tree.isExpanded(moduleKey),
                        onToggle = { tree.toggle(moduleKey) },
                        subfolderActions = tree.subfolderActions(moduleKey),
                    )
                }
            }
            if (!isGrouped || tree.isExpanded(moduleKey)) {
                moduleSections(
                    module = module,
                    folderActions = folderActions,
                    dragState = dragState,
                    tree = tree,
                    naming = naming,
                    onNamingChange = { naming = it },
                    flashedName = flashedName,
                    flashKey = flashKey,
                )
            }
        }
        if (isFiltering && shownModules.isEmpty()) {
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

/**
 * The groups of one [module]: those with folders, then its other files.
 */
private fun LazyListScope.moduleSections(
    module: SidebarModule,
    folderActions: FolderTreeActions,
    dragState: SidebarDragState,
    tree: SidebarTree,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    flashedName: String?,
    flashKey: Any?,
) {
    FolderGroup.entries.forEach { group ->
        folderTreeSection(
            module = module.path,
            group = group,
            state = module.folderGroups[group] ?: return@forEach,
            isCreating = group in module.creatingGroups,
            actions = folderActions,
            dragState = dragState,
            tree = tree,
            naming = naming,
            onNamingChange = onNamingChange,
            flashedName = flashedName,
            flashKey = flashKey,
        )
    }
    module.resourceGroups.forEach { group -> resourceGroupSection(module, group, tree) }
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
