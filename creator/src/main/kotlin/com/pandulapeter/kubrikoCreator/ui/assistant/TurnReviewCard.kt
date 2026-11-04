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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_keep
import kubriko.creator.generated.resources.action_undo
import kubriko.creator.generated.resources.assistant_changes_kept
import kubriko.creator.generated.resources.assistant_changes_partly_undone
import kubriko.creator.generated.resources.assistant_changes_undone
import org.jetbrains.compose.resources.stringResource

/**
 * Keep and Undo for everything a turn changed, after the agent's final answer, until one is chosen, and then which one was.
 */
@Composable
internal fun TurnReviewCard(
    review: TranscriptItem.TurnReview,
    onKeep: () -> Unit,
    onUndo: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
        .padding(10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    when (review.state) {
        TranscriptItem.TurnReview.State.OFFERED -> {
            FilledTonalButton(onClick = onKeep) {
                Text(stringResource(Res.string.action_keep))
            }
            TextButton(onClick = onUndo) {
                Text(stringResource(Res.string.action_undo))
            }
        }
        TranscriptItem.TurnReview.State.KEPT -> Status(stringResource(Res.string.assistant_changes_kept))
        TranscriptItem.TurnReview.State.UNDONE -> Status(stringResource(Res.string.assistant_changes_undone))
        TranscriptItem.TurnReview.State.PARTLY_UNDONE -> Status(stringResource(Res.string.assistant_changes_partly_undone))
    }
}

@Composable
private fun Status(
    text: String,
) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.primary,
)
