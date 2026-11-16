/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.mask

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.abs

/**
 * The size a mask made of a unit shape scaled per axis (a box, an ellipse) was last given, the absolute values of its
 * width and height, which tells a size set again from one that changes the shape.
 */
internal class RequestedSize(initial: SceneSize) {
    var width = abs(initial.width.raw)
        private set
    var height = abs(initial.height.raw)
        private set

    val size get() = SceneSize(width.sceneUnit, height.sceneUnit)

    /**
     * Takes [value] and returns whether it differs from the last size, by raw bits, so that setting the same NaN again is
     * no change either.
     */
    fun update(value: SceneSize): Boolean {
        val newWidth = abs(value.width.raw)
        val newHeight = abs(value.height.raw)
        if (newWidth.toRawBits() == width.toRawBits() && newHeight.toRawBits() == height.toRawBits()) return false
        width = newWidth
        height = newHeight
        return true
    }
}
