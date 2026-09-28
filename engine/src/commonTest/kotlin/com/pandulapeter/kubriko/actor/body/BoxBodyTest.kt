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
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BoxBodyTest {

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

    private fun assertClose(expected: Float, actual: Float, message: String? = null) {
        val tolerance = max(1e-4f, abs(expected) * 1e-4f)
        assertTrue(abs(expected - actual) <= tolerance, "${message.orEmpty()} expected $expected, got $actual")
    }

    /** Reads the box through the getter every time: it is one instance refreshed lazily in place. */
    private fun BoxBody.bounds() = axisAlignedBoundingBox.let { floatArrayOf(it.left.raw, it.top.raw, it.right.raw, it.bottom.raw) }

    private fun assertBounds(left: Float, top: Float, right: Float, bottom: Float, body: BoxBody) {
        val bounds = body.bounds()
        assertClose(left, bounds[0], "left")
        assertClose(top, bounds[1], "top")
        assertClose(right, bounds[2], "right")
        assertClose(bottom, bounds[3], "bottom")
    }

    private fun newBody() = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f))

    @Test
    fun unrotatedBodyIsCenteredOnItsPivot() {
        val body = newBody()
        assertEquals(offset(2f, 3f), body.pivot)
        assertBounds(8f, 17f, 12f, 23f, body)
    }

    @Test
    fun rotationAroundTheCenter() {
        val body = newBody()
        body.rotation = AngleRadians.HalfPi
        assertBounds(7f, 18f, 13f, 22f, body)
        body.rotation = AngleRadians.Pi
        assertBounds(8f, 17f, 12f, 23f, body)
    }

    @Test
    fun scaleAroundACornerPivot() {
        val body = BoxBody(initialPosition = offset(10f, 20f), initialSize = size(4f, 6f), initialPivot = SceneOffset.Zero)
        body.scale = Scale(2f, 0.5f)
        assertBounds(10f, 20f, 18f, 23f, body)
        body.scale = Scale(-1f, 1f)
        val bounds = body.bounds()
        assertTrue(bounds[0] <= bounds[2])
        assertBounds(6f, 20f, 10f, 26f, body)
    }

    @Test
    fun pivotIsClampedIntoTheSize() {
        val body = BoxBody(initialSize = size(4f, 6f), initialPivot = offset(10f, -3f))
        assertEquals(offset(4f, 0f), body.pivot)
        body.pivot = offset(-1f, 100f)
        assertEquals(offset(0f, 6f), body.pivot)
        body.pivot = offset(4f, 6f)
        body.size = size(2f, 3f)
        assertEquals(offset(2f, 3f), body.pivot)
    }

    @Test
    fun boundingBoxFollowsEveryPropertyChange() {
        val body = newBody()
        val changes = listOf<(BoxBody) -> Unit>(
            { it.position = offset(-5f, 7f) },
            { it.size = size(9f, 1f) },
            { it.pivot = offset(0f, 0f) },
            { it.scale = Scale(3f, 2f) },
            { it.rotation = 0.3f.rad },
        )
        for (change in changes) {
            val before = body.bounds()
            change(body)
            assertTrue(!before.contentEquals(body.bounds()), "The bounding box did not move.")
        }
        val before = body.bounds()
        body.position = body.position
        body.size = body.size
        body.scale = body.scale
        body.rotation = body.rotation
        assertTrue(before.contentEquals(body.bounds()))
    }

    @Test
    fun degenerateInputs() {
        val nanBody = BoxBody(initialPosition = offset(Float.NaN, 0f), initialSize = size(4f, 6f))
        assertTrue(nanBody.bounds()[0].isNaN())
        val emptyBody = BoxBody(initialPosition = offset(3f, 4f), initialSize = size(0f, 0f))
        assertBounds(3f, 4f, 3f, 4f, emptyBody)
    }

    @Test
    fun copyIsIndependent() {
        val source = BoxBody(
            initialPosition = offset(1f, 2f),
            initialSize = size(3f, 4f),
            initialPivot = offset(1f, 1f),
            initialScale = Scale(2f, 3f),
            initialRotation = 0.5f.rad,
        )
        val copy = source.copyAsBoxBody()
        assertEquals(source.position, copy.position)
        assertEquals(source.size, copy.size)
        assertEquals(source.pivot, copy.pivot)
        assertEquals(source.scale, copy.scale)
        assertEquals(source.rotation, copy.rotation)
        val sourceBounds = source.bounds()
        assertTrue(sourceBounds.contentEquals(copy.bounds()))
        copy.position = offset(100f, 100f)
        assertTrue(sourceBounds.contentEquals(source.bounds()))
        assertNotEquals(sourceBounds.toList(), copy.bounds().toList())
    }
}
