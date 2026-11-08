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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The links of [text], the page or document [selection] shows, read off the main thread again whenever the text or
 * anything that may add or remove what they point at changes, with everything it can mention; null if [selection] shows
 * neither.
 *
 * @param documentsRevision Raised when a document is created, moved, or deleted.
 */
@Composable
internal fun rememberPageLinks(
    rootDirectory: File,
    selection: WorkspaceSelection,
    text: String?,
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    pageNames: List<String>,
    documentsRevision: Int,
    onOpen: (Mention) -> Unit,
): PageLinksState? {
    val pagePath = markdownPathOf(selection) ?: return null
    val currentOnOpen by rememberUpdatedState(onOpen)
    val state = remember(selection) { PageLinksState(pagePath) { currentOnOpen(it) } }
    state.candidates = remember(state, blockTypes, resources, pageNames) { mentionCandidatesOf(blockTypes, resources, pageNames, pagePath) }
    LaunchedEffect(state, text, blockTypes, resources, pageNames, documentsRevision) {
        state.looks = if (text == null) emptyMap() else withContext(Dispatchers.IO) { runCatching { mentionLooksOf(rootDirectory, pagePath, text) }.getOrDefault(emptyMap()) }
    }
    return state
}

/**
 * The path from the project's root of the page or document [selection] shows, or null if it shows neither.
 */
internal fun markdownPathOf(selection: WorkspaceSelection): String? = when (selection) {
    is WorkspaceSelection.Page -> PageNaming.pathOf(selection.name)
    is WorkspaceSelection.Document -> selection.path
    else -> null
}
