/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.assistant_variables_failed
import kubriko.creator.generated.resources.assistant_variables_link
import kubriko.creator.generated.resources.assistant_variables_note
import kubriko.creator.generated.resources.assistant_variables_optional
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the values of a sign-in method's environment variables (an agent's own API key), which are kept in the
 * operating system's secret store and given to the agent from then on.
 *
 * @param hasFailed Whether the last values given couldn't be kept.
 */
@Composable
internal fun SignInVariablesForm(
    method: SignInMethod,
    hasFailed: Boolean,
    onSave: (Map<String, String>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) = AssistantCard(modifier) {
    val values = remember(method) { mutableStateMapOf<String, String>() }
    val variables = method.variables.orEmpty()
    val isComplete = canSaveVariables(variables, values)
    Text(
        text = stringResource(Res.string.assistant_variables_note, method.name, LocalAgent.current.name),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    variables.forEach { variable ->
        val label = variable.label ?: variable.name
        CompactTextField(
            modifier = Modifier.fillMaxWidth(),
            value = values[variable.name].orEmpty(),
            onValueChange = { values[variable.name] = it },
            placeholder = if (variable.isOptional) stringResource(Res.string.assistant_variables_optional, label) else label,
            isError = hasFailed,
            isSecret = variable.isSecret,
        )
    }
    if (hasFailed) {
        Text(
            text = stringResource(Res.string.assistant_variables_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = { onSave(values.toMap()) }, enabled = isComplete) {
            Text(stringResource(Res.string.action_save))
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.action_cancel))
        }
        method.link?.let { link ->
            TextButton(onClick = { openInBrowser(link) }) {
                Text(stringResource(Res.string.assistant_variables_link))
            }
        }
    }
}

/**
 * Whether every variable that isn't optional has a value.
 */
internal fun canSaveVariables(variables: List<SignInMethod.Variable>, values: Map<String, String>) =
    variables.all { it.isOptional || !values[it.name].isNullOrBlank() }
