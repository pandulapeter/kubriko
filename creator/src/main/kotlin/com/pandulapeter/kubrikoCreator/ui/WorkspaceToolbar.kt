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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_project
import kubriko.creator.generated.resources.action_play
import kubriko.creator.generated.resources.action_redo
import kubriko.creator.generated.resources.action_redo_named
import kubriko.creator.generated.resources.action_refresh
import kubriko.creator.generated.resources.action_stop
import kubriko.creator.generated.resources.action_toggle_ai_connection
import kubriko.creator.generated.resources.action_toggle_version_control
import kubriko.creator.generated.resources.action_undo
import kubriko.creator.generated.resources.action_undo_named
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_branch
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.ic_redo
import kubriko.creator.generated.resources.ic_refresh
import kubriko.creator.generated.resources.ic_stop
import kubriko.creator.generated.resources.ic_undo
import kubriko.creator.generated.resources.session_editing
import kubriko.creator.generated.resources.session_playing
import kubriko.creator.generated.resources.session_starting
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkspaceToolbar(
    sessionState: SessionState,
    history: CommandHistory,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    branch: String?,
    isAiConnected: Boolean,
    openPanel: SidePanel?,
    onPanelToggled: (SidePanel) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRefresh: () -> Unit,
    onCloseProject: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (sessionState == SessionState.Idle) {
        Button(
            contentPadding = ToolbarButtonPadding,
            colors = ButtonDefaults.buttonColors(containerColor = Accent.ACTION.color, contentColor = Color.White),
            onClick = onPlay,
        ) {
            ButtonContent(Res.drawable.ic_play, Res.string.action_play)
        }
    } else {
        FilledTonalButton(
            contentPadding = ToolbarButtonPadding,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            onClick = onStop,
        ) {
            ButtonContent(Res.drawable.ic_stop, Res.string.action_stop)
        }
    }
    sessionState.description()?.let { description ->
        if (sessionState is SessionState.Starting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.weight(1f))
    HistoryButtons(
        history = history,
        onUndo = onUndo,
        onRedo = onRedo,
    )
    PanelToggle(
        isOpen = openPanel == SidePanel.AI_CONNECTION,
        onClick = { onPanelToggled(SidePanel.AI_CONNECTION) },
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Res.drawable.ic_ai),
            contentDescription = null,
            tint = if (isAiConnected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = stringResource(Res.string.action_toggle_ai_connection),
        )
    }
    PanelToggle(
        isOpen = openPanel == SidePanel.VERSION_CONTROL,
        onClick = { onPanelToggled(SidePanel.VERSION_CONTROL) },
    ) {
        VersionControlLabel(branch)
    }
    IconActionButton(
        icon = Res.drawable.ic_refresh,
        label = Res.string.action_refresh,
        onClick = onRefresh,
    )
    IconActionButton(
        icon = Res.drawable.ic_close,
        label = Res.string.action_close_project,
        onClick = onCloseProject,
    )
}

@Composable
private fun HistoryButtons(
    history: CommandHistory,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    IconActionButton(
        icon = Res.drawable.ic_undo,
        label = history.nextUndoTitle?.let { stringResource(Res.string.action_undo_named, stringResource(it)) } ?: stringResource(Res.string.action_undo),
        onClick = onUndo,
        isEnabled = history.nextUndoTitle != null,
    )
    IconActionButton(
        icon = Res.drawable.ic_redo,
        label = history.nextRedoTitle?.let { stringResource(Res.string.action_redo_named, stringResource(it)) } ?: stringResource(Res.string.action_redo),
        onClick = onRedo,
        isEnabled = history.nextRedoTitle != null,
    )
}

@Composable
private fun PanelToggle(
    isOpen: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) = if (isOpen) {
    FilledTonalButton(
        contentPadding = ToolbarButtonPadding,
        onClick = onClick,
        content = content,
    )
} else {
    TextButton(
        contentPadding = ToolbarButtonPadding,
        onClick = onClick,
        content = content,
    )
}

@Composable
private fun VersionControlLabel(
    branch: String?,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(Res.drawable.ic_branch),
        contentDescription = null,
    )
    Text(
        modifier = Modifier.padding(start = 6.dp),
        text = branch ?: stringResource(Res.string.action_toggle_version_control),
    )
}

@Composable
private fun ButtonContent(
    icon: DrawableResource,
    label: StringResource,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(icon),
        contentDescription = null,
    )
    Text(
        modifier = Modifier.padding(start = 6.dp),
        text = stringResource(label),
    )
}

private val ToolbarButtonPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)

internal fun SessionState.description() = when (this) {
    SessionState.Idle -> null
    is SessionState.Starting -> Res.string.session_starting
    is SessionState.Running -> when (mode) {
        SessionMode.EDIT -> Res.string.session_editing
        SessionMode.PLAY -> Res.string.session_playing
    }
}
