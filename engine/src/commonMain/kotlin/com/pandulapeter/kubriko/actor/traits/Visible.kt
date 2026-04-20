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
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Should be implemented by [Actor]s that have a visual representation in the scene.
 * A [Visible] actor must also be [Positionable] and [LayerAware].
 */
interface Visible : Positionable, LayerAware {

    /**
     * The bounding box of the actor, used for visibility and clipping.
     */
    override val body: BoxBody

    /**
     * Determines the rendering order relative to other [Visible] actors.
     * Actors with smaller [drawingOrder] values are drawn later (on top).
     */
    val drawingOrder: Float get() = 0f

    /**
     * Whether the actor should be drawn.
     */
    val isVisible: Boolean get() = true

    /**
     * Whether the drawing should be clipped to the actor's [body].
     */
    val shouldClip: Boolean get() = true

    /**
     * Draws the actor into the scene.
     * The [DrawScope] is already transformed based on the actor's position, scale, and rotation.
     */
    fun DrawScope.draw()
}