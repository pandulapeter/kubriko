/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * An Actor type being dragged from the sidebar into the open Scene, in the window's root coordinates. The Scene editor
 * registers where it is and what a drop does ([dropTarget]).
 */
@Stable
internal class ActorTypeDragState(
    private val clock: () -> Long = System::nanoTime,
) {
    private var lastDragEndedAt = Long.MIN_VALUE / 2
    var dragged by mutableStateOf<String?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var dropTarget: DropTarget? = null

    /**
     * Where Actor types can be dropped, and what happens to one dropped at a point (in root coordinates) inside it.
     */
    class DropTarget(
        val bounds: Rect,
        val onDrop: (actorTypeName: String, at: Offset) -> Unit,
    )

    val isOverDropTarget get() = dragged != null && dropTarget?.bounds?.contains(pointer) == true

    fun start(actorTypeName: String, at: Offset) {
        dragged = actorTypeName
        pointer = at
    }

    fun moveTo(at: Offset) {
        pointer = at
    }

    fun end() {
        val actorTypeName = dragged ?: return
        val target = dropTarget?.takeIf { isOverDropTarget }
        cancel()
        target?.onDrop(actorTypeName, pointer)
    }

    fun cancel() {
        dragged = null
        lastDragEndedAt = clock()
    }

    /**
     * Whether a click now is the release of a drag that just ended, which the dragged row's own click ignores.
     */
    val isClickAfterDrag get() = clock() - lastDragEndedAt < CLICK_AFTER_DRAG_IN_NANOSECONDS

    private companion object {
        const val CLICK_AFTER_DRAG_IN_NANOSECONDS = 300_000_000L
    }
}
