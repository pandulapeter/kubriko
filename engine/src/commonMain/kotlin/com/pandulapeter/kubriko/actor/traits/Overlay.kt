/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor

/**
 * [Actor]s that implement this interface get to draw directly onto the viewport.
 */
interface Overlay : LayerAware {

    /**
     * This number will be used to determine the order of executing the [drawToViewport] function relative to other [Overlay] [Actor]s.
     * Actors with smaller [overlayDrawingOrder]s get drawn later (on top).
     */
    val overlayDrawingOrder: Float get() = 0f

    /**
     * Invoked by [Kubriko] to draw using the viewport drawing scope.
     */
    fun DrawScope.drawToViewport()
}