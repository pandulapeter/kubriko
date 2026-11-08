/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddPageImage
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditPage
import com.pandulapeter.kubrikoCreator.screenplay.DocumentStore
import com.pandulapeter.kubrikoCreator.screenplay.MovedFiles
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [rememberOpenMarkdownText] for the project of [context], following moves and closing what is gone through
 * [selectionController].
 */
@Composable
internal fun rememberOpenMarkdownText(context: CommandContext, selectionController: SelectionController, selection: WorkspaceSelection): String? = rememberOpenMarkdownText(
    screenplay = context.screenplay,
    documents = context.documents,
    movedFiles = context.movedFiles,
    selection = selection,
    onMoved = selectionController::open,
    onGone = selectionController::clear,
)

/**
 * The text of the page or document [selection] shows, read off the main thread again after every change to them, or null
 * until it is read, if it can't be (another program holds it), or if [selection] shows neither. One that Kubriko Creator
 * moved ([MovedFiles]) calls [onMoved] with where it is now, and one that is gone otherwise (deleted, or moved outside
 * Kubriko Creator) calls [onGone].
 */
@Composable
internal fun rememberOpenMarkdownText(
    screenplay: ScreenplayStore,
    documents: DocumentStore,
    movedFiles: MovedFiles,
    selection: WorkspaceSelection,
    onMoved: (WorkspaceSelection) -> Unit,
    onGone: () -> Unit,
): String? {
    val currentOnMoved by rememberUpdatedState(onMoved)
    val currentOnGone by rememberUpdatedState(onGone)
    val pageRevision by screenplay.revision.collectAsState()
    val documentRevision by documents.revision.collectAsState()
    val pageNames by screenplay.current.collectAsState()
    LaunchedEffect(selection, pageNames, documentRevision) {
        val path = markdownPathOf(selection) ?: return@LaunchedEffect
        val isKnown = when (selection) {
            is WorkspaceSelection.Page -> selection.name in pageNames
            else -> documents.isKnown(path)
        }
        if (isKnown) return@LaunchedEffect
        val root = screenplay.rootDirectory
        // A move is looked at first: after a rename that changes only the case, the old name still finds the file on
        // macOS and Windows.
        val (moved, isOnDisk) = withContext(Dispatchers.IO) {
            movedFiles.whereIs(path)?.takeIf { File(root, it).isFile } to File(root, path).isFile
        }
        when {
            moved != null -> currentOnMoved(markdownSelectionOf(moved))
            !isOnDisk -> currentOnGone()
        }
    }
    val revision = when (selection) {
        is WorkspaceSelection.Page -> pageRevision
        is WorkspaceSelection.Document -> documentRevision
        else -> 0
    }
    val read = produceState<Pair<WorkspaceSelection, String?>?>(null, selection, revision) {
        val text = withContext(Dispatchers.IO) {
            runCatching {
                when (selection) {
                    is WorkspaceSelection.Page -> screenplay.read(selection.name)
                    is WorkspaceSelection.Document -> documents.read(selection.path)
                    else -> null
                }
            }.getOrNull()
        }
        value = selection to text
    }.value
    // A text read for what was shown before is never handed to what is shown now.
    return read?.takeIf { it.first == selection }?.second
}

/**
 * The path from the project's root of the page or document [selection] shows, or null if it shows neither.
 */
internal fun markdownPathOf(selection: WorkspaceSelection): String? = when (selection) {
    is WorkspaceSelection.Page -> PageNaming.pathOf(selection.name)
    is WorkspaceSelection.Document -> selection.path
    else -> null
}

/**
 * What shows the page or document at [path], a path from the project's root.
 */
internal fun markdownSelectionOf(path: String): WorkspaceSelection {
    val page = PageNaming.nameOf(path)
    return if (page != null) WorkspaceSelection.Page(page) else WorkspaceSelection.Document(path)
}

/**
 * The name the page editor shows for [selection]: a page's name, or a document's path.
 */
internal fun markdownTitleOf(selection: WorkspaceSelection): String? = when (selection) {
    is WorkspaceSelection.Page -> selection.name
    is WorkspaceSelection.Document -> selection.path
    else -> null
}

/**
 * What saves [text] into the page or document [selection] shows, or null if it shows neither.
 */
internal fun markdownEditOf(selection: WorkspaceSelection, text: String): Invocation<*>? = when (selection) {
    is WorkspaceSelection.Page -> Invocation(EditPage, EditPage.Input(selection.name, text))
    is WorkspaceSelection.Document -> Invocation(EditDocument, EditDocument.Input(selection.path, text))
    else -> null
}

/**
 * What puts an image on the page [selection] shows through [dispatch], or null if it shows no page: documents take no
 * images, since Kubriko Creator doesn't keep files next to them.
 */
internal fun pageImageAdderOf(selection: WorkspaceSelection, dispatch: (Invocation<*>) -> Unit): ((PageImageSource, String?) -> Unit)? {
    val page = (selection as? WorkspaceSelection.Page)?.name ?: return null
    return { source, section ->
        dispatch(
            Invocation(
                AddPageImage,
                when (source) {
                    is PageImageSource.ImageFile -> AddPageImage.Input(page, source.description, file = source.file.path, section = section)
                    is PageImageSource.Sprite -> AddPageImage.Input(page, source.path.substringAfterLast('/').substringBeforeLast('.'), sprite = source.path, section = section)
                },
            ),
        )
    }
}
