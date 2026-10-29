/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kotlinx.coroutines.flow.StateFlow
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_commit
import kubriko.creator.generated.resources.action_refresh
import kubriko.creator.generated.resources.action_start_version_control
import kubriko.creator.generated.resources.git_branch
import kubriko.creator.generated.resources.git_changes
import kubriko.creator.generated.resources.git_commit_message
import kubriko.creator.generated.resources.git_history
import kubriko.creator.generated.resources.git_no_changes
import kubriko.creator.generated.resources.git_no_repository
import kubriko.creator.generated.resources.git_title
import kubriko.creator.generated.resources.ic_branch
import kubriko.creator.generated.resources.ic_history
import kubriko.creator.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource

/**
 * Version control for the project: what changed since the last commit, a message and a button to commit it, and the
 * history of commits.
 */
@Composable
internal fun GitPanel(
    state: StateFlow<GitState>,
    onRefresh: () -> Unit,
    onStartVersionControl: () -> Unit,
    onCommit: (String) -> Unit,
    onDiscard: (ChangeEntry) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_branch,
        title = stringResource(Res.string.git_title),
    ) {
        IconActionButton(icon = Res.drawable.ic_refresh, label = Res.string.action_refresh, onClick = onRefresh)
    }
    when (val state = state.collectAsState().value) {
        GitState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        GitState.NoRepository -> NoRepository(onStartVersionControl)
        is GitState.Ready -> Repository(state, onCommit, onDiscard)
    }
}

@Composable
private fun NoRepository(
    onStartVersionControl: () -> Unit,
) = Column(
    modifier = Modifier.padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(
        text = stringResource(Res.string.git_no_repository),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onStartVersionControl) {
        Text(stringResource(Res.string.action_start_version_control))
    }
}

@Composable
private fun Repository(
    state: GitState.Ready,
    onCommit: (String) -> Unit,
    onDiscard: (ChangeEntry) -> Unit,
) {
    var message by remember { mutableStateOf("") }
    var entryToDiscard by remember { mutableStateOf<ChangeEntry?>(null) }
    LazyColumn {
        state.branch?.let { branch ->
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = stringResource(Res.string.git_branch, branch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Text(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                text = stringResource(Res.string.git_changes, state.changes.size),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (state.changes.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = stringResource(Res.string.git_no_changes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.changes) { entry ->
            ChangeRow(
                entry = entry,
                onDiscard = { entryToDiscard = entry },
            )
        }
        if (state.changes.isNotEmpty()) {
            item {
                CommitForm(
                    message = message,
                    onMessageChange = { message = it },
                    onCommit = {
                        onCommit(message)
                        message = ""
                    },
                )
            }
        }
        item {
            Column {
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                PanelHeader(
                    icon = Res.drawable.ic_history,
                    title = stringResource(Res.string.git_history),
                )
            }
        }
        items(state.history) { commit ->
            CommitRow(commit)
        }
    }
    entryToDiscard?.let { entry ->
        DiscardConfirmation(
            entry = entry,
            onConfirm = {
                onDiscard(entry)
                entryToDiscard = null
            },
            onDismiss = { entryToDiscard = null },
        )
    }
}

@Composable
private fun CommitForm(
    message: String,
    onMessageChange: (String) -> Unit,
    onCommit: () -> Unit,
) = Column {
    CompactTextField(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 56.dp),
        value = message,
        onValueChange = onMessageChange,
        placeholder = stringResource(Res.string.git_commit_message),
        isSingleLine = false,
    )
    Button(
        modifier = Modifier.padding(horizontal = 12.dp),
        enabled = message.isNotBlank(),
        onClick = onCommit,
    ) {
        Text(stringResource(Res.string.action_commit))
    }
}
