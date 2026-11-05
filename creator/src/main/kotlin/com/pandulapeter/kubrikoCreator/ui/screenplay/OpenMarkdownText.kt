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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditPage
import com.pandulapeter.kubrikoCreator.screenplay.DocumentStore
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStore
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The text of the page or document [selection] shows, read off the main thread again after every change to them, or null
 * until it is read, if it can't be (another program holds it), or if [selection] shows neither. One that is gone (deleted,
 * or moved away) calls [onGone].
 */
@Composable
internal fun rememberOpenMarkdownText(
    screenplay: ScreenplayStore,
    documents: DocumentStore,
    selection: WorkspaceSelection,
    onGone: () -> Unit,
): String? {
    val currentOnGone by rememberUpdatedState(onGone)
    val pageRevision by screenplay.revision.collectAsState()
    val documentRevision by documents.revision.collectAsState()
    val pageNames by screenplay.current.collectAsState()
    LaunchedEffect(selection, pageNames, documentRevision) {
        val isGone = when (selection) {
            is WorkspaceSelection.Page -> selection.name !in pageNames
            is WorkspaceSelection.Document -> !documents.isKnown(selection.path)
            else -> false
        }
        if (isGone) currentOnGone()
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
