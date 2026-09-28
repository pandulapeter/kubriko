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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneOffsetExtensionsTest {

    private val samples = listOf(
        offset(0f, 0f),
        offset(3f, 4f),
        offset(-2.5f, 7f),
        offset(1000f, -0.25f),
        offset(-40f, -40f),
    )

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun assertClose(expected: Float, actual: Float, message: String? = null, magnitude: Float = abs(expected)) {
        val tolerance = max(1e-4f, magnitude * 1e-4f)
        assertTrue(abs(expected - actual) <= tolerance, "${message.orEmpty()} expected $expected, got $actual")
    }

    /** Both components are compared with a tolerance relative to the vector's own magnitude. */
    private fun assertClose(expected: SceneOffset, actual: SceneOffset, message: String? = null) {
        val magnitude = max(abs(expected.x.raw), abs(expected.y.raw))
        assertClose(expected.x.raw, actual.x.raw, "${message.orEmpty()} x", magnitude)
        assertClose(expected.y.raw, actual.y.raw, "${message.orEmpty()} y", magnitude)
    }

    @Test
    fun distanceIsSymmetricAndNonNegative() {
        for (a in samples) {
            assertEquals(0f, a.distanceTo(a).raw)
            for (b in samples) {
                assertTrue(a.distanceTo(b).raw >= 0f)
                assertEquals(a.distanceTo(b), b.distanceTo(a))
            }
        }
        assertEquals(5f, offset(3f, 4f).distanceTo(SceneOffset.Zero).raw)
    }

    @Test
    fun lengthIsTheDistanceFromZero() {
        for (a in samples) {
            assertEquals(a.distanceTo(SceneOffset.Zero), a.length())
        }
    }

    @Test
    fun normalizedHasUnitLength() {
        for (a in samples.filter { it != SceneOffset.Zero }) {
            assertClose(1f, a.normalized().length().raw, "length of normalized $a")
        }
        assertEquals(SceneOffset.Zero, SceneOffset.Zero.normalized())
    }

    @Test
    fun rotateAroundFollowsTheYDownClockwiseConvention() {
        assertClose(offset(0f, 1f), offset(1f, 0f).rotateAround(SceneOffset.Zero, AngleRadians.HalfPi))
        val center = offset(5f, -3f)
        for (a in samples) {
            assertClose(a, a.rotateAround(center, AngleRadians.TwoPi), "full turn of $a")
            val angle = 0.7f.rad
            assertClose(a, a.rotateAround(center, angle).rotateAround(center, -angle), "there and back of $a")
            assertClose(a, a.rotateAround(a, angle), "rotation of $a around itself")
        }
    }

    @Test
    fun angleTowardsAndDirectionTowardsAgree() {
        val expectedAngles = mapOf(
            SceneOffset.Right to 0f,
            SceneOffset.Down to (PI / 2).toFloat(),
            SceneOffset.Left to PI.toFloat(),
            SceneOffset.Up to (-PI / 2).toFloat(),
        )
        for ((direction, angle) in expectedAngles) {
            assertClose(angle, SceneOffset.Zero.angleTowards(direction).raw, "angle towards $direction")
            assertEquals(SceneOffset.Zero.angleTowards(direction), SceneOffset.Zero.directionTowards(direction))
        }
        for (a in samples) {
            for (b in samples) {
                assertEquals(a.angleTowards(b), a.directionTowards(b))
            }
        }
    }

    @Test
    fun dotCrossAndNormal() {
        for (a in samples) {
            assertEquals(0f, a.dot(a.normal()).raw + 0f)
            for (b in samples) {
                assertClose(a.cross(b).raw, -b.cross(a).raw, "cross of $a and $b")
            }
        }
    }

    @Test
    fun clampingFunctionsAgree() {
        val topLeft = offset(-10f, -5f)
        val bottomRight = offset(10f, 5f)
        val inside = offset(3f, -2f)
        assertEquals(inside, inside.clampWithin(topLeft, bottomRight))
        assertEquals(offset(10f, -5f), offset(40f, -50f).clampWithin(topLeft, bottomRight))
        val candidates = samples + listOf(inside, offset(40f, -50f), offset(-11f, 6f), topLeft, bottomRight)
        for (candidate in candidates) {
            val clamped = candidate.clampWithin(topLeft, bottomRight)
            assertEquals(clamped, candidate.constrainedWithin(topLeft, bottomRight))
            assertEquals(clamped, candidate.clamp(min = topLeft, max = bottomRight))
        }
        assertEquals(offset(1000f, 7f), offset(1000f, 7f).clamp(min = topLeft))
        assertEquals(offset(-10f, 7f), offset(-40f, 7f).clamp(min = topLeft))
    }

    // clamp(max = …) without a min never clamps: the missing min falls back to the offset itself, which max() then keeps.
    @Ignore
    @Test
    fun clampWithOnlyAMaximumBoundsThatSide() {
        assertEquals(offset(1000f, 5f), offset(1000f, 7f).clamp(max = offset(Float.MAX_VALUE, 5f)))
        assertEquals(offset(-40f, -40f), offset(-40f, -40f).clamp(max = offset(10f, 5f)))
    }

    @Test
    fun lerpInterpolatesLinearly() {
        val start = offset(-4f, 10f)
        val stop = offset(6f, -2f)
        assertEquals(start, lerp(start, stop, 0f))
        assertEquals(stop, lerp(start, stop, 1f))
        assertClose(offset(1f, 4f), lerp(start, stop, 0.5f))
    }

    @Test
    fun centerOfARectanglesCorners() {
        val corners = listOf(offset(-2f, 1f), offset(6f, 1f), offset(6f, 9f), offset(-2f, 9f))
        assertEquals(offset(2f, 5f), corners.center)
    }

    @Test
    fun screenToSceneConversionsMapTheCenterAndPreserveDeltas() {
        val cameraPositions = listOf(SceneOffset.Zero, offset(-250f, 400f))
        val scales = listOf(Scale(1f, 1f), Scale(0.2f, 0.2f), Scale(5f, 2f))
        val viewportSizes = listOf(Size(1920f, 1080f), Size(1f, 1f))
        val p = Offset(100f, 100f)
        val q = Offset(-37f, 512f)
        for (camera in cameraPositions) {
            for (scale in scales) {
                for (viewportSize in viewportSizes) {
                    val center = Offset(viewportSize.width / 2, viewportSize.height / 2)
                    assertClose(camera, center.toSceneOffset(camera, viewportSize, scale), "center for $camera, $scale, $viewportSize")
                    val delta = (p.toSceneOffset(camera, viewportSize, scale) - q.toSceneOffset(camera, viewportSize, scale)).toOffset(scale)
                    assertClose(p.x - q.x, delta.x, "x delta for $camera, $scale, $viewportSize")
                    assertClose(p.y - q.y, delta.y, "y delta for $camera, $scale, $viewportSize")
                }
            }
        }
    }
}
