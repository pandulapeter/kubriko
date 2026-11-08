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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.LineDiff
import com.pandulapeter.kubrikoCreator.template.ProposedFileChange
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_upgrade_file_deleted
import org.jetbrains.compose.resources.stringResource

/**
 * One file's [change], as the lines it changes with a few around them.
 */
@Composable
internal fun FileDiff(change: ProposedFileChange) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = if (change.after == null) stringResource(Res.string.project_upgrade_file_deleted, change.path) else change.path,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    DiffLines(remember(change) { LineDiff.withContext(LineDiff.of(change.before, change.after.orEmpty())) })
}
