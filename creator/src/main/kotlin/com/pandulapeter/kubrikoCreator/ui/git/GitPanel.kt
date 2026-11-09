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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kotlinx.coroutines.flow.StateFlow
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_commit_changes
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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Version control for the project: a message and a button to commit what changed since the last commit, kept in sight
 * above the list of what changed and the history of commits, however long that list is. Needs a bounded height.
 *
 * @param onCommit Commits the changes with the message, calling `onCommitted` once they are committed, which clears the
 * message (a commit that fails keeps it).
 * @param areScreenplayRemindersShown Whether to say when the game changed but no page of the screenplay did.
 */
@Composable
internal fun GitPanel(
    state: StateFlow<GitState>,
    onStartVersionControl: () -> Unit,
    onCommit: (message: String, onCommitted: () -> Unit) -> Unit,
    onDiscard: (ChangeEntry) -> Unit,
    modifier: Modifier = Modifier,
    areScreenplayRemindersShown: Boolean = false,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_branch,
        title = stringResource(Res.string.git_title),
    )
    when (val state = state.collectAsState().value) {
        GitState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        GitState.NoRepository -> NoRepository(onStartVersionControl)
        is GitState.Ready -> Repository(state, onCommit, onDiscard, areScreenplayRemindersShown, Modifier.weight(1f))
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

/**
 * The commit form above the list of changes and the history: the form stays in sight however far the list is scrolled,
 * and while it takes more than half of the panel's height (a short window, or a long message), it scrolls within that
 * half, so the list always keeps the rest.
 */
@Composable
private fun Repository(
    state: GitState.Ready,
    onCommit: (message: String, onCommitted: () -> Unit) -> Unit,
    onDiscard: (ChangeEntry) -> Unit,
    areScreenplayRemindersShown: Boolean,
    modifier: Modifier = Modifier,
) = BoxWithConstraints(
    modifier = modifier,
) {
    var message by remember { mutableStateOf("") }
    var entryToDiscard by remember { mutableStateOf<ChangeEntry?>(null) }
    val formHeight = maxHeight / 2
    Column {
        Column(
            modifier = Modifier.heightIn(max = formHeight).verticalScroll(rememberScrollState()),
        ) {
            state.branch?.let { branch ->
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = stringResource(Res.string.git_branch, branch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.changes.isNotEmpty()) {
                if (areScreenplayRemindersShown && isScreenplayReminderDue(state.changes)) {
                    ScreenplayReminder(modifier = Modifier.padding(top = 8.dp))
                }
                CommitForm(
                    message = message,
                    changeCount = state.changes.size,
                    onMessageChange = { message = it },
                    onCommit = {
                        val sent = message
                        onCommit(sent) { if (message == sent) message = "" }
                    },
                )
            }
        }
        ChangesAndHistory(
            modifier = Modifier.weight(1f),
            state = state,
            onDiscard = { entryToDiscard = it },
        )
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

/**
 * The changes under their count, then the history of commits, each row keyed by what it shows, so a row the user is
 * looking at stays in place when rows above it come or go.
 */
@Composable
private fun ChangesAndHistory(
    state: GitState.Ready,
    onDiscard: (ChangeEntry) -> Unit,
    modifier: Modifier = Modifier,
) = LazyColumn(
    modifier = modifier,
) {
    item(key = CHANGES_KEY) {
        Text(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            text = stringResource(Res.string.git_changes, state.changes.size),
            style = MaterialTheme.typography.titleSmall,
        )
    }
    if (state.changes.isEmpty()) {
        item(key = NO_CHANGES_KEY) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = stringResource(Res.string.git_no_changes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(state.changes, key = { it.paths }) { entry ->
        ChangeRow(
            entry = entry,
            onDiscard = { onDiscard(entry) },
        )
    }
    item(key = HISTORY_KEY) {
        Column {
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            PanelHeader(
                icon = Res.drawable.ic_history,
                title = stringResource(Res.string.git_history),
            )
        }
    }
    items(state.history, key = { it.id }) { commit ->
        CommitRow(commit)
    }
}

@Composable
private fun CommitForm(
    message: String,
    changeCount: Int,
    onMessageChange: (String) -> Unit,
    onCommit: () -> Unit,
) = Column {
    CompactTextField(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 56.dp, max = 120.dp),
        value = message,
        onValueChange = onMessageChange,
        placeholder = stringResource(Res.string.git_commit_message),
        isSingleLine = false,
    )
    Button(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        enabled = message.isNotBlank(),
        onClick = onCommit,
    ) {
        Text(pluralStringResource(Res.plurals.action_commit_changes, changeCount, changeCount))
    }
}

private const val CHANGES_KEY = "changes"
private const val NO_CHANGES_KEY = "noChanges"
private const val HISTORY_KEY = "history"
