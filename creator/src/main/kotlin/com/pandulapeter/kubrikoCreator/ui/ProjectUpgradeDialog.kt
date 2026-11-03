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
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_apply_upgrade
import kubriko.creator.generated.resources.action_commit_first
import kubriko.creator.generated.resources.action_not_now
import kubriko.creator.generated.resources.project_upgrade_by_hand
import kubriko.creator.generated.resources.project_upgrade_message
import kubriko.creator.generated.resources.project_upgrade_title
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the exact changes an upgrade makes to the project's template files before they are made, and what has to be
 * done by hand.
 *
 * @param onCommitFirst Commits the project as it is, or null if there is nothing to commit.
 */
@Composable
internal fun ProjectUpgradeDialog(
    upgrade: TemplateUpgrade.Needed,
    onApply: () -> Unit,
    onCommitFirst: (() -> Unit)?,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.project_upgrade_title),
    maxWidth = 760.dp,
    onDismiss = onDismiss,
    text = {
        Column(
            modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.project_upgrade_message))
            upgrade.changes.forEach { FileDiff(it) }
            if (upgrade.byHand.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.project_upgrade_by_hand, upgrade.byHand.joinToString()),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    },
    confirmButton = {
        Button(onClick = onApply, enabled = upgrade.changes.isNotEmpty()) { Text(stringResource(Res.string.action_apply_upgrade)) }
    },
    dismissButton = {
        Row {
            if (onCommitFirst != null) TextButton(onClick = onCommitFirst) { Text(stringResource(Res.string.action_commit_first)) }
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_not_now)) }
        }
    },
)
