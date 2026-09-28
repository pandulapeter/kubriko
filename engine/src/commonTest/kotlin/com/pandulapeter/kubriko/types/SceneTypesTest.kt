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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.helpers.extensions.bottomRight
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SceneTypesTest {

    private val values = listOf(0f, -0f, 1f, -3.5f, 1e7f, 1e-7f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

    private fun assertClose(expected: Float, actual: Float, message: String? = null) {
        val tolerance = max(1e-4f, abs(expected) * 1e-4f)
        assertTrue(abs(expected - actual) <= tolerance, "${message.orEmpty()} expected $expected, got $actual")
    }

    private fun assertSameBits(expected: Float, actual: Float, message: String) =
        assertEquals(expected.toRawBits(), actual.toRawBits(), "$message: expected $expected, got $actual")

    @Test
    fun sceneUnitOperatorsMatchFloatOperations() {
        for (a in values) {
            val unitA = a.sceneUnit
            assertSameBits(+a, (+unitA).raw, "+$a")
            assertSameBits(-a, (-unitA).raw, "-$a")
            assertSameBits(a * 3, (unitA * 3).raw, "$a * 3")
            assertSameBits(a / 3, (unitA / 3).raw, "$a / 3")
            for (b in values) {
                val unitB = b.sceneUnit
                assertSameBits(a + b, (unitA + unitB).raw, "$a + $b")
                assertSameBits(a - b, (unitA - unitB).raw, "$a - $b")
                assertSameBits(a * b, (unitA * b).raw, "$a * $b")
                assertSameBits(a * b, (unitA * unitB).raw, "$a * $b (SceneUnit)")
                assertSameBits(a / b, (unitA / b).raw, "$a / $b")
                assertSameBits(a / b, (unitA / unitB).raw, "$a / $b (SceneUnit)")
                assertEquals(a.compareTo(b), unitA.compareTo(unitB), "$a compareTo $b")
            }
        }
    }

    @Test
    fun sceneOffsetOperatorsAreComponentWise() {
        for (a in values) {
            for (b in values) {
                val offset = SceneOffset(a.sceneUnit, b.sceneUnit)
                val other = SceneOffset(b.sceneUnit, a.sceneUnit)
                val sum = offset + other
                assertSameBits(a + b, sum.x.raw, "x of sum")
                assertSameBits(b + a, sum.y.raw, "y of sum")
                val difference = offset - other
                assertSameBits(a - b, difference.x.raw, "x of difference")
                assertSameBits(b - a, difference.y.raw, "y of difference")
                val negated = -offset
                assertSameBits(-a, negated.x.raw, "x of negation")
                assertSameBits(-b, negated.y.raw, "y of negation")
                val multiplied = offset * 2f
                assertSameBits(a * 2f, multiplied.x.raw, "x of product")
                assertSameBits(b * 2f, multiplied.y.raw, "y of product")
                val divided = offset / 2f
                assertSameBits(a / 2f, divided.x.raw, "x of quotient")
                assertSameBits(b / 2f, divided.y.raw, "y of quotient")
            }
        }
    }

    @Test
    fun sceneOffsetEqualityIsBitwise() {
        assertNotEquals(SceneOffset.Zero, SceneOffset(0f.sceneUnit, (-0f).sceneUnit))
        assertEquals(SceneOffset(1f.sceneUnit, 2f.sceneUnit), SceneOffset(1f.sceneUnit, 2f.sceneUnit))
        val nanOffset = SceneOffset(Float.NaN.sceneUnit, 1f.sceneUnit)
        assertTrue(nanOffset.x.raw.isNaN())
    }

    @Test
    fun sceneOffsetScalesComponentsByTheirOwnFactor() {
        val offset = SceneOffset(3f.sceneUnit, 4f.sceneUnit)
        val scale = Scale(2f, 0.5f)
        assertEquals(SceneOffset(6f.sceneUnit, 2f.sceneUnit), offset * scale)
        assertEquals(SceneOffset(1.5f.sceneUnit, 8f.sceneUnit), offset / scale)
    }

    @Test
    fun directionConstantsFollowTheYDownConvention() {
        fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
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
    fun sceneSizeOperatorsAreComponentWise() {
        for (a in values) {
            for (b in values) {
                val size = SceneSize(a.sceneUnit, b.sceneUnit)
                val other = SceneSize(b.sceneUnit, a.sceneUnit)
                assertSameBits(a + b, (size + other).width.raw, "width of sum")
                assertSameBits(b + a, (size + other).height.raw, "height of sum")
                assertSameBits(a - b, (size - other).width.raw, "width of difference")
                assertSameBits(b - a, (size - other).height.raw, "height of difference")
            }
        }
        val size = SceneSize(4f.sceneUnit, 6f.sceneUnit)
        assertEquals(SceneOffset(2f.sceneUnit, 3f.sceneUnit), size.center)
        assertEquals(SceneOffset(4f.sceneUnit, 6f.sceneUnit), size.bottomRight)
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
    fun angleConversionsRoundTripWithoutWrapping() {
        for (degrees in listOf(0f, 90f, -90f, 180f, 359f, 720f, -720f)) {
            val radians = degrees.deg.rad
            assertClose((degrees * PI / 180).toFloat(), radians.raw, "rad of $degrees°")
            assertClose(degrees, radians.deg.raw, "round trip of $degrees°")
        }
    }

    @Test
    fun sineAndCosineMatchKotlinMath() {
        for (degrees in listOf(0f, 90f, -90f, 180f, 359f, 720f, -720f)) {
            val radians = (degrees * PI / 180).toFloat()
            assertClose(kotlin.math.sin(radians), degrees.deg.sin, "sin of $degrees°")
            assertClose(kotlin.math.cos(radians), degrees.deg.cos, "cos of $degrees°")
            assertClose(kotlin.math.sin(radians), radians.rad.sin, "sin of $radians rad")
            assertClose(kotlin.math.cos(radians), radians.rad.cos, "cos of $radians rad")
        }
    }

    @Test
    fun wrappedComposeTypesKeepTheirValues() {
        assertEquals(Offset(1f, 2f), SceneOffset(1f.sceneUnit, 2f.sceneUnit).raw)
        assertEquals(Size(3f, 4f), SceneSize(3f.sceneUnit, 4f.sceneUnit).raw)
    }
}
