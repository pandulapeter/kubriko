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

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class PointBodyTest {

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    @Test
    fun boundingBoxCollapsesToThePosition() {
        val body = PointBody(initialPosition = offset(3f, -4f))

        assertEquals(offset(3f, -4f), body.axisAlignedBoundingBox.min)
        assertEquals(offset(3f, -4f), body.axisAlignedBoundingBox.max)
    }

    @Test
    fun boundingBoxFollowsThePosition() {
        val body = PointBody(initialPosition = offset(3f, -4f))
        body.axisAlignedBoundingBox

        body.position = offset(10f, 20f)

        assertEquals(offset(10f, 20f), body.axisAlignedBoundingBox.min)
        assertEquals(offset(10f, 20f), body.axisAlignedBoundingBox.max)
    }

    @Test
    fun copyChangesIndependently() {
        val source = PointBody(initialPosition = offset(1f, 2f))

        val copy = source.copyAsPointBody()
        copy.position = offset(5f, 5f)

        assertEquals(offset(1f, 2f), source.position)
        assertEquals(offset(1f, 2f), source.axisAlignedBoundingBox.min)
    }
}
