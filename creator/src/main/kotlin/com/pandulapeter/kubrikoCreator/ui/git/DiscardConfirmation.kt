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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_discard
import kubriko.creator.generated.resources.git_discard_message
import kubriko.creator.generated.resources.git_discard_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before discarding a change, which can't be undone.
 */
@Composable
internal fun DiscardConfirmation(
    entry: ChangeEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.git_discard_title, entry.title)) },
    text = { Text(stringResource(Res.string.git_discard_message)) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_discard)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
)
