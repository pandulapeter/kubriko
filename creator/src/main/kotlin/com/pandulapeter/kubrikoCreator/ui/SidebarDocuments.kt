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
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.NewNameField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.action_add_to_screenplay
import kubriko.creator.generated.resources.action_delete_document
import kubriko.creator.generated.resources.action_new_document
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.new_document_hint
import org.jetbrains.compose.resources.stringResource

/**
 * What the sidebar's documents do. The ones that return a [Rejection] say why a name can't be used, or null.
 */
internal class DocumentActions(
    val onOpen: (path: String) -> Unit,
    val onCreate: (folder: String, name: String) -> Rejection?,
    val onRename: (path: String, newName: String) -> Rejection?,
    val onDelete: (path: String) -> Unit,
    val onAddToScreenplay: (path: String) -> Unit,
)

/**
 * The documents of [module], after its groups, each plain folder of them a folder row, closed at first. Every document
 * and folder row offers New document in its ⋮ and right-click menu, with the name typed in place (folders in it too, as
 * `notes/combat`), and a document also Rename, Add to the screenplay, and Delete.
 *
 * @param selectedDocument The path of the document shown in the center, if any.
 */
internal fun LazyListScope.documentRows(
    module: SidebarModule,
    actions: DocumentActions,
    tree: SidebarTree,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    selectedDocument: String?,
    onSelectionCleared: () -> Unit,
) {
    val documentsKey = SidebarKeys.documents(module.path)
    val indent = module.depth
    if (naming == SidebarNaming.NewDocument(module.path, "")) {
        item(key = "$documentsKey-new") {
            DocumentNameField(
                modifier = sidebarItemMotion(),
                depth = indent,
                onName = { name -> actions.onCreate(module.documentFolderPath(""), name) },
                onNamingChange = onNamingChange,
            )
        }
    }
    if (module.documents.isEmpty()) return
    val items = module.documentItems()
    val collapsed = folderTreeRows(items, emptySet(), collapsed = emptySet())
        .filterIsInstance<FolderTreeRow.Folder>()
        .mapNotNullTo(HashSet()) { folder -> folder.path.takeIf { !tree.isExpanded(SidebarKeys.folder(documentsKey, it)) } }
    items(
        folderTreeRows(items, emptySet(), collapsed),
        key = { row ->
            when (row) {
                is FolderTreeRow.Folder -> "$documentsKey-folder-${row.path}"
                is FolderTreeRow.Item -> "$documentsKey-item-${row.name}"
            }
        },
    ) { row ->
        when (row) {
            is FolderTreeRow.Folder -> DocumentFolder(
                modifier = sidebarItemMotion(),
                module = module,
                row = row,
                tree = tree,
                actions = actions,
                naming = naming,
                onNamingChange = onNamingChange,
            )
            is FolderTreeRow.Item -> DocumentItem(
                modifier = sidebarItemMotion(),
                module = module,
                path = row.name,
                depth = row.depth + indent,
                isSelected = row.name == selectedDocument,
                actions = actions,
                naming = naming,
                onNamingChange = onNamingChange,
                onSelectionCleared = onSelectionCleared,
            )
        }
    }
}

/**
 * A folder of [module]'s documents, followed by the field naming a new document in it while one is.
 */
@Composable
private fun DocumentFolder(
    module: SidebarModule,
    row: FolderTreeRow.Folder,
    tree: SidebarTree,
    actions: DocumentActions,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    val key = SidebarKeys.folder(SidebarKeys.documents(module.path), row.path)
    SidebarFolderRow(
        name = row.name,
        depth = row.depth + module.depth,
        isExpanded = row.isExpanded,
        onToggle = { tree.toggle(key) },
        actions = listOf(
            newDocumentAction {
                tree.expand(key)
                onNamingChange(SidebarNaming.NewDocument(module.path, row.path))
            },
        ),
        subfolderActions = tree.subfolderActions(key),
    )
    if (naming == SidebarNaming.NewDocument(module.path, row.path)) {
        DocumentNameField(
            depth = row.depth + module.depth + 1,
            onName = { name -> actions.onCreate(module.documentFolderPath(row.path), name) },
            onNamingChange = onNamingChange,
        )
    }
}

/**
 * The document at [path], or the field renaming it. Its New document names one in its folder, which in a project of one
 * module, having no band, is how one is started at the top.
 */
@Composable
private fun DocumentItem(
    module: SidebarModule,
    path: String,
    depth: Int,
    isSelected: Boolean,
    actions: DocumentActions,
    naming: SidebarNaming?,
    onNamingChange: (SidebarNaming?) -> Unit,
    onSelectionCleared: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    val fileName = path.substringAfterLast('/')
    if (naming == SidebarNaming.RenamedDocument(path)) {
        DocumentNameField(
            depth = depth,
            initialName = fileName,
            onName = { name -> actions.onRename(path, name) },
            onNamingChange = onNamingChange,
        )
    } else {
        SidebarItem(
            icon = Res.drawable.ic_file,
            accent = Accent.CODE,
            text = fileName,
            depth = depth,
            isSelected = isSelected,
            onClick = { if (isSelected) onSelectionCleared() else actions.onOpen(path) },
            actions = listOf(
                newDocumentAction { onNamingChange(SidebarNaming.NewDocument(module.path, module.documentFolderOf(path))) },
                ContextAction(stringResource(Res.string.action_rename), Res.drawable.ic_edit) { onNamingChange(SidebarNaming.RenamedDocument(path)) },
                ContextAction(stringResource(Res.string.action_add_to_screenplay), Res.drawable.ic_page) { actions.onAddToScreenplay(path) },
                ContextAction(stringResource(Res.string.action_delete_document), Res.drawable.ic_delete, isDestructive = true) { actions.onDelete(path) },
            ),
        )
    }
}

@Composable
internal fun newDocumentAction(onClick: () -> Unit) = ContextAction(stringResource(Res.string.action_new_document), Res.drawable.ic_add, onClick = onClick)

/**
 * A document's name being typed at [depth], looking like its row. [onName] says why a name can't be used, or null once it
 * is used, which ends the naming.
 */
@Composable
private fun DocumentNameField(
    depth: Int,
    onName: (String) -> Rejection?,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
) = Box(modifier) {
    NewNameField(
        placeholder = Res.string.new_document_hint,
        initialName = initialName,
        indent = SIDEBAR_ROW_START + SIDEBAR_INDENT * depth,
        confirmLabel = if (initialName.isEmpty()) Res.string.action_add else Res.string.action_rename,
        icon = Res.drawable.ic_file,
        iconTint = Accent.CODE.color,
        onCreate = { name -> onName(name).also { if (it == null) onNamingChange(null) }?.toFieldError() },
        onCancel = { onNamingChange(null) },
    )
}
