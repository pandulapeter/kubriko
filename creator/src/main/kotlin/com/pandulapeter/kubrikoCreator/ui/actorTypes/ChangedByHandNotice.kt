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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_discard
import kubriko.creator.generated.resources.action_discard_changes_made_by_hand
import kubriko.creator.generated.resources.actor_type_changed_by_hand
import kubriko.creator.generated.resources.discard_changes_made_by_hand_message
import kubriko.creator.generated.resources.discard_changes_made_by_hand_title
import kubriko.creator.generated.resources.ic_warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tells that an Actor type's generated code was changed outside Kubriko Creator, which stopped updating it.
 */
@Composable
internal fun ChangedByHandNotice(
    actorTypeName: String,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    shape = MaterialTheme.shapes.medium,
) {
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.actor_type_changed_by_hand, actorTypeName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        var isConfirming by remember(actorTypeName) { mutableStateOf(false) }
        Button(
            onClick = { isConfirming = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Text(stringResource(Res.string.action_discard_changes_made_by_hand))
        }
        if (isConfirming) {
            DiscardConfirmation(
                actorTypeName = actorTypeName,
                onConfirm = {
                    isConfirming = false
                    onDiscard()
                },
                onDismiss = { isConfirming = false },
            )
        }
    }
}

@Composable
private fun DiscardConfirmation(
    actorTypeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.discard_changes_made_by_hand_title, actorTypeName)) },
    text = { Text(stringResource(Res.string.discard_changes_made_by_hand_message)) },
    confirmButton = {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(stringResource(Res.string.action_discard))
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
)
