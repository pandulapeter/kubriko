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

import kotlin.test.Test
import kotlin.test.assertEquals

class SpriteZoomGesturesTest {
    private val state = SpriteEditorState()
    private val gestures = SpriteZoomGestures(state)

    @Test
    fun aWheelNotchIsAStepAndATrackpadsSmallScrollsAddUpToOne() {
        gestures.onZoomScroll(-1f)
        assertEquals(12, state.zoom)
        repeat(4) { gestures.onZoomScroll(-0.2f) }
        assertEquals(12, state.zoom, "four fifths of a notch aren't a step yet")
        gestures.onZoomScroll(-0.2f)
        assertEquals(16, state.zoom)
        gestures.onZoomScroll(2f)
        assertEquals(8, state.zoom)
    }

    @Test
    fun aPinchZoomsOnlyOverTheCanvasAndASlowOneAsFarAsAQuickOne() {
        gestures.onPinch(1.0)
        assertEquals(8, state.zoom)
        gestures.isPointerOver = true
        repeat(10) { gestures.onPinch(0.03) }
        assertEquals(16, state.zoom)
        gestures.onPinch(-0.3)
        assertEquals(8, state.zoom)
    }
}
