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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.new_actor_type_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Names and creates a new Actor type, showing why a name was rejected.
 */
@Composable
internal fun NewActorTypeField(
    onCreate: (String) -> StringResource?,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            placeholder = { Text(stringResource(Res.string.new_actor_type_hint)) },
            isError = error != null,
            supportingText = error?.let { { Text(stringResource(it)) } },
            singleLine = true,
        )
        TextButton(
            enabled = name.isNotBlank(),
            onClick = {
                error = onCreate(name.trim())
                if (error == null) name = ""
            },
        ) {
            Text(stringResource(Res.string.action_add))
        }
    }
}
