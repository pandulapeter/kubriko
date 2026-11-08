/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.actor.looks.EllipseLook
import com.pandulapeter.kubriko.actor.looks.RectangleLook
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertTrue

/** Holds a frame of batched actors, moving and still, to zero allocations once the buffers have grown. */
class BatchedLayerDrawerAllocationTest {

    private val textures = List(3) { ImageBitmap(8, 8) }
    private val actors = List(200) { index ->
        TestBatched(
            look = when (index % 4) {
                0 -> RectangleLook(Color.Red)
                1 -> EllipseLook(Color.Green)
                2 -> SpriteLook(textures[index % 3])
                else -> QuadsLook(3)
            },
            drawingOrder = (index % 5).toFloat(),
        )
    }
    private val scene = actors.take(100) + TestPlain("middle") + actors.drop(100)
    private val drawer = BatchedLayerDrawer()

    private fun measureFrames(isTextureSampledPerVertex: Boolean): Double {
        var frame = 0
        return measureAllocatedBytesPerRun(warmUpRuns = 3_000, measuredRuns = 500) {
            frame++
            for (i in actors.indices step 2) {
                actors[i].body.position = SceneOffset((frame % 7).toFloat().sceneUnit, i.toFloat().sceneUnit)
                actors[i].body.rotation = (frame * 0.01f).rad
            }
            drawer.draw(scene, SilentLayerTarget, pixelsPerUnit = if (frame % 2 == 0) 1f else 2f, isTextureSampledPerVertex)
        }
    }

    @Test
    fun `a steady frame of batched actors allocates nothing`() {
        val bytes = measureFrames(isTextureSampledPerVertex = true)

        assertTrue(bytes < 1, "$bytes B per frame of batched actors")
        assertTrue(SilentLayerTarget.runCount > 0)
    }

    @Test
    fun `a steady frame drawing sprites one by one allocates nothing`() {
        val bytes = measureFrames(isTextureSampledPerVertex = false)

        assertTrue(bytes < 1, "$bytes B per frame of batched actors with sprites drawn one by one")
    }
}
