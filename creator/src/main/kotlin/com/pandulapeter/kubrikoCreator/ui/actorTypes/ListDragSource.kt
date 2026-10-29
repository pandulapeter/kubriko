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

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Lets the element be dragged as [item] into a list, reporting the pointer to [state] in root coordinates.
 */
@Composable
internal fun <T : Any> Modifier.listDragSource(state: ListDragState<T>, item: () -> T): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentItem by rememberUpdatedState(item)
    return onGloballyPositioned { origin = it.positionInRoot() }.pointerInput(state) {
        detectDragGestures(
            onDragStart = { state.start(currentItem(), origin + it) },
            onDragEnd = state::end,
            onDragCancel = state::cancel,
            onDrag = { change, _ ->
                change.consume()
                state.moveTo(origin + change.position)
            },
        )
    }
}

/**
 * The key of the row last dragged from [draggedIndex] (while it is dragged, and after it is dropped, until the next
 * drag), so that the list draws it above the others while it moves to its new place.
 */
@Composable
internal fun rememberDraggedKey(draggedIndex: Int?, keys: List<Long>): Long? {
    var draggedKey by remember { mutableStateOf<Long?>(null) }
    SideEffect { draggedIndex?.let { draggedKey = keys.getOrNull(it) } }
    return draggedKey
}
