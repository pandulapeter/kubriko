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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.template.LineDiff
import com.pandulapeter.kubrikoCreator.template.ProposedFileChange
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_upgrade_file_copied
import kubriko.creator.generated.resources.project_upgrade_file_deleted
import org.jetbrains.compose.resources.stringResource

/**
 * One file's [change], as the lines it changes with a few around them, or only named for a file that isn't text.
 */
@Composable
internal fun FileDiff(change: ProposedFileChange) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = when {
            change.after == null -> stringResource(Res.string.project_upgrade_file_deleted, change.path)
            change.resource != null -> stringResource(Res.string.project_upgrade_file_copied, change.path)
            else -> change.path
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    if (change.resource != null) return@Column
    val lines = remember(change) { LineDiff.withContext(LineDiff.of(change.before, change.after.orEmpty())) }
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

private val LineDiff.Kind.background
    get() = when (this) {
        LineDiff.Kind.SAME -> Color.Transparent
        LineDiff.Kind.REMOVED -> Color(0x40E05555)
        LineDiff.Kind.ADDED -> Color(0x4055B060)
    }
