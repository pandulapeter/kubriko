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

import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths
import com.pandulapeter.kubrikoCreator.ui.components.sidePanel
import com.pandulapeter.kubrikoCreator.ui.git.GitPanel
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController

/**
 * The workspace's Version control panel, after a divider, with every change made through a command.
 */
@Composable
internal fun WorkspaceGitSidePanel(
    controller: WorkspaceController,
) {
    VerticalDivider()
    GitPanel(
        modifier = Modifier.sidePanel(PanelWidths.VERSION_CONTROL),
        state = controller.git.state,
        onRefresh = controller.git::refresh,
        onStartVersionControl = { controller.commands.dispatch(Invocation(StartVersionControl, NoInput)) },
        onCommit = { message, onCommitted -> controller.commands.dispatch(Invocation(Commit, Commit.Input(message)), onSuccess = onCommitted) },
        onDiscard = { change -> controller.commands.dispatch(Invocation(DiscardChanges, DiscardChanges.Input(change.paths))) },
    )
}
