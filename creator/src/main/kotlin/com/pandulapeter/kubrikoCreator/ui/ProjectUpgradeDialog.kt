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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pandulapeter.kubrikoCreator.template.LineDiff
import com.pandulapeter.kubrikoCreator.template.ProposedFileChange
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
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
) = AlertDialog(
    modifier = Modifier.widthIn(max = 760.dp).padding(24.dp),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.project_upgrade_title)) },
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
        TextButton(onClick = onApply, enabled = upgrade.changes.isNotEmpty()) { Text(stringResource(Res.string.action_apply_upgrade)) }
    },
    dismissButton = {
        Row {
            if (onCommitFirst != null) TextButton(onClick = onCommitFirst) { Text(stringResource(Res.string.action_commit_first)) }
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_not_now)) }
        }
    },
)

@Composable
private fun FileDiff(change: ProposedFileChange) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = change.path,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    val lines = remember(change) { LineDiff.withContext(LineDiff.of(change.before, change.after)) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
            .horizontalScroll(rememberScrollState())
            .padding(8.dp)
            .width(IntrinsicSize.Max),
    ) {
        lines.forEach { line ->
            Text(
                modifier = Modifier.fillMaxWidth().background(line?.kind?.background ?: Color.Transparent),
                softWrap = false,
                text = line?.let { it.kind.prefix + it.text } ?: "⋯",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (line == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val LineDiff.Kind.prefix
    get() = when (this) {
        LineDiff.Kind.SAME -> "  "
        LineDiff.Kind.REMOVED -> "- "
        LineDiff.Kind.ADDED -> "+ "
    }

private val LineDiff.Kind.background
    get() = when (this) {
        LineDiff.Kind.SAME -> Color.Transparent
        LineDiff.Kind.REMOVED -> Color(0x40E05555)
        LineDiff.Kind.ADDED -> Color(0x4055B060)
    }
