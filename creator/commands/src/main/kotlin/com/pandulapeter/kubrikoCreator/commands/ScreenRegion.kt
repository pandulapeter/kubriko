/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * A region of the game's view, as fractions of it from 0 to 1.
 */
public data class ScreenRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /**
     * The region grown by [fraction] of its own size on every side, and at least [minimumMargin] of the view, kept within
     * the view: room around the Actors a screenshot is cropped to, so they aren't cut at its edge.
     */
    public fun withMargin(fraction: Float, minimumMargin: Float): ScreenRegion {
        val horizontal = maxOf((right - left) * fraction, minimumMargin)
        val vertical = maxOf((bottom - top) * fraction, minimumMargin)
        return ScreenRegion(
            left = (left - horizontal).coerceIn(0f, 1f),
            top = (top - vertical).coerceIn(0f, 1f),
            right = (right + horizontal).coerceIn(0f, 1f),
            bottom = (bottom + vertical).coerceIn(0f, 1f),
        )
    }
}
