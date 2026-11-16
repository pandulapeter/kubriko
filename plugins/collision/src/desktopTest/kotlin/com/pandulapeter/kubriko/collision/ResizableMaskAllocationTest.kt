/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.extensions.hasCollisionWith
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ResizableMaskAllocationTest {

    @Test
    fun resizingABoxEveryFrameAndCheckingItAllocatesNothing() {
        val mask = BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        val other = BoxCollisionMask(initialPosition = SceneOffset(12f.sceneUnit, 0f.sceneUnit), initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        var frame = 0
        var hits = 0

        val allocated = measureAllocatedBytesPerRun {
            frame++
            mask.size = SceneSize((10f + frame % 30).sceneUnit, (10f + frame % 7).sceneUnit)
            if (mask.hasCollisionWith(other)) hits++
            mask.axisAlignedBoundingBox
        }

        assertEquals(0.0, allocated)
        assert(hits > 0)
    }

    @Test
    fun resizingAnEllipseEveryFrameAndCheckingItAllocatesNothing() {
        val mask = EllipseCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        val other = BoxCollisionMask(initialPosition = SceneOffset(12f.sceneUnit, 0f.sceneUnit), initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        var frame = 0
        var hits = 0

        val allocated = measureAllocatedBytesPerRun {
            frame++
            mask.size = SceneSize((10f + frame % 30).sceneUnit, (10f + frame % 7).sceneUnit)
            if (mask.hasCollisionWith(other)) hits++
            mask.axisAlignedBoundingBox
        }

        assertEquals(0.0, allocated)
        assert(hits > 0)
    }

    @Test
    fun anEllipseMadeWithoutAreaGrowsWithoutNewArrays() {
        val mask = EllipseCollisionMask()

        val allocated = measureAllocatedBytesPerRun(warmUpRuns = 0, measuredRuns = 1) {
            mask.size = SceneSize(10f.sceneUnit, 20f.sceneUnit)
        }

        assert(allocated < 128) { "Growing allocated $allocated bytes, as four new arrays of 16 would" }
    }
}
