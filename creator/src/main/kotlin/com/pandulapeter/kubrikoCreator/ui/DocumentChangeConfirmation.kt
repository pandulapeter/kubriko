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

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_to_screenplay
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_delete
import kubriko.creator.generated.resources.document_add_to_screenplay_text
import kubriko.creator.generated.resources.document_add_to_screenplay_title
import kubriko.creator.generated.resources.document_delete_text
import kubriko.creator.generated.resources.document_delete_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before [pending] changes one of the user's documents: deleting it, or moving it into the screenplay as a page, each
 * saying what happens to the file. Anything else is never pending, so it shows nothing.
 */
@Composable
internal fun DocumentChangeConfirmation(
    pending: PendingDocumentChange,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val input = pending.invocation.input
    when (input) {
        is DeleteDocument.Input -> DocumentConfirmation(
            title = stringResource(Res.string.document_delete_title, input.document),
            text = stringResource(Res.string.document_delete_text),
            confirmLabel = stringResource(Res.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is AddDocumentToScreenplay.Input -> DocumentConfirmation(
            title = stringResource(Res.string.document_add_to_screenplay_title, input.document),
            text = stringResource(Res.string.document_add_to_screenplay_text, "${PageNaming.FOLDER}/${input.page}.${PageNaming.EXTENSION}"),
            confirmLabel = stringResource(Res.string.action_add_to_screenplay),
            isDestructive = false,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        else -> Unit
    }
}

@Composable
private fun DocumentConfirmation(
    title: String,
    text: String,
    confirmLabel: String,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = title,
    maxWidth = 480.dp,
    onDismiss = onDismiss,
    confirmButton = {
        Button(
            onClick = onConfirm,
            colors = if (isDestructive) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(confirmLabel)
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}
