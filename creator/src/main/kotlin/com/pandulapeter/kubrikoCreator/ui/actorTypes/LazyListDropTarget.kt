/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Makes the lazy list of [listState] where items dragged with [state] are dropped: its first [rowCount] items, of
 * [rowContentType], are the rows the gaps are between, and it scrolls while an item is held near its top or bottom edge.
 */
@Composable
internal fun Modifier.lazyListDropTarget(
    state: ListDragState<*>,
    listState: LazyListState,
    rowCount: Int,
    rowContentType: Any,
): Modifier {
    var listTop by remember { mutableFloatStateOf(0f) }
    state.laidOutRows = { laidOutRows(listState.layoutInfo.visibleItemsInfo, listTop - listState.layoutInfo.viewportStartOffset, rowCount, rowContentType) }
    val isDragging = state.dragged != null
    val density = LocalDensity.current
    LaunchedEffect(listState, isDragging) {
        if (!isDragging) return@LaunchedEffect
        val edge = with(density) { AUTO_SCROLL_EDGE.toPx() }
        val maximumStep = with(density) { AUTO_SCROLL_MAXIMUM_STEP.toPx() }
        while (true) {
            withFrameNanos { }
            val step = state.autoScrollStep(edge, maximumStep)
            if (step != 0f) listState.scrollBy(step)
        }
    }
    return onGloballyPositioned { coordinates ->
        state.listBounds = coordinates.boundsInRoot()
        listTop = coordinates.positionInRoot().y
    }
}

/**
 * The rows among [items] (those of [rowContentType], the first [rowCount] items of the list), with their spans in root
 * coordinates for a list whose items start at [itemsTop]. With none of them laid out, the gap is after the last row if
 * the list shows what comes after the rows.
 */
internal fun laidOutRows(items: List<LazyListItemInfo>, itemsTop: Float, rowCount: Int, rowContentType: Any): LaidOutRows {
    val rows = items.filter { it.contentType == rowContentType }
    return LaidOutRows(
        firstIndex = rows.firstOrNull()?.index ?: if (items.any { it.index >= rowCount }) rowCount else 0,
        spans = rows.map { (itemsTop + it.offset)..(itemsTop + it.offset + it.size) },
    )
}

private val AUTO_SCROLL_EDGE = 40.dp
private val AUTO_SCROLL_MAXIMUM_STEP = 12.dp
