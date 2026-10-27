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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_clear_console
import kubriko.creator.generated.resources.console_title
import kubriko.creator.generated.resources.console_with_prefix
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Console(
    modifier: Modifier,
    entries: List<ConsoleEntry>,
    onClear: () -> Unit,
) = Column(
    modifier = modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.console_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClear) {
            Text(stringResource(Res.string.action_clear_console))
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.scrollToItem(entries.lastIndex)
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        state = listState,
    ) {
        itemsIndexed(entries) { _, entry ->
            ConsoleLine(entry)
        }
    }
}

@Composable
private fun ConsoleLine(
    entry: ConsoleEntry,
) = Text(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.dp),
    text = entry.text(),
    color = entry.color(),
    style = MaterialTheme.typography.bodySmall,
    fontFamily = FontFamily.Monospace,
)

@Composable
private fun ConsoleEntry.text() = when (this) {
    is ConsoleEntry.Status -> stringResource(text)
    is ConsoleEntry.Problem -> if (location == null) message else stringResource(Res.string.console_with_prefix, location, message)
    is ConsoleEntry.BuildOutput -> line
    is ConsoleEntry.Log -> if (source == null) message else stringResource(Res.string.console_with_prefix, source, message)
    is ConsoleEntry.SessionFailure -> message
}

@Composable
private fun ConsoleEntry.color(): Color = when (this) {
    is ConsoleEntry.Status -> if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    is ConsoleEntry.Problem, is ConsoleEntry.SessionFailure -> MaterialTheme.colorScheme.error
    is ConsoleEntry.BuildOutput -> MaterialTheme.colorScheme.onSurfaceVariant
    is ConsoleEntry.Log -> if (isImportant) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
}
