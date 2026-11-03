/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.ui.FileDiff
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_apply_upgrade
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_close
import kubriko.creator.generated.resources.target_change_created
import kubriko.creator.generated.resources.target_change_message
import kubriko.creator.generated.resources.target_change_title_off
import kubriko.creator.generated.resources.target_change_title_on
import org.jetbrains.compose.resources.stringResource

/**
 * Shows exactly what turning a target on or off changes in the build files, and the files it creates, before [onApply]
 * makes it; or why it can't be done.
 */
@Composable
internal fun TargetChangeDialog(
    change: PendingTargetChange,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(if (change.isOn) Res.string.target_change_title_on else Res.string.target_change_title_off, stringResource(change.target.title)),
    maxWidth = 760.dp,
    onDismiss = onDismiss,
    text = {
        Column(
            modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val result = change.result) {
                is TargetChange.Refused -> Text(
                    text = result.problem.text(result.arguments),
                    color = MaterialTheme.colorScheme.error,
                )
                is TargetChange.Proposed -> ProposedChange(result)
            }
        }
    },
    confirmButton = {
        if (change.result is TargetChange.Proposed) {
            Button(onClick = onApply) { Text(stringResource(Res.string.action_apply_upgrade)) }
        } else {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        }
    },
    dismissButton = if (change.result is TargetChange.Proposed) {
        { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } }
    } else {
        null
    },
)

@Composable
private fun ProposedChange(change: TargetChange.Proposed) {
    Text(stringResource(Res.string.target_change_message))
    change.changes.filterNot { it.isNew }.forEach { FileDiff(it) }
    change.changes.filter { it.isNew }.takeIf { it.isNotEmpty() }?.let { created ->
        Text(
            text = stringResource(Res.string.target_change_created, created.joinToString { it.path }),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
