/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_done
import kubriko.creator.generated.resources.manager_type_kotlin_ui_hint
import kubriko.creator.generated.resources.manager_type_kotlin_ui_title
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a Manager type's Kotlin UI in a field wide enough for Compose code. Every change goes through [onCodeChange]
 * as it is typed, so there is nothing to confirm.
 */
@Composable
internal fun KotlinUiDialog(
    managerTypeName: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    modifier = Modifier.widthIn(max = 860.dp).padding(24.dp),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.manager_type_kotlin_ui_title, managerTypeName)) },
    text = {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.manager_type_kotlin_ui_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompactTextField(
                modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp),
                value = code,
                onValueChange = onCodeChange,
                isSingleLine = false,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
    },
    confirmButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_done)) }
    },
)
