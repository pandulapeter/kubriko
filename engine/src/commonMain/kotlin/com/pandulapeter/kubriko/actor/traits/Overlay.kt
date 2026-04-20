/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
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
 * Should be implemented by [Actor]s that draw directly onto the viewport,
 * rather than being part of the game world.
 */
interface Overlay : LayerAware {

    /**
     * Determines the rendering order relative to other [Overlay] actors.
     * Actors with smaller [overlayDrawingOrder] values are drawn later (on top).
     */
    val overlayDrawingOrder: Float get() = 0f

    /**
     * Draws the overlay elements into the viewport's [DrawScope].
     */
    fun DrawScope.drawToViewport()
}