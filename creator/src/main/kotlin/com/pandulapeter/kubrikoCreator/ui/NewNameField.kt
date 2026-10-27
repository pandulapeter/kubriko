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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.ic_check
import kubriko.creator.generated.resources.ic_close
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Names and creates something new (an Actor type, a Scene): Enter creates it, Escape cancels, and a rejected name shows why.
 */
@Composable
internal fun NewNameField(
    placeholder: StringResource,
    onCreate: (String) -> StringResource?,
    onCancel: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 4.dp, bottom = 4.dp),
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    val focusRequester = remember { FocusRequester() }
    val create = { if (name.isNotBlank()) error = onCreate(name.trim()) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.Enter -> true.also { create() }
                        event.key == Key.Escape -> true.also { onCancel() }
                        else -> false
                    }
                },
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            placeholder = stringResource(placeholder),
            isError = error != null,
        )
        IconActionButton(
            icon = Res.drawable.ic_check,
            label = Res.string.action_add,
            onClick = create,
            isEnabled = name.isNotBlank(),
        )
        IconActionButton(
            icon = Res.drawable.ic_close,
            label = Res.string.action_cancel,
            onClick = onCancel,
        )
    }
    error?.let {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
