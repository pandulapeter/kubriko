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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_project
import kubriko.creator.generated.resources.action_play
import kubriko.creator.generated.resources.action_stop
import kubriko.creator.generated.resources.session_editing
import kubriko.creator.generated.resources.session_playing
import kubriko.creator.generated.resources.session_starting
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkspaceToolbar(
    projectName: String,
    sessionState: SessionState,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onCloseProject: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = projectName,
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.weight(1f))
    sessionState.description()?.let { description ->
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (sessionState == SessionState.Idle) {
        Button(onClick = onPlay) {
            Text(stringResource(Res.string.action_play))
        }
    } else {
        OutlinedButton(onClick = onStop) {
            Text(stringResource(Res.string.action_stop))
        }
    }
    TextButton(onClick = onCloseProject) {
        Text(stringResource(Res.string.action_close_project))
    }
}

internal fun SessionState.description() = when (this) {
    SessionState.Idle -> null
    is SessionState.Starting -> Res.string.session_starting
    is SessionState.Running -> when (mode) {
        SessionMode.EDIT -> Res.string.session_editing
        SessionMode.PLAY -> Res.string.session_playing
    }
}
