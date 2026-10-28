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
 * Where the camera goes to show a whole Scene.
 *
 * @param scaleFactor How many pixels a scene unit takes.
 */
internal data class SceneFraming(
    val centerX: Float,
    val centerY: Float,
    val scaleFactor: Float,
) {
    companion object {

        /**
         * Shows the background's bounds (the Scene settings'), or without one, every Actor's, with a margin around
         * them, in a viewport of [width] by [height] pixels. Null when there is nothing to show or no viewport yet.
         */
        fun fit(bounds: List<SceneHitTest.Bounds>, width: Float, height: Float): SceneFraming? {
            val shown = bounds.filter { it.isBackground }.ifEmpty { bounds }
            if (shown.isEmpty() || width <= 0f || height <= 0f) return null
            val left = shown.minOf { it.left }
            val top = shown.minOf { it.top }
            val right = shown.maxOf { it.right }
            val bottom = shown.maxOf { it.bottom }
            val contentWidth = (right - left).coerceAtLeast(MINIMUM_SIZE)
            val contentHeight = (bottom - top).coerceAtLeast(MINIMUM_SIZE)
            return SceneFraming(
                centerX = (left + right) / 2,
                centerY = (top + bottom) / 2,
                scaleFactor = minOf(width / contentWidth, height / contentHeight) * FILL,
            )
        }

        private const val MINIMUM_SIZE = 1f
        private const val FILL = 0.9f
    }
}
