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
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.ui.components.PanelRow
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidthState
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.git.GitPanel
import kotlinx.coroutines.flow.StateFlow

/**
 * The workspace's Version control panel, after the splitter that resizes it, with every change made through a command.
 *
 * @param onCommand Dispatches a command, calling its second argument once the command succeeded.
 */
@Composable
internal fun WorkspaceGitSidePanel(
    gitState: StateFlow<GitState>,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    width: PanelWidthState,
    row: PanelRow,
) {
    PanelSplitter(width, row)
    GitPanel(
        modifier = Modifier.sidePanel(width.widthIn(row)),
        state = gitState,
        onStartVersionControl = { onCommand(Invocation(StartVersionControl, NoInput)) {} },
        onCommit = { message, onCommitted -> onCommand(Invocation(Commit, Commit.Input(message)), onCommitted) },
        onDiscard = { change -> onCommand(Invocation(DiscardChanges, DiscardChanges.Input(change.paths))) {} },
    )
}
