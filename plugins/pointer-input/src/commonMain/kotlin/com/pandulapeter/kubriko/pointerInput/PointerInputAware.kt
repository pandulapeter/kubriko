/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.actor.Actor
import kotlinx.collections.immutable.PersistentMap

/**
 * An interface for actors that can react to pointer input (touch, mouse, etc.).
 *
 * Actors implementing this interface are automatically notified of pointer events
 * by the [PointerInputManager].
 */
interface PointerInputAware : Actor {

    /**
     * Called every frame with the current positions of all active pointers.
     *
     * @param pointers A map of [PointerId]s to their current positions in screen pixels.
     */
    fun handleActivePointers(pointers: PersistentMap<PointerId, Offset>) = Unit

    /**
     * Called when a pointer is pressed.
     *
     * @param pointerId The unique ID of the pointer.
     * @param screenOffset The position of the pointer in screen pixels.
     */
    fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) = Unit

    /**
     * Called when a pointer is released.
     *
     * @param pointerId The unique ID of the pointer.
     * @param screenOffset The position of the pointer in screen pixels.
     */
    fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) = Unit

    /**
     * Called when the position of a pointer (either pressed or hovering) changes.
     *
     * @param pointerId The unique ID of the pointer, or null if it's a hovering pointer without an ID.
     * @param screenOffset The new position of the pointer in screen pixels.
     */
    fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) = Unit

    /**
     * Called when a pointer is dragged across the screen.
     *
     * @param screenOffset The amount of movement in screen pixels since the last event.
     */
    fun onPointerDrag(screenOffset: Offset) = Unit

    /**
     * Called when a pinch-to-zoom or scroll-to-zoom gesture is detected.
     *
     * @param position The center of the zoom gesture in screen pixels.
     * @param factor The zoom factor. Values > 1 mean zoom in, values < 1 mean zoom out.
     */
    fun onPointerZoom(position: Offset, factor: Float) = Unit

    /**
     * Called when a pointer enters the viewport bounds.
     */
    fun onPointerEnteringTheViewport() = Unit

    /**
     * Called when a pointer leaves the viewport bounds.
     */
    fun onPointerLeavingTheViewport() = Unit
}