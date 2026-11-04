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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.ui.commandTitleText
import com.pandulapeter.kubrikoCreator.ui.description
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_changes_more
import kubriko.creator.generated.resources.assistant_changes_title
import kubriko.creator.generated.resources.console_with_prefix
import org.jetbrains.compose.resources.stringResource

/**
 * What a turn changed in the project, listed before the agent's final answer.
 */
@Composable
internal fun TurnChangesCard(
    changes: TranscriptItem.TurnChanges,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = stringResource(Res.string.assistant_changes_title),
        style = MaterialTheme.typography.labelLarge,
    )
    changes.changes.take(SHOWN_CHANGES).forEach { change ->
        Text(
            text = change.target?.let { stringResource(Res.string.console_with_prefix, commandTitleText(change.commandName), it.description()) } ?: commandTitleText(change.commandName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (changes.changes.size > SHOWN_CHANGES) {
        Text(
            text = stringResource(Res.string.assistant_changes_more, changes.changes.size - SHOWN_CHANGES),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val SHOWN_CHANGES = 8
