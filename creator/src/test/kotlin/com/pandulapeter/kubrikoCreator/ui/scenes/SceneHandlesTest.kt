/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubrikoCreator.commands.scenes.ActorValueChanges
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsValues
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneHandlesTest {
    private val frame = ActorFrame(position = Offset(50f, 50f), size = Offset(100f, 40f), pivot = Offset(50f, 20f))
    private val metrics = HandleMetrics(size = 8f, reach = 6f, rotateDistance = 20f, strokeWidth = 1f)
    private val identity = { point: Offset -> point }

    @Test
    fun aPressTakesTheNearestHandleWithinReach() {
        assertEquals(FrameHandle.TOP_LEFT, handleAt(frame, Offset(2f, 32f), metrics, identity))
        assertEquals(FrameHandle.RIGHT, handleAt(frame, Offset(99f, 50f), metrics, identity))
        assertEquals(FrameHandle.ROTATE, handleAt(frame, Offset(50f, 11f), metrics, identity))
        assertNull(handleAt(frame, Offset(50f, 50f), metrics, identity))
    }

    @Test
    fun theRotatingHandleStandsOutAwayFromTheMiddleOfTheBox() {
        assertEquals(Offset(50f, 10f), screenPointOf(frame, FrameHandle.ROTATE, metrics, identity))
        val upsideDown = frame.copy(rotation = PI.toFloat())
        val point = screenPointOf(upsideDown, FrameHandle.ROTATE, metrics, identity)
        assertEquals(50f, point.x, absoluteTolerance = 0.01f)
        assertEquals(90f, point.y, absoluteTolerance = 0.01f)
    }

    @Test
    fun resizingSetsThePositionAndSizeAndRotatingOnlyTheRotation() {
        val resized = frame.copy(position = Offset(60f, 50f), size = Offset(120f, 40f))
        assertEquals(
            SetActorsValues.Input("main", listOf(ActorValueChanges("a", x = 60f, y = 50f, width = 120f, height = 40f))),
            transformInputOf("main", "a", FrameHandle.RIGHT, resized),
        )
        assertEquals(SetActorsValues.Input("main", listOf(ActorValueChanges("a", rotation = 90f))), transformInputOf("main", "a", FrameHandle.ROTATE, frame.copy(rotation = (PI / 2).toFloat())))
    }

    @Test
    fun everyDragOfAHandleIsAnUndoStepOfItsOwn() {
        assertNull(SetActorsValues.mergeKey(transformInputOf("main", "a", FrameHandle.RIGHT, frame)))
    }
}
