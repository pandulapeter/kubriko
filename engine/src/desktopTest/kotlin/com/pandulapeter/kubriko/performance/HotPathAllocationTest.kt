/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.performance

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.Timer
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.helpers.extensions.lerp
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.rotateAround
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.FrameTickScheduler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Holds the engine's per-frame paths to their allocation budgets. A change that trips one of these must fix the
 * allocation, not the budget.
 */
class HotPathAllocationTest {

    private class CountingPositionable(position: SceneOffset) : Actor, Dynamic, Positionable {
        override val body = PointBody(initialPosition = position)
        var updates = 0

        override fun update(deltaTimeInMilliseconds: Int) {
            updates++
        }
    }

    private class OscillatingVisible(position: SceneOffset) : Actor, Dynamic, Visible {
        override val body = BoxBody(initialPosition = position, initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        private val origin = position
        private var isShifted = false
        var boundsSum = 0f

        override fun update(deltaTimeInMilliseconds: Int) {
            isShifted = !isShifted
            body.position = if (isShifted) origin + SceneOffset.Right else origin
            boundsSum += body.axisAlignedBoundingBox.left.raw
        }

        override fun DrawScope.draw() = Unit
    }

    /** Inside the 1920×1080 viewport around the origin for even indices, far outside it for odd ones. */
    private fun position(index: Int) = if (index % 2 == 0) {
        SceneOffset(((index % 40) * 10f - 200f).sceneUnit, ((index / 40 % 40) * 10f - 200f).sceneUnit)
    } else {
        SceneOffset((100_000f + (index % 40) * 10f).sceneUnit, ((index / 40 % 40) * 10f).sceneUnit)
    }

    private fun measureTick(actorCount: Int, createActor: (SceneOffset) -> Actor): Double {
        val (kubriko, tickSource) = newTestKubriko(actorManager = ActorManager.newInstance(shouldPutFarAwayActorsToSleep = true))
        kubriko.actorManager.add(List(actorCount) { createActor(position(it)) })
        kubriko.actorManager.awaitProcessed()
        repeat(10) { tickSource.tick(16) }
        val bytesPerTick = measureAllocatedBytesPerRun { tickSource.tick(16) }
        kubriko.dispose()
        return bytesPerTick
    }

    private fun assertTickBudget(createActor: (SceneOffset) -> Actor) {
        val small = measureTick(100, createActor)
        val large = measureTick(10_000, createActor)
        assertTrue(small <= 1_024, "$small B per tick with 100 actors")
        assertTrue(large <= 1_024, "$large B per tick with 10 000 actors")
        assertTrue(abs(large - small) <= 256, "$small B vs $large B per tick: the tick allocates per actor")
    }

    @Test
    fun tickCostIsFlatInActorCount() = assertTickBudget { CountingPositionable(it) }

    @Test
    fun tickWithVisibleMovingActorsDoesNotAllocatePerActor() = assertTickBudget { OscillatingVisible(it) }

    @Test
    fun boxBodyRefreshDoesNotAllocate() {
        val body = BoxBody(initialSize = SceneSize(10f.sceneUnit, 20f.sceneUnit))
        var step = 0
        var sink = 0f
        val bytes = measureAllocatedBytesPerRun {
            step++
            body.position = SceneOffset(step.toFloat().sceneUnit, 0f.sceneUnit)
            sink += body.axisAlignedBoundingBox.right.raw
            body.rotation = (step * 0.01f).rad
            sink += body.axisAlignedBoundingBox.bottom.raw
        }
        assertTrue(bytes < 1, "$bytes B per BoxBody refresh")
        assertTrue(sink != 0f)
    }

    @Test
    fun timerUpdateDoesNotAllocate() {
        var fires = 0
        val timer = Timer(timeInMilliseconds = 100, shouldTriggerMultipleTimes = true) { fires++ }
        val bytes = measureAllocatedBytesPerRun { timer.update(16) }
        assertTrue(bytes < 1, "$bytes B per Timer update")
        assertTrue(fires > 0)
    }

    @Test
    fun frameTickSchedulerDoesNotAllocate() {
        val scheduler = FrameTickScheduler()
        val targetFrameRate = TargetFrameRate.Limit(30)
        var frameTime = 0L
        var sink = 0L
        val bytes = measureAllocatedBytesPerRun(warmUpRuns = 200, measuredRuns = 50) {
            for (i in 0 until 300) {
                frameTime += 8
                sink += scheduler.onFrame(frameTime, frameTime, canTick = true, targetFrameRate = targetFrameRate)
                sink += scheduler.sleepBeforeNextFrame(frameTime + 1, targetFrameRate)
            }
        }
        assertTrue(bytes < 1, "$bytes B per 300 FrameTickScheduler frames")
        assertTrue(sink != 0L)
    }

    @Test
    fun sceneGeometryMathDoesNotAllocate() {
        val center = SceneOffset(5f.sceneUnit, -3f.sceneUnit)
        var position = SceneOffset(1f.sceneUnit, 2f.sceneUnit)
        var sink = 0f
        val bytes = measureAllocatedBytesPerRun {
            position = (position + SceneOffset.Right * 0.5f).rotateAround(center, 0.01f.rad)
            position = lerp(position, center, 0.1f) - SceneOffset.Down / 2f
            sink += position.distanceTo(center).raw + position.normalized().x.raw + position.angleTowards(center).raw
            sink += (SceneSize(4f.sceneUnit, 2f.sceneUnit) * Scale(2f, 0.5f)).width.raw
        }
        assertTrue(bytes < 1, "$bytes B per run of scene geometry math")
        assertTrue(sink != 0f)
    }

    @Test
    fun viewportBoundsCheckDoesNotAllocate() {
        val (kubriko, _) = newTestKubriko()
        val body = BoxBody(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        val other = BoxBody(initialPosition = SceneOffset(5f.sceneUnit, 5f.sceneUnit), initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))
        var hits = 0
        val bytes = measureAllocatedBytesPerRun {
            if (body.axisAlignedBoundingBox.isWithinViewportBounds(kubriko.viewportManager)) hits++
            if (body.axisAlignedBoundingBox.isOverlapping(other.axisAlignedBoundingBox)) hits++
        }
        kubriko.dispose()
        assertTrue(bytes < 1, "$bytes B per viewport bounds check")
        assertTrue(hits > 0)
    }

    @Test
    fun triangleBatchSteadyStateDoesNotAllocate() {
        val batch = TriangleBatch()
        val bytes = measureAllocatedBytesPerRun {
            batch.reset()
            for (i in 0 until 1_000) {
                val x = i.toFloat()
                batch.addQuad(x, 0f, x + 1f, 0f, x + 1f, 1f, x, 1f, argb = -1)
            }
            for (i in 0 until 200) {
                val y = i.toFloat()
                batch.addLine(0f, y, 100f, y + 3f, halfWidth = 1f, argbA = -1, isAntiAlias = true)
            }
        }
        assertTrue(bytes < 1, "$bytes B per TriangleBatch fill")
    }
}
