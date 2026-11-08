/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RenderingModeTest {

    @Test
    fun `boxes switch between plain and batched in the same state`() {
        val plain = MovingBox(MovingBox.State(boxColor = Color.Red, isRotatingClockwise = true))

        val batched = plain.inRenderingMode(isBatched = true)

        assertIs<BatchedMovingBox>(batched)
        assertEquals(Color.Red, batched.boxColor)
        assertTrue(batched.isRotatingClockwise)
        assertSame(plain.body, batched.body)
        assertIs<MovingBox>(batched.inRenderingMode(isBatched = false))
    }

    @Test
    fun `boxes with circles keep their circle`() {
        val plain = BoxWithCircle(BoxWithCircle.State(circleColor = Color.Blue, circleRadius = 12f.sceneUnit))

        val batched = plain.inRenderingMode(isBatched = true) as BatchedBoxWithCircle

        assertEquals(Color.Blue, batched.circleColor)
        assertEquals(12f.sceneUnit, batched.circleRadius)
    }

    @Test
    fun `an actor already in the asked mode stays itself`() {
        val batched = BatchedMovingBox(MovingBox.State())

        assertSame(batched, batched.inRenderingMode(isBatched = true))
        assertTrue(batched.inRenderingMode(isBatched = false) !is Batched)
    }

    @Test
    fun `changing a batched box's appearance invalidates its look`() {
        val batched = BatchedBoxWithCircle(BoxWithCircle.State())
        val revision = batched.look.revision

        batched.circleColor = Color.Green

        assertTrue(batched.look.revision > revision)
    }
}
