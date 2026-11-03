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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_settings_library_more
import org.jetbrains.compose.resources.stringResource

/**
 * The Libraries in use section of Project settings: each Kubriko library the build depends on, and why.
 */
@Composable
internal fun LibrariesPage(
    libraries: List<LibraryInUse>,
) = Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    libraries.forEach { library -> LibraryRow(library) }
}

@Composable
private fun LibraryRow(
    library: LibraryInUse,
) = Column(
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(library.artifact.title),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = library.artifact.artifactId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val reasons = reasonsOf(library)
    reasons.take(MAXIMUM_REASONS).forEach { reason ->
        Text(
            text = reason.text(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (reasons.size > MAXIMUM_REASONS) {
        Text(
            text = stringResource(Res.string.project_settings_library_more, reasons.size - MAXIMUM_REASONS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val MAXIMUM_REASONS = 6
