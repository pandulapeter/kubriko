/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui

import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker

// Reusable sample storage for the minimap overlay. Marker positions and footprint half-sizes are
// stored in screen-scaled pixels (world * viewport scale), quantized to half-pixel steps so the
// sub-pixel wiggle of idle animations doesn't register as a content change. Colors are stored as
// ARGB ints so writing them doesn't box Color values; arrays only ever grow.
internal class MiniMapBuffer {
    var markerCount = 0
    var markerX = FloatArray(INITIAL_CAPACITY)
    var markerY = FloatArray(INITIAL_CAPACITY)
    var markerHalfWidth = FloatArray(INITIAL_CAPACITY)
    var markerHalfHeight = FloatArray(INITIAL_CAPACITY)
    var markerRotation = FloatArray(INITIAL_CAPACITY)
    var markerColor = IntArray(INITIAL_CAPACITY)
    var markerDrawers = arrayOfNulls<MiniMapMarker>(INITIAL_CAPACITY)

    fun ensureCapacity(capacity: Int) {
        if (markerX.size < capacity) {
            val newSize = maxOf(capacity, markerX.size * 2)
            markerX = markerX.copyOf(newSize)
            markerY = markerY.copyOf(newSize)
            markerHalfWidth = markerHalfWidth.copyOf(newSize)
            markerHalfHeight = markerHalfHeight.copyOf(newSize)
            markerRotation = markerRotation.copyOf(newSize)
            markerColor = markerColor.copyOf(newSize)
            markerDrawers = markerDrawers.copyOf(newSize)
        }
    }

    fun contentEquals(other: MiniMapBuffer): Boolean {
        if (markerCount != other.markerCount) return false
        for (i in 0 until markerCount) {
            if (markerX[i] != other.markerX[i] || markerY[i] != other.markerY[i]
                || markerHalfWidth[i] != other.markerHalfWidth[i]
                || markerHalfHeight[i] != other.markerHalfHeight[i]
                || markerRotation[i] != other.markerRotation[i]
                || markerColor[i] != other.markerColor[i]
                || markerDrawers[i] != other.markerDrawers[i]
            ) return false
        }
        return true
    }

    private companion object {
        const val INITIAL_CAPACITY = 16
    }
}
