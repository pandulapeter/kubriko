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
 * [Actor]s that want to be drawn on the Scene should implement this interface.
 * A [Visible] actor must also be [Positionable].
 */
interface Visible : Positionable, LayerAware {

    override val body: BoxBody

    /**
     * This number will be used to determine the order of executing the [draw] function relative to other [Visible] [Actor]s.
     * [Actor]s with smaller [drawingOrder]-s get drawn later (on top).
     */
    val drawingOrder: Float get() = 0f

    val isVisible: Boolean get() = true

    val shouldClip: Boolean get() = true

    /**
     * Implement this function to draw the [Actor] into the Scene using the [scope], that has already been positioned, scaled and rotated.
     * The units used within this drawing scope must always be raw values of [SceneUnit].
     */
    fun DrawScope.draw()
}