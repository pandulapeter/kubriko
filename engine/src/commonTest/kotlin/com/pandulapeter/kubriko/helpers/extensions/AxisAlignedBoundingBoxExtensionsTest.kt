/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AxisAlignedBoundingBoxExtensionsTest {

    private fun box(left: Float, top: Float, right: Float, bottom: Float) = AxisAlignedBoundingBox(
        min = SceneOffset(left.sceneUnit, top.sceneUnit),
        max = SceneOffset(right.sceneUnit, bottom.sceneUnit),
    )

    private val unitBox = box(0f, 0f, 10f, 10f)

    @Test
    fun overlappingBoxesOverlapBothWays() {
        val other = box(5f, -5f, 15f, 5f)

        assertTrue(unitBox.isOverlapping(other))
        assertTrue(other.isOverlapping(unitBox))
    }

    @Test
    fun containedBoxOverlaps() {
        assertTrue(unitBox.isOverlapping(box(2f, 2f, 3f, 3f)))
        assertTrue(box(2f, 2f, 3f, 3f).isOverlapping(unitBox))
    }

    @Test
    fun boxesTouchingAtAnEdgeDoNotOverlap() {
        assertFalse(unitBox.isOverlapping(box(10f, 0f, 20f, 10f)))
        assertFalse(unitBox.isOverlapping(box(0f, -10f, 10f, 0f)))
        assertFalse(unitBox.isOverlapping(box(10f, 10f, 20f, 20f)))
    }

    @Test
    fun separatedBoxesDoNotOverlap() {
        assertFalse(unitBox.isOverlapping(box(11f, 0f, 20f, 10f)))
        assertFalse(unitBox.isOverlapping(box(0f, 11f, 10f, 20f)))
    }
}
