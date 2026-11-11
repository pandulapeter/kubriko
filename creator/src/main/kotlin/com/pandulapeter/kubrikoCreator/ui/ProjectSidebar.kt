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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import com.pandulapeter.kubrikoCreator.workspace.ProjectLayout
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_new
import kubriko.creator.generated.resources.action_new_document
import kubriko.creator.generated.resources.choice_none_found
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.sidebar_filter
import org.jetbrains.compose.resources.stringResource

/**
 * The project's contents: its Actor types, Manager types, and Scenes as trees of folders, then its other files grouped
 * by what they are, and its documents by the folders they are in, below a field that narrows them all to those whose names contain what is typed (opening the folders
 * that hold them; Escape clears it). In a project of several Gradle modules, each module's are under a band of its own,
 * nested as the modules are ([sidebarModules]). Clicking a type edits it, and clicking a Scene opens it; clicking the selected one again, or empty
 * space, selects nothing. Opening a closed folder, group, or module shows the folders in it closed, and the buttons a
 * row holding folders shows while it is pointed at open or close every one of them.
 *
 * @param folderGroups The groups with folders it shows (the type groups only for projects that opted in to them).
 * @param layout Where the types and new Scenes are kept, which decides the modules they are shown in.
 * @param gameModule The folder of the module chosen as the game's in the toolbar, whose branch opens first in a project
 *   that isn't opted in.
 * @param dragState Where types, Scenes, pages, and documents are dragged to: folders (bands too, for documents), and
 *   the open Scene.
 * @param documentActions What the documents listed in each band do ([documentRows]); a band offers New document too.
 * @param resourceActions What the sprites, sounds, and music do ([resourceGroupSection]); files of theirs dropped onto
 *   the sidebar are added.
 * @param selectedDocument The path of the document shown in the center, if any.
 */
@Composable
internal fun ProjectSidebar(
    modifier: Modifier,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    folderGroups: Map<FolderGroup, FolderTreeGroupState>,
    folderActions: FolderTreeActions,
    dragState: SidebarDragState,
    documentActions: DocumentActions,
    resourceActions: ResourceActions = ResourceActions(),
    selectedDocument: String? = null,
    layout: ProjectLayout = ProjectLayout(),
    gameModule: String? = null,
    flashedName: String? = null,
    flashKey: Any? = null,
) = Column(
    modifier = modifier,
) {
    var query by remember { mutableStateOf("") }
    var naming by remember { mutableStateOf<SidebarNaming?>(null) }
    val expansion = remember { SidebarExpansion() }
    val isFiltering = query.isNotBlank()
    val modules = remember(resources, folderGroups, layout, gameModule) { sidebarModules(resources, folderGroups, layout, gameModule) }
    val isGrouped = modules.size > 1
    val structure = remember(modules) { modules.map { it.withoutSelection() } }
    val outline = remember(structure) { SidebarOutline(structure.flatMap { it.outlineNodes(isGrouped) }) }
    LaunchedEffect(outline) { expansion.forgetAllBut(outline) }
    val shownModules = remember(query, modules) { if (isFiltering) filteredModules(modules, query) else modules }
    val tree = remember(outline, expansion, isFiltering) { SidebarTree(outline, expansion, isFiltering) }
    val listState = rememberLazyListState()
    val previousRows = remember { arrayOfNulls<SidebarRows>(1) }
    val rows by remember(shownModules, isGrouped, tree) {
        derivedStateOf { SidebarRows.of(shownModules, isGrouped, tree, previousRows[0]).also { previousRows[0] = it } }
    }
    val currentFlash by rememberUpdatedState(SidebarFlash(flashedName, flashKey))
    val flash = remember { { currentFlash } }
    var revealedKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revealedKey) {
        revealedKey?.let { listState.revealItem(it) }
        revealedKey = null
    }
    SidebarTopRow(
        query = query,
        onQueryChange = { query = it },
        newItems = remember(modules) { sidebarNewItems(modules) },
        onNew = { item ->
            if (item is SidebarNewItem.Resource) resourceActions.onChoose(item.kind)
            item.target(modules)?.let { target ->
                query = ""
                target.openedKeys.forEach(tree::expand)
                naming = target.naming
                revealedKey = target.revealKey
            }
        },
    )
    LazyColumn(
        modifier = Modifier.weight(1f).pointerInput(folderActions) { detectTapGestures { folderActions.onSelectionCleared() } }.fileDropTarget(::resourceFilesAmong, resourceActions.onAdd),
        state = listState,
    ) {
        // A first row that is always there, so the list keeps its top in place, not whatever row came first: module bands
        // that appear once the project is read are inserted above that row, and would otherwise be scrolled out of view.
        item(key = "sidebar-top") { Spacer(Modifier.height(1.dp)) }
        newProjectDocumentRow(naming, documentActions, onNamingChange = { naming = it })
        val openModules = HashSet<String>()
        shownModules.forEach { module ->
            if (module.parent != null && module.parent !in openModules) return@forEach
            val moduleKey = SidebarKeys.module(module.path)
            if (isGrouped) {
                item(key = moduleKey) {
                    SidebarModuleHeader(
                        modifier = sidebarItemMotion().folderDropTarget(dragState, SidebarDragState.FolderKey(group = null, path = module.path)),
                        path = module.path,
                        depth = module.depth,
                        isExpanded = tree.isExpanded(moduleKey),
                        onToggle = { tree.toggle(moduleKey) },
                        subfolderActions = tree.subfolderActions(moduleKey),
                        actions = listOf(
                            newDocumentAction {
                                tree.expand(moduleKey)
                                naming = SidebarNaming.NewDocument(module.path, "")
                            },
                        ),
                    )
                }
            }
            if (!isGrouped || tree.isExpanded(moduleKey)) {
                openModules += module.path
                moduleSections(
                    module = module,
                    folderActions = folderActions,
                    resourceActions = resourceActions,
                    dragState = dragState,
                    tree = tree,
                    naming = naming,
                    onNamingChange = { naming = it },
                    rows = rows,
                    flash = flash,
                )
                documentRows(
                    module = module,
                    actions = documentActions,
                    dragState = dragState,
                    tree = tree,
                    naming = naming,
                    onNamingChange = { naming = it },
                    selectedDocument = selectedDocument,
                    onSelectionCleared = folderActions.onSelectionCleared,
                    rows = rows,
                    flash = flash,
                )
            }
        }
        if (isFiltering && shownModules.isEmpty()) noneFoundRow()
    }
}

/**
 * What the sidebar shows when nothing matches the filter.
 */
private fun LazyListScope.noneFoundRow() = item(key = "filter-none") {
    Text(
        modifier = sidebarItemMotion(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)),
        text = stringResource(Res.string.choice_none_found),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The groups of one [module]: those with folders, then its other files ([documentRows] follow them).
 */
private fun LazyListScope.moduleSections(
    module: SidebarModule,
    folderActions: FolderTreeActions,
    resourceActions: ResourceActions,
    dragState: SidebarDragState,
    tree: SidebarTree,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    rows: SidebarRows,
    flash: () -> SidebarFlash,
) {
    FolderGroup.entries.forEach { group ->
        folderTreeSection(
            module = module,
            group = group,
            actions = folderActions,
            dragState = dragState,
            tree = tree,
            naming = naming,
            onNamingChange = onNamingChange,
            rows = rows,
            flash = flash,
        )
    }
    module.resourceGroups.forEach { group ->
        resourceGroupSection(
            module = module,
            group = group,
            tree = tree,
            rows = rows,
            actions = resourceActions,
            folderActions = folderActions,
            dragState = dragState,
            naming = naming,
            onNamingChange = onNamingChange,
        )
    }
}

/**
 * The filter field, and the New menu after it, offering [newItems].
 */
@Composable
private fun SidebarTopRow(
    query: String,
    onQueryChange: (String) -> Unit,
    newItems: List<SidebarNewItem>,
    onNew: (SidebarNewItem) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    SidebarFilterField(
        modifier = Modifier.weight(1f),
        query = query,
        onQueryChange = onQueryChange,
    )
    MenuIconButton(
        icon = Res.drawable.ic_add,
        label = Res.string.action_new,
        options = newItems,
        optionText = { item ->
            when (item) {
                is SidebarNewItem.GroupItem -> stringResource(item.group.newItemLabel)
                is SidebarNewItem.Resource -> stringResource(item.kind.addLabel)
                SidebarNewItem.Document -> stringResource(Res.string.action_new_document)
            }
        },
        optionIcon = { item ->
            when (item) {
                is SidebarNewItem.GroupItem -> item.group.icon
                is SidebarNewItem.Resource -> item.kind.icon
                SidebarNewItem.Document -> Res.drawable.ic_file
            }
        },
        onSelected = onNew,
    )
}

@Composable
private fun SidebarFilterField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) = CompactTextField(
    modifier = modifier
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
