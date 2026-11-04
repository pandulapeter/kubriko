/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_cancel
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_save
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.description_dialog_title
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.description_length
import org.jetbrains.compose.resources.stringResource

/**
 * Where a [DescriptionPreview]'s description is edited: a field with room for all of it, wrapping, with how much of the
 * [maximumLength] is used. Enter saves, as the description is one line.
 */
@Composable
internal fun DescriptionDialog(
    name: String,
    initialDescription: String,
    placeholder: String,
    maximumLength: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialDescription, TextRange(initialDescription.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val save = { onSave(value.text.trim()) }
    CreatorDialog(
        title = stringResource(Res.string.description_dialog_title, name),
        maxWidth = 520.dp,
        onDismiss = onDismiss,
        confirmButton = { TextButton(onClick = save) { Text(stringResource(Res.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompactTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (isEnter && event.type == KeyEventType.KeyDown) save()
                        isEnter
                    },
                value = value,
                onValueChange = { value = it.copy(text = descriptionLine(it.text, maximumLength)) },
                placeholder = placeholder,
                isSingleLine = false,
            )
            Text(
                modifier = Modifier.align(Alignment.End),
                text = stringResource(Res.string.description_length, value.text.length, maximumLength),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * [text] as a description can hold it: on one line, with every line break a space, and at most [maximumLength] long.
 */
internal fun descriptionLine(text: String, maximumLength: Int): String = text
    .replace("\r\n", " ")
    .replace('\n', ' ')
    .replace('\r', ' ')
    .take(maximumLength)
