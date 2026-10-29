/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

/**
 * Which Actor of a Scene is under a point, as the Scene editor picks it for a click ([SceneBoundsGrid]).
 */
internal object SceneHitTest {

    /**
     * Where an Actor is drawn, in scene units.
     *
     * @param index The Actor's index in the Scene.
     * @param drawingOrder Lower is drawn on top, as Kubriko's `Visible.drawingOrder`.
     * @param isBackground Whether it is only picked when nothing else is there (the Scene settings, which fill the bounds).
     */
    data class Bounds(
        val index: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val drawingOrder: Float = 0f,
        val isBackground: Boolean = false,
    ) {
        fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
    }

    /**
     * Whether these bounds are drawn above [other]: those of a background never are, then the lowest drawing order is,
     * and of equal ones the Actor added later, which Kubriko draws later.
     */
    fun Bounds.isDrawnAbove(other: Bounds) = when {
        isBackground != other.isBackground -> !isBackground
        drawingOrder != other.drawingOrder -> drawingOrder < other.drawingOrder
        else -> index > other.index
    }
}
