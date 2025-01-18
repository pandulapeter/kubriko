/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.Actor

// TODO: Documentation
interface PointerInputAware : Actor {

    fun handleActivePointers(screenOffset: Offset) = Unit

    fun onPointerPressed(screenOffset: Offset) = Unit

    fun onPointerReleased(screenOffset: Offset) = Unit

    fun onPointerOffsetChanged(screenOffset: Offset) = Unit

    fun onPointerEnteringTheViewport() = Unit

    fun onPointerLeavingTheViewport() = Unit
}