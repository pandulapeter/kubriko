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
import com.pandulapeter.kubrikoCreator.ui.search.rememberUsages
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import kotlinx.coroutines.launch
import java.io.File

/**
 * The workspace's Search panel, after the splitter that resizes it: what refers to what [uiState] asks about, read again
 * after every change, each going to its place when chosen ([goTo]).
 *
 * @param rootDirectory The project's folder, which the paths of hand-written Kotlin files start from.
 */
@Composable
internal fun WorkspaceSearchSidePanel(
    commands: CommandExecutor,
    selection: SelectionController,
    rootDirectory: File,
    uiState: WorkspaceUiState,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    val subject = uiState.usagesSubject
    val revision = commands.history.collectAsState().value to commands.context.blockTypes?.types?.collectAsState()?.value
    val scope = rememberCoroutineScope()
    SearchPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        usagesSubject = subject,
        usages = rememberUsages(subject, revision, commands::findUsages),
        onUsageSelected = { usage -> scope.launch { selection.goTo(usage, subject, commands.context.scenes, rootDirectory, uiState::showProjectSettings) } },
    )
}
