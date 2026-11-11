/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import kotlin.math.abs

/**
 * Turns the canvas's zooming gestures into [SpriteEditorState]'s zoom steps: a mouse wheel's notch is a step, while a
 * trackpad's scrolls and pinches come in many small movements, which add up to one, so a slow pinch zooms as far as a
 * quick one. A pinch counts only while the pointer is over the canvas ([isPointerOver]), since the window reports every
 * pinch over it.
 */
internal class SpriteZoomGestures(private val state: SpriteEditorState) {
    var isPointerOver: Boolean = false
    private var scrolled = 0f
    private var pinched = 0.0

    /**
     * A scroll with Ctrl or Cmd held, [delta] rows down (a wheel's notch is 1, a trackpad's movements a part of one).
     */
    fun onZoomScroll(delta: Float) {
        scrolled += delta
        while (abs(scrolled) >= SCROLL_PER_STEP) {
            if (scrolled < 0) state.zoomIn() else state.zoomOut()
            scrolled -= if (scrolled < 0) -SCROLL_PER_STEP else SCROLL_PER_STEP
        }
    }

    /**
     * A trackpad's pinch, [magnification] above 0 to zoom in.
     */
    fun onPinch(magnification: Double) {
        if (!isPointerOver) return
        pinched += magnification
        while (abs(pinched) >= PINCH_PER_STEP) {
            if (pinched > 0) state.zoomIn() else state.zoomOut()
            pinched -= if (pinched > 0) PINCH_PER_STEP else -PINCH_PER_STEP
        }
    }

    private companion object {
        const val SCROLL_PER_STEP = 1f
        const val PINCH_PER_STEP = 0.15
    }
}
