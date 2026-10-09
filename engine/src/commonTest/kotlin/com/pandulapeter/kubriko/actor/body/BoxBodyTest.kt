/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxBodyTest {

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

    private fun BoxBody.bounds() = axisAlignedBoundingBox.let { floatArrayOf(it.left.raw, it.top.raw, it.right.raw, it.bottom.raw) }

    private fun assertBounds(left: Float, top: Float, right: Float, bottom: Float, body: BoxBody) {
        val bounds = body.bounds()
        assertEquals(left, bounds[0], 1e-4f, "left")
        assertEquals(top, bounds[1], 1e-4f, "top")
        assertEquals(right, bounds[2], 1e-4f, "right")
        assertEquals(bottom, bounds[3], 1e-4f, "bottom")
    }

    private fun newBody() = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f))

    @Test
    fun pivotDefaultsToTheCenterAndSitsAtThePosition() {
        val body = newBody()

        assertEquals(offset(2f, 3f), body.pivot)
        assertBounds(8f, 17f, 12f, 23f, body)
    }

    @Test
    fun rotationTurnsTheBoundsAroundThePivot() {
        val body = newBody()

        body.rotation = AngleRadians.HalfPi
        assertBounds(7f, 18f, 13f, 22f, body)

        body.rotation = AngleRadians.Pi
        assertBounds(8f, 17f, 12f, 23f, body)
    }

    @Test
    fun rotationFollowsTheYDownClockwiseConventionAroundAnOffCenterPivot() {
        val body = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f), initialPivot = SceneOffset.Zero)

        body.rotation = AngleRadians.HalfPi

        assertBounds(4f, 20f, 10f, 24f, body)
    }

    @Test
    fun scaleStretchesTheBoundsAwayFromThePivot() {
        val body = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f), initialPivot = SceneOffset.Zero)

        body.scale = Scale(2f, 0.5f)

        assertBounds(10f, 20f, 18f, 23f, body)
    }

    @Test
    fun negativeScaleMirrorsTheBoundsAcrossThePivot() {
        val body = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f), initialPivot = SceneOffset.Zero)

        body.scale = Scale(-1f, 1f)

        assertBounds(6f, 20f, 10f, 26f, body)
    }

    @Test
    fun initialPivotIsClampedIntoTheSize() {
        val body = BoxBody(initialSize = size(4f, 6f), initialPivot = offset(10f, -3f))

        assertEquals(offset(4f, 0f), body.pivot)
    }

    @Test
    fun assignedPivotIsClampedIntoTheSize() {
        val body = BoxBody(initialSize = size(4f, 6f))

        body.pivot = offset(-1f, 100f)

        assertEquals(offset(0f, 6f), body.pivot)
    }

    @Test
    fun shrinkingTheSizeClampsThePivot() {
        val body = BoxBody(initialSize = size(4f, 6f), initialPivot = offset(4f, 6f))

        body.size = size(2f, 3f)

        assertEquals(offset(2f, 3f), body.pivot)
    }

    @Test
    fun boundingBoxFollowsEveryPropertyChange() {
        val body = newBody()
        val changes = listOf<Pair<String, (BoxBody) -> Unit>>(
            "position" to { it.position = offset(-5f, 7f) },
            "size" to { it.size = size(9f, 1f) },
            "pivot" to { it.pivot = offset(0f, 0f) },
            "scale" to { it.scale = Scale(3f, 2f) },
            "rotation" to { it.rotation = 0.3f.rad },
        )
        for ((property, change) in changes) {
            val before = body.bounds()

            change(body)

            assertFalse(before.contentEquals(body.bounds()), "The bounding box did not follow the $property change.")
        }
    }

    @Test
    fun zeroSizedBodyCollapsesToItsPosition() {
        val body = BoxBody(initialPosition = offset(3f, 4f), initialSize = size(0f, 0f))

        assertBounds(3f, 4f, 3f, 4f, body)
    }

    @Test
    fun copyStartsEqualAndChangesIndependently() {
        val source = BoxBody(
            initialPosition = offset(1f, 2f),
            initialSize = size(3f, 4f),
            initialPivot = offset(1f, 1f),
            initialScale = Scale(2f, 3f),
            initialRotation = 0.5f.rad,
        )
        val sourceBounds = source.bounds()

        val copy = source.copyAsBoxBody()
        assertEquals(source.position, copy.position)
        assertEquals(source.size, copy.size)
        assertEquals(source.pivot, copy.pivot)
        assertEquals(source.scale, copy.scale)
        assertEquals(source.rotation, copy.rotation)
        assertTrue(sourceBounds.contentEquals(copy.bounds()))

        copy.position = offset(100f, 100f)
        assertTrue(sourceBounds.contentEquals(source.bounds()))
    }

    @Test
    fun copyAppliesTheGivenOverrides() {
        val source = newBody()

        val copy = source.copyAsBoxBody(size = size(10f, 10f), rotation = AngleRadians.Pi)

        assertEquals(source.position, copy.position)
        assertEquals(size(10f, 10f), copy.size)
        assertEquals(AngleRadians.Pi, copy.rotation)
    }
}
