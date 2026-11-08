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

import com.pandulapeter.kubriko.actor.looks.LookGeometry
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.TriangleBatch

/** Where [BatchedLayerDrawer] sends what it draws, so the run logic can be tested without a canvas. */
internal interface BatchedLayerTarget {

    /** Draws a plain [Visible] the way the layers always have. */
    fun drawPlain(visible: Visible)

    /** Draws one run of batched actors. */
    fun drawRun(batch: TriangleBatch)

    /** Draws a sprite on its own, on devices that cannot sample a batch's texture per vertex. */
    fun drawSpriteIndividually(actor: Batched, look: SpriteLook)
}

/**
 * Draws one layer's sorted visible actors: plain ones one by one, and every stretch of consecutive [Batched] ones as
 * runs, each run one call. Within a stretch, actors of equal drawing order (which the sort leaves unordered) are
 * grouped by texture, each group in the order its first member appears, so the grouping is the same on every frame
 * and runs stay replayable. A run ends where its texture changes and where it would outgrow
 * [TriangleBatch.MAX_INDEXED_VERTICES]. Every buffer is reused, so a steady frame allocates nothing. One per layer,
 * used from the drawing thread.
 */
internal class BatchedLayerDrawer {
    private val stretch = ArrayList<Batched>()
    private var isEmitted = BooleanArray(64)
    private val runs = ArrayList<BatchedRun>()
    private var runCount = 0
    private var currentRun: BatchedRun? = null
    private var pixelsPerUnit = 1f
    private var isTextureSampledPerVertex = true

    /**
     * Draws [visibles] into [target]. [pixelsPerUnit] is the screen pixels per scene unit, and
     * [isTextureSampledPerVertex] whether sprites may be drawn as geometry at all.
     */
    fun draw(visibles: List<Visible>, target: BatchedLayerTarget, pixelsPerUnit: Float, isTextureSampledPerVertex: Boolean) {
        runCount = 0
        this.pixelsPerUnit = pixelsPerUnit
        this.isTextureSampledPerVertex = isTextureSampledPerVertex
        for (i in visibles.indices) {
            val visible = visibles[i]
            if (!visible.isVisible) continue
            if (visible is Batched) {
                val look = visible.look
                look.geometry.update(look, visible.body, pixelsPerUnit, isTextureSampledPerVertex)
                stretch.add(visible)
            } else {
                drawStretch(target)
                target.drawPlain(visible)
            }
        }
        drawStretch(target)
        for (i in runCount until runs.size) runs[i].clear()
    }

    private fun drawStretch(target: BatchedLayerTarget) {
        val size = stretch.size
        if (size == 0) return
        if (isEmitted.size < size) isEmitted = BooleanArray(maxOf(size, isEmitted.size * 2))
        isEmitted.fill(false, 0, size)
        var start = 0
        while (start < size) {
            val drawingOrder = stretch[start].drawingOrder + 0f
            var end = start + 1
            while (end < size && stretch[end].drawingOrder + 0f == drawingOrder) end++
            drawSegment(start, end, target)
            start = end
        }
        endRun(target)
        stretch.clear()
    }

    /** Draws the actors of one drawing order, grouped by texture in the order each group first appears. */
    private fun drawSegment(start: Int, end: Int, target: BatchedLayerTarget) {
        for (i in start until end) {
            if (isEmitted[i]) continue
            val groupGeometry = stretch[i].look.geometry
            for (j in i until end) {
                if (!isEmitted[j] && isSameGroup(groupGeometry, stretch[j].look.geometry)) {
                    drawActor(stretch[j], target)
                    isEmitted[j] = true
                }
            }
        }
    }

    private fun isSameGroup(first: LookGeometry, second: LookGeometry) =
        first.isDrawnIndividually == second.isDrawnIndividually && first.canShareRunWith(second)

    private fun drawActor(actor: Batched, target: BatchedLayerTarget) {
        val look = actor.look
        val geometry = look.geometry
        // Updated again right before it is appended: a look shared between actors holds the last one's geometry
        // otherwise. For a look of its own this only compares revisions.
        geometry.update(look, actor.body, pixelsPerUnit, isTextureSampledPerVertex)
        if (geometry.isDrawnIndividually) {
            endRun(target)
            target.drawSpriteIndividually(actor, look as SpriteLook)
            return
        }
        val vertexCount = geometry.batch.vertexCount
        if (vertexCount == 0) return
        val run = currentRun
        if (run == null || !run.canHold(geometry) || run.vertexCount + vertexCount > TriangleBatch.MAX_INDEXED_VERTICES) {
            endRun(target)
            beginRun(geometry).append(actor, geometry)
        } else {
            run.append(actor, geometry)
        }
    }

    private fun beginRun(geometry: LookGeometry): BatchedRun {
        if (runCount == runs.size) runs.add(BatchedRun())
        val run = runs[runCount]
        run.begin(geometry)
        currentRun = run
        return run
    }

    private fun endRun(target: BatchedLayerTarget) {
        val run = currentRun ?: return
        target.drawRun(run.end())
        runCount++
        currentRun = null
    }
}
