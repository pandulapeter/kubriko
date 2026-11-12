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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.search.SearchPanel
import com.pandulapeter.kubrikoCreator.ui.search.rememberTextSearch
import com.pandulapeter.kubrikoCreator.ui.search.rememberUsages
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * The workspace's Search panel, after the splitter that resizes it: what refers to what [uiState] asks about, or what its
 * field finds in the project's texts, read again after every change, Kubriko Creator's or one on disk
 * ([outsideChanges]), each going to its place when chosen ([goTo]).
 *
 * @param rootDirectory The project's folder, which the paths of hand-written Kotlin files start from.
 */
@Composable
internal fun WorkspaceSearchSidePanel(
    commands: CommandExecutor,
    selection: SelectionController,
    rootDirectory: File,
    outsideChanges: StateFlow<Int>,
    uiState: WorkspaceUiState,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    val subject = uiState.usagesSubject
    val revision = Triple(commands.history.collectAsState().value, commands.context.blockTypes?.types?.collectAsState()?.value, outsideChanges.collectAsState().value)
    val scope = rememberCoroutineScope()
    SearchPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        usagesSubject = subject,
        usages = rememberUsages(subject, revision, commands::findUsages),
        onUsageSelected = { usage -> scope.launch { selection.goTo(usage, subject, commands.context.scenes, rootDirectory, uiState::showProjectSettings) } },
        query = uiState.searchQuery,
        onQueryChange = uiState::changeSearchQuery,
        texts = rememberTextSearch(uiState.searchQuery, revision) { query -> commands.searchTexts(query, TEXT_HIT_LIMIT) },
        onHitSelected = { hit -> selection.open(hit.place) },
        onBackToSearch = uiState::stopFindingUsages,
        fieldFocus = uiState.searchFieldFocus,
    )
}

/**
 * How many lines a search of the project's texts lists at most, enough to look through; a longer query finds the rest.
 */
private const val TEXT_HIT_LIMIT = 200
