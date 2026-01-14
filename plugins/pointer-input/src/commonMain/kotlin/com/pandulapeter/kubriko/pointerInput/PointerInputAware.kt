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

// TODO: Documentation
interface PointerInputAware : Actor {

    fun handleActivePointers(pointers: PersistentMap<PointerId, Offset>) = Unit

    fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) = Unit

    fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) = Unit

    fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) = Unit

    fun onPointerDrag(screenOffset: Offset) = Unit

    fun onPointerZoom(position: Offset, factor: Float) = Unit

    fun onPointerEnteringTheViewport() = Unit

    fun onPointerLeavingTheViewport() = Unit
}