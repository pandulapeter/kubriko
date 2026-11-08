/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.LineDiff
import com.pandulapeter.kubrikoCreator.ui.DiffLines
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_keep
import kubriko.creator.generated.resources.action_undo
import kubriko.creator.generated.resources.page_review_note
import kubriko.creator.generated.resources.page_review_title
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's changes to a page, from [before] to [after], shown as a diff of its Markdown until they are kept or
 * undone.
 */
@Composable
internal fun PageChangeReview(
    before: String,
    after: String,
    onKeep: () -> Unit,
    onUndo: () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(Res.string.page_review_title), style = MaterialTheme.typography.labelLarge)
            Text(text = stringResource(Res.string.page_review_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onKeep) { Text(stringResource(Res.string.action_keep)) }
        TextButton(onClick = onUndo) { Text(stringResource(Res.string.action_undo)) }
    }
    DiffLines(lines = remember(before, after) { LineDiff.of(before, after) }, isWrapped = true)
}
