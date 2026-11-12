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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.MenuStyle
import com.pandulapeter.kubrikoCreator.ui.components.highlighted
import com.pandulapeter.kubrikoCreator.ui.components.matchHighlightStyle
import com.pandulapeter.kubrikoCreator.ui.components.scrollEdgeFades
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.choice_none_found
import kubriko.creator.generated.resources.quick_open_placeholder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Quick open: a field over everything in [items], listing what matches what is typed ([quickOpenMatchesOf]) the closest
 * first, with what matched highlighted. Up and Down move the lit row, Enter or a click opens it ([onOpen]), and Escape
 * or a click elsewhere closes it ([onDismiss]).
 */
@Composable
internal fun QuickOpenDialog(
    items: List<QuickOpenItem>,
    onOpen: (QuickOpenTarget) -> Unit,
    onDismiss: () -> Unit,
) = Popup(
    alignment = Alignment.TopCenter,
    offset = IntOffset(0, 96),
    onDismissRequest = onDismiss,
    properties = PopupProperties(focusable = true),
) {
    var query by remember { mutableStateOf("") }
    var lit by remember { mutableStateOf(0) }
    val matches = remember(query, items) { quickOpenMatchesOf(query, items) }
    val listState = rememberLazyListState()
    LaunchedEffect(matches) { lit = 0 }
    LaunchedEffect(lit) { if (lit in matches.indices) listState.animateScrollToItem(lit) }
    Surface(
        modifier = Modifier.width(560.dp),
        shape = MenuStyle.shape,
        color = MenuStyle.color,
        border = MenuStyle.border,
        shadowElevation = MenuStyle.elevation,
    ) {
        Column {
            QuickOpenField(
                query = query,
                onQueryChange = { query = it },
                onKey = { key ->
                    when (key) {
                        Key.DirectionDown -> if (matches.isNotEmpty()) lit = (lit + 1) % matches.size
                        Key.DirectionUp -> if (matches.isNotEmpty()) lit = (lit - 1 + matches.size) % matches.size
                        Key.Enter, Key.NumPadEnter -> matches.getOrNull(lit)?.let { onOpen(it.item.target) }
                        Key.Escape -> onDismiss()
                        else -> return@QuickOpenField false
                    }
                    true
                },
            )
            if (matches.isEmpty()) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = stringResource(Res.string.choice_none_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp).fillMaxWidth().scrollEdgeFades(listState, MenuStyle.color),
                state = listState,
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                itemsIndexed(matches) { index, match ->
                    QuickOpenRow(match = match, isLit = index == lit, onClick = { onOpen(match.item.target) })
                }
            }
        }
    }
}

@Composable
private fun QuickOpenField(
    query: String,
    onQueryChange: (String) -> Unit,
    onKey: (Key) -> Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompactTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event -> event.type == KeyEventType.KeyDown && onKey(event.key) },
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(Res.string.quick_open_placeholder),
    )
}

/**
 * One match: its icon in its kind's accent, its title with what matched highlighted, and where it is. Its row never
 * takes the focus, so the field keeps it.
 */
@Composable
private fun QuickOpenRow(
    match: QuickOpenMatch,
    isLit: Boolean,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .then(if (isLit) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
        .focusProperties { canFocus = false }
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val target = match.item.target
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(target.icon),
        contentDescription = null,
        tint = target.accent?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val style = matchHighlightStyle()
    Text(
        modifier = Modifier.weight(1f, fill = false),
        text = remember(match, style) { highlighted(match.item.title, match.titleRanges, style) },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    target.detailText(match.item.detail)?.let { detail ->
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
