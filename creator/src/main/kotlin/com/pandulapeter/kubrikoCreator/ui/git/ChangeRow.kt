/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.git

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_discard_changes
import kubriko.creator.generated.resources.git_change_added
import kubriko.creator.generated.resources.git_change_conflicting
import kubriko.creator.generated.resources.git_change_deleted
import kubriko.creator.generated.resources.git_change_modified
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.ic_undo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One change: how it changed (as git's A, M, D letters), what changed, and a button to discard it.
 */
@Composable
internal fun ChangeRow(
    entry: ChangeEntry,
    onDiscard: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        modifier = Modifier.width(12.dp),
        text = stringResource(entry.kind.letter),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = entry.kind.color(),
    )
    Icon(
        modifier = Modifier.size(14.dp),
        painter = painterResource(if (entry.isActorType) Res.drawable.ic_actor_type else Res.drawable.ic_file),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        modifier = Modifier.weight(1f),
        text = entry.title,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.StartEllipsis,
    )
    IconActionButton(
        icon = Res.drawable.ic_undo,
        label = Res.string.action_discard_changes,
        onClick = onDiscard,
    )
}

private val ChangeKind.letter
    get() = when (this) {
        ChangeKind.ADDED -> Res.string.git_change_added
        ChangeKind.MODIFIED -> Res.string.git_change_modified
        ChangeKind.DELETED -> Res.string.git_change_deleted
        ChangeKind.CONFLICTING -> Res.string.git_change_conflicting
    }

@Composable
private fun ChangeKind.color(): Color = when (this) {
    ChangeKind.ADDED -> MaterialTheme.colorScheme.primary
    ChangeKind.MODIFIED -> MaterialTheme.colorScheme.tertiary
    ChangeKind.DELETED, ChangeKind.CONFLICTING -> MaterialTheme.colorScheme.error
}
