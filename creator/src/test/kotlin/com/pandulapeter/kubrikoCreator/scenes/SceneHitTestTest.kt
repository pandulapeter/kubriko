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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneHitTestTest {
    private val settings = SceneHitTest.Bounds(0, -500f, -500f, 500f, 500f, drawingOrder = Float.MAX_VALUE, isBackground = true)
    private val ball = SceneHitTest.Bounds(1, -10f, -10f, 10f, 10f)
    private val wall = SceneHitTest.Bounds(2, 0f, -50f, 100f, 50f)
    private val overlay = SceneHitTest.Bounds(3, 5f, 5f, 15f, 15f, drawingOrder = -1f)

    @Test
    fun theActorDrawnOnTopIsPicked() {
        val bounds = listOf(settings, ball, wall, overlay)
        assertEquals(1, SceneHitTest.pick(bounds, -5f, 0f))
        // Of equal drawing orders, the later one is drawn above.
        assertEquals(2, SceneHitTest.pick(bounds, 5f, 0f))
        assertEquals(3, SceneHitTest.pick(bounds, 8f, 8f))
        assertEquals(2, SceneHitTest.pick(bounds, 50f, 0f))
    }

    @Test
    fun theBackgroundIsOnlyPickedWhereNothingElseIs() {
        assertEquals(0, SceneHitTest.pick(listOf(settings, ball), 200f, 200f))
        assertEquals(1, SceneHitTest.pick(listOf(ball, settings), 0f, 0f))
        assertNull(SceneHitTest.pick(listOf(settings, ball), 600f, 0f))
        assertNull(SceneHitTest.pick(emptyList(), 0f, 0f))
    }
}
