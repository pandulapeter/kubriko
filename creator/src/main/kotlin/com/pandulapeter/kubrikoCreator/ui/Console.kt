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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_clear_console
import kubriko.creator.generated.resources.action_collapse_console
import kubriko.creator.generated.resources.action_expand_console
import kubriko.creator.generated.resources.console_title
import kubriko.creator.generated.resources.console_with_prefix
import kubriko.creator.generated.resources.ic_clear
import kubriko.creator.generated.resources.ic_console
import kubriko.creator.generated.resources.ic_move_up
import org.jetbrains.compose.resources.stringResource

/**
 * The console at the bottom of the workspace, which can be collapsed to its header.
 */
@Composable
internal fun Console(
    lines: ConsoleLines,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    listHeight: Dp,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    PanelHeader(
        icon = Res.drawable.ic_console,
        title = stringResource(Res.string.console_title),
    ) {
        AnimatedVisibility(
            modifier = Modifier.weight(1f),
            visible = !isExpanded,
            enter = fadeIn(CreatorMotion.medium()),
            exit = fadeOut(CreatorMotion.short()),
        ) {
            lines.entries.lastOrNull()?.let { entry ->
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = entry.text(),
                    color = entry.color(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconActionButton(
            icon = Res.drawable.ic_clear,
            label = Res.string.action_clear_console,
            onClick = onClear,
            isEnabled = lines.entries.isNotEmpty(),
        )
        val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f, CreatorMotion.medium())
        IconActionButton(
            modifier = Modifier.graphicsLayer { rotationZ = arrowRotation },
            icon = Res.drawable.ic_move_up,
            label = if (isExpanded) Res.string.action_collapse_console else Res.string.action_expand_console,
            onClick = { onExpandedChange(!isExpanded) },
        )
    }
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(CreatorMotion.medium()) + fadeIn(CreatorMotion.medium()),
        exit = shrinkVertically(CreatorMotion.medium()) + fadeOut(CreatorMotion.short()),
    ) {
        val listState = rememberLazyListState()
        var shownLastNumber by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(lines.revision) {
            val previousLastNumber = shownLastNumber
            shownLastNumber = lines.lastNumber
            val visibleNumbers = listState.layoutInfo.visibleItemsInfo.map { it.key }
            if (lines.entries.isNotEmpty() && isFollowing(previousLastNumber, lines.firstNumber, visibleNumbers)) {
                listState.scrollToItem(lines.entries.lastIndex)
            }
        }
        SelectionContainer {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(listHeight),
                state = listState,
            ) {
                itemsIndexed(lines.entries, key = { index, _ -> lines.firstNumber + index }) { _, entry ->
                    ConsoleLine(entry)
                }
            }
        }
    }
}

/**
 * Whether the console's list goes on showing the newest line: it showed the one that was last before
 * ([previousLastNumber]) among [visibleNumbers], or nothing was shown yet, or that line is gone (older than
 * [firstNumber], the first one kept), so nothing the user was reading is left.
 */
internal fun isFollowing(previousLastNumber: Long?, firstNumber: Long, visibleNumbers: List<Any>): Boolean =
    previousLastNumber == null || previousLastNumber < firstNumber || previousLastNumber in visibleNumbers

private val ConsoleLines.lastNumber get() = entries.lastIndex.takeIf { it >= 0 }?.let { firstNumber + it }

@Composable
private fun ConsoleLine(
    entry: ConsoleEntry,
) = Text(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp),
    text = entry.text(),
    color = entry.color(),
    style = MaterialTheme.typography.bodySmall,
    fontFamily = FontFamily.Monospace,
)

@Composable
private fun ConsoleEntry.text() = when (this) {
    is ConsoleEntry.Status -> text.text(arguments)
    is ConsoleEntry.Problem -> location?.let { stringResource(Res.string.console_with_prefix, it, message) } ?: message
    is ConsoleEntry.BuildOutput -> line
    is ConsoleEntry.Log -> source?.let { stringResource(Res.string.console_with_prefix, it, message) } ?: message
    is ConsoleEntry.SessionFailure -> message
}

@Composable
private fun ConsoleEntry.color(): Color = when (this) {
    is ConsoleEntry.Status -> if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    is ConsoleEntry.Problem, is ConsoleEntry.SessionFailure -> MaterialTheme.colorScheme.error
    is ConsoleEntry.BuildOutput -> MaterialTheme.colorScheme.onSurfaceVariant
    is ConsoleEntry.Log -> if (isImportant) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Whether the console opens by itself when [this] arrives: a problem or an error, or something the game logged, which
 * is how Log Blocks are seen. Build output and other status lines don't open it.
 */
internal val ConsoleEntry.opensConsole
    get() = when (this) {
        is ConsoleEntry.Problem, is ConsoleEntry.Log, is ConsoleEntry.SessionFailure -> true
        is ConsoleEntry.Status -> isError
        is ConsoleEntry.BuildOutput -> false
    }
