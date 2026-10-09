/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import com.pandulapeter.kubriko.helpers.extensions.bottomRight
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The scene types wrap plain floats, so their operators have to be exactly the float operations, special values
 * included.
 */
class SceneTypesTest {

    private val values = listOf(0f, -0f, 1f, -3.5f, 1e7f, 1e-7f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

    @Test
    fun sceneUnitOperatorsMatchFloatOperations() {
        for (a in values) {
            val unitA = a.sceneUnit
            assertEquals(+a, (+unitA).raw, "+$a")
            assertEquals(-a, (-unitA).raw, "-$a")
            assertEquals(a * 3, (unitA * 3).raw, "$a * 3")
            assertEquals(a / 3, (unitA / 3).raw, "$a / 3")
            for (b in values) {
                val unitB = b.sceneUnit
                assertEquals(a + b, (unitA + unitB).raw, "$a + $b")
                assertEquals(a - b, (unitA - unitB).raw, "$a - $b")
                assertEquals(a * b, (unitA * b).raw, "$a * $b")
                assertEquals(a * b, (unitA * unitB).raw, "$a * $b (SceneUnit)")
                assertEquals(a / b, (unitA / b).raw, "$a / $b")
                assertEquals(a / b, (unitA / unitB).raw, "$a / $b (SceneUnit)")
                assertEquals(a.compareTo(b), unitA.compareTo(unitB), "$a compareTo $b")
            }
        }
    }

    @Test
    fun sceneOffsetOperatorsAreComponentWise() {
        for (a in values) {
            for (b in values) {
                val offset = offset(a, b)
                val other = offset(b, a)
                assertEquals(offset(a + b, b + a), offset + other, "sum of $offset and $other")
                assertEquals(offset(a - b, b - a), offset - other, "difference of $offset and $other")
                assertEquals(offset(-a, -b), -offset, "negation of $offset")
                assertEquals(offset(a * 2f, b * 2f), offset * 2f, "$offset * 2")
                assertEquals(offset(a / 2f, b / 2f), offset / 2f, "$offset / 2")
            }
        }
    }

    @Test
    fun sceneOffsetScalesEachComponentByItsOwnFactor() {
        val offset = offset(3f, 4f)
        val scale = Scale(2f, 0.5f)

        assertEquals(offset(6f, 2f), offset * scale)
        assertEquals(offset(1.5f, 8f), offset / scale)
    }

    @Test
    fun directionConstantsFollowTheYDownConvention() {
        assertEquals(offset(0f, 0f), SceneOffset.Zero)
        assertEquals(offset(-1f, 0f), SceneOffset.Left)
        assertEquals(offset(1f, 0f), SceneOffset.Right)
        assertEquals(offset(0f, -1f), SceneOffset.Up)
        assertEquals(offset(0f, 1f), SceneOffset.Down)
        assertEquals(offset(-1f, -1f), SceneOffset.UpLeft)
        assertEquals(offset(1f, -1f), SceneOffset.UpRight)
        assertEquals(offset(-1f, 1f), SceneOffset.DownLeft)
        assertEquals(offset(1f, 1f), SceneOffset.DownRight)
    }

    @Test
    fun directionConstructorPointsAlongTheAngle() {
        val right = SceneOffset(AngleRadians.Zero)
        val down = SceneOffset(AngleRadians.HalfPi)

        assertEquals(1f, right.x.raw, 1e-5f)
        assertEquals(0f, right.y.raw, 1e-5f)
        assertEquals(0f, down.x.raw, 1e-5f)
        assertEquals(1f, down.y.raw, 1e-5f)
    }

    @Test
    fun sceneSizeOperatorsAreComponentWise() {
        for (a in values) {
            for (b in values) {
                val size = size(a, b)
                val other = size(b, a)
                assertEquals(size(a + b, b + a), size + other, "sum of $size and $other")
                assertEquals(size(a - b, b - a), size - other, "difference of $size and $other")
            }
        }
    }

    @Test
    fun sceneSizeScalesEachDimensionByItsOwnFactor() {
        val size = size(3f, 4f)
        val scale = Scale(2f, 0.5f)

        assertEquals(size(6f, 2f), size * scale)
        assertEquals(size(1.5f, 8f), size / scale)
    }

    @Test
    fun sceneSizeCornersAndCenter() {
        val size = size(4f, 6f)

        assertEquals(offset(2f, 3f), size.center)
        assertEquals(offset(4f, 6f), size.bottomRight)
    }

    @Test
    fun scaleOperatorsAndIdentity() {
        val scale = Scale(2f, -3f)

        assertEquals(scale, scale * Scale.Unit)
        assertEquals(Scale(4f, 9f), scale * scale)
        assertEquals(Scale(3f, -2f), scale + 1f)
        assertEquals(Scale(1f, -4f), scale - 1f)
        assertEquals(Scale(4f, -6f), scale * 2f)
        assertEquals(Scale(1f, -1.5f), scale / 2f)
        assertEquals(Scale(4f, -6f), scale + scale)
        assertEquals(Scale(0f, 0f), scale - scale)
    }

    @Test
    fun normalizedDegreesFallWithinOneTurn() {
        for (degrees in listOf(-720f, -450f, -90f, -0.5f, 0f, 90f, 359f, 360f, 400f, 1080f)) {
            val normalized = degrees.deg.normalized
            assertTrue(normalized >= 0f && normalized < 360f, "$degrees° normalized to $normalized")
            assertEquals(0f, ((degrees - normalized) % 360f + 0f), 1e-3f, "$degrees° normalized to $normalized")
        }
    }

    @Test
    fun normalizedRadiansFallWithinOneTurn() {
        val twoPi = (2 * PI).toFloat()
        for (radians in listOf(-20f, -7f, -1f, 0f, 1f, 6f, 7f, 20f)) {
            val normalized = radians.rad.normalized
            assertTrue(normalized >= 0f && normalized < twoPi, "$radians normalized to $normalized")
            assertEquals(kotlin.math.sin(radians), kotlin.math.sin(normalized), 1e-4f, "$radians normalized to $normalized")
            assertEquals(kotlin.math.cos(radians), kotlin.math.cos(normalized), 1e-4f, "$radians normalized to $normalized")
        }
    }
}
