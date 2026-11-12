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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_delete
import kubriko.creator.generated.resources.delete_confirmation_loading
import kubriko.creator.generated.resources.delete_confirmation_none
import kubriko.creator.generated.resources.delete_confirmation_read_failed
import kubriko.creator.generated.resources.delete_confirmation_refused
import kubriko.creator.generated.resources.delete_confirmation_usages
import kubriko.creator.generated.resources.usage_row
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before deleting [subject], listing every usage of it and what deleting it does there, each one leading to its
 * place in the project when it is in Kubriko Creator's model. Deleting waits until [usages] are read, and is refused
 * while one of them keeps it from being deleted.
 *
 * @param usages The usages, or null while they are being read.
 * @param readProblem Why the usages couldn't be read, if they couldn't: deleting still goes ahead, and says what it can't do.
 */
@Composable
internal fun DeleteConfirmation(
    subject: RelationSubject,
    usages: List<Usage>?,
    onConfirm: () -> Unit,
    readProblem: String? = null,
    onUsageSelected: (Usage) -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = subject.deleteConfirmationTitle(),
    maxWidth = 560.dp,
    onDismiss = onDismiss,
    confirmButton = {
        Button(
            enabled = usages != null && usages.none { it.effect == UsageEffect.PREVENTS_DELETING },
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(stringResource(Res.string.action_delete))
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (readProblem != null) {
            Text(text = stringResource(Res.string.delete_confirmation_read_failed, readProblem), color = MaterialTheme.colorScheme.error)
        } else {
            DeleteConfirmationSummary(usages)
        }
        if (!usages.isNullOrEmpty()) {
            UsageList(usages = usages, onUsageSelected = onUsageSelected)
        }
    }
}

@Composable
private fun DeleteConfirmationSummary(usages: List<Usage>?) = Text(
            text = stringResource(
                when {
                    usages == null -> Res.string.delete_confirmation_loading
                    usages.any { it.effect == UsageEffect.PREVENTS_DELETING } -> Res.string.delete_confirmation_refused
                    usages.isEmpty() -> Res.string.delete_confirmation_none
                    else -> Res.string.delete_confirmation_usages
                },
            ),
        )

@Composable
private fun UsageList(
    usages: List<Usage>,
    onUsageSelected: (Usage) -> Unit,
) = Column(
    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
) {
    usages.forEach { usage ->
        UsageRow(usage = usage, onClick = if (usage.canBeShown) ({ onUsageSelected(usage) }) else null)
    }
}

@Composable
private fun UsageRow(
    usage: Usage,
    onClick: (() -> Unit)?,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .padding(vertical = 6.dp),
) {
    Text(
        text = stringResource(Res.string.usage_row, usage.placeText(), usage.relationText()),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = usage.effect.text(),
        style = MaterialTheme.typography.bodySmall,
        color = if (usage.needsAttention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
