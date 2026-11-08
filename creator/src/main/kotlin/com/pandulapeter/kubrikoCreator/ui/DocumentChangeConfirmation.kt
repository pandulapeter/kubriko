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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_to_screenplay
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_delete
import kubriko.creator.generated.resources.document_add_to_screenplay_page
import kubriko.creator.generated.resources.document_add_to_screenplay_text
import kubriko.creator.generated.resources.document_add_to_screenplay_title
import kubriko.creator.generated.resources.document_delete_text
import kubriko.creator.generated.resources.document_delete_title
import kubriko.creator.generated.resources.page_delete_images
import kubriko.creator.generated.resources.page_delete_mentioned_in
import kubriko.creator.generated.resources.page_delete_text
import kubriko.creator.generated.resources.page_delete_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException

/**
 * Asks before [pending] changes one of the user's documents: deleting it, or moving it into the screenplay as a page,
 * each saying what happens to the file; or before deleting a page, whether its images ([unusedImagesOf]) go with it. The page's name can still be changed, and is checked with [validate] as it is
 * typed, since the one offered may be taken. Anything else is never pending, so it shows nothing.
 *
 * @param onConfirm Makes the change as confirmed.
 */
@Composable
internal fun DocumentChangeConfirmation(
    pending: PendingDocumentChange,
    unusedImagesOf: suspend (page: String) -> List<String>,
    usagesOf: suspend (Invocation<*>) -> List<Usage>?,
    validate: (Invocation<*>) -> Rejection?,
    onConfirm: (Invocation<*>) -> Unit,
    onDismiss: () -> Unit,
) {
    when (val input = pending.invocation.input) {
        is DeleteDocument.Input -> DocumentConfirmation(
            title = stringResource(Res.string.document_delete_title, input.document),
            confirmLabel = stringResource(Res.string.action_delete),
            isDestructive = true,
            onConfirm = { onConfirm(pending.invocation) },
            onDismiss = onDismiss,
        ) {
            Text(text = stringResource(Res.string.document_delete_text), style = MaterialTheme.typography.bodyMedium)
        }
        is AddDocumentToScreenplay.Input -> {
            var page by remember(pending) { mutableStateOf(input.page) }
            val invocation = Invocation(AddDocumentToScreenplay, input.copy(page = page.trim()))
            val rejection = remember(invocation) { validate(invocation) }
            DocumentConfirmation(
                title = stringResource(Res.string.document_add_to_screenplay_title, input.document),
                confirmLabel = stringResource(Res.string.action_add_to_screenplay),
                isDestructive = false,
                isConfirmable = rejection == null,
                onConfirm = { onConfirm(invocation) },
                onDismiss = onDismiss,
            ) {
                CompactTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = page,
                    onValueChange = { page = it },
                    label = stringResource(Res.string.document_add_to_screenplay_page),
                    isError = rejection != null,
                )
                val error = rejection?.toFieldError()
                if (error != null) {
                    Text(text = stringResource(error.text, *error.arguments.toTypedArray()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(text = stringResource(Res.string.document_add_to_screenplay_text, PageNaming.pathOf(page.trim())), style = MaterialTheme.typography.bodyMedium)
            }
        }
        is DeletePage.Input -> PageDeleteConfirmation(input, unusedImagesOf, usagesOf, onConfirm, onDismiss)
        else -> Unit
    }
}

/**
 * Deletes a page at once when it has no images only it shows and nothing mentions it, and otherwise asks, saying which pages
 * mention it (their links become plain text) and whether its images go with it (they do unless the user
 * says otherwise).
 */
@Composable
private fun PageDeleteConfirmation(
    input: DeletePage.Input,
    unusedImagesOf: suspend (page: String) -> List<String>,
    usagesOf: suspend (Invocation<*>) -> List<Usage>?,
    onConfirm: (Invocation<*>) -> Unit,
    onDismiss: () -> Unit,
) {
    val facts = produceState<PageDeleteFacts?>(null, input.page) {
        value = coroutineScope {
            val images = async { runCatching { unusedImagesOf(input.page) }.getOrDefault(emptyList()) }
            val mentions = async {
                try {
                    usagesOf(Invocation(DeletePage, input)).orEmpty().mapNotNull { (it.place as? UsagePlace.Markdown)?.path }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    null
                }
            }
            PageDeleteFacts(images.await(), mentions.await())
        }
    }.value ?: return
    // Unless nothing mentions it for sure, the user is asked, since the delete would change the pages mentioning it.
    if (facts.images.isEmpty() && facts.mentionedIn?.isEmpty() == true) {
        LaunchedEffect(input) { onConfirm(Invocation(DeletePage, input.copy(deleteImages = false))) }
        return
    }
    var deletesImages by remember(input) { mutableStateOf(true) }
    DocumentConfirmation(
        title = stringResource(Res.string.page_delete_title, input.page),
        confirmLabel = stringResource(Res.string.action_delete),
        isDestructive = true,
        onConfirm = { onConfirm(Invocation(DeletePage, input.copy(deleteImages = deletesImages && facts.images.isNotEmpty()))) },
        onDismiss = onDismiss,
    ) {
        Text(text = stringResource(Res.string.page_delete_text), style = MaterialTheme.typography.bodyMedium)
        val mentionedIn = facts.mentionedIn
        if (!mentionedIn.isNullOrEmpty()) {
            Text(text = stringResource(Res.string.page_delete_mentioned_in), style = MaterialTheme.typography.bodyMedium)
            mentionedIn.forEach { path ->
                Text(text = PageNaming.nameOf(path) ?: path, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (facts.images.isNotEmpty()) {
            LabeledSwitch(
                label = pluralStringResource(Res.plurals.page_delete_images, facts.images.size, facts.images.size),
                checked = deletesImages,
                onCheckedChange = { deletesImages = it },
                labelStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * What deleting a page takes with it: the [images] only it shows, and the pages whose links to it become
 * plain text ([mentionedIn], by their paths from the project's root, or null if they couldn't be read).
 */
private class PageDeleteFacts(
    val images: List<String>,
    val mentionedIn: List<String>?,
)

@Composable
private fun DocumentConfirmation(
    title: String,
    confirmLabel: String,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isConfirmable: Boolean = true,
    content: @Composable () -> Unit,
) = CreatorDialog(
    title = title,
    maxWidth = 480.dp,
    onDismiss = onDismiss,
    confirmButton = {
        Button(
            enabled = isConfirmable,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}
