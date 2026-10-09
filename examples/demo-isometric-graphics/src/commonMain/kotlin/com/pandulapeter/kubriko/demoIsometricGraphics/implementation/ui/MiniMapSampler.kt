/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.RenderableCuboidHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidRenderer
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

// Double-buffered, allocation-free sampling: each tick writes into the back buffer and the
// buffers are swapped only when the content actually changed, so steady-state sampling neither
// allocates nor invalidates the minimap canvas. The camera offset is deliberately NOT part of the
// sample — the draw reads it live for smooth full-frame-rate scrolling. Both sample() and buffer
// reads happen on the main thread (LaunchedEffect + draw), so no synchronization is needed.
internal class MiniMapSampler {
    private var front = MiniMapBuffer()
    private var back = MiniMapBuffer()
    private val modelIdToIndex = mutableMapOf<String, Int>()
    private val modelIdHasPreferred = mutableSetOf<String>()
    val buffer: MiniMapBuffer get() = front

    // Returns true when the new sample differs from the previous one.
    fun sample(scale: Float, actors: List<*>): Boolean {
        modelIdToIndex.clear()
        modelIdHasPreferred.clear()
        val target = back
        var count = 0
        actors.forEach { actor ->
            if (actor is RenderableCuboidHolder) {
                // For models that should only draw one marker per instance (e.g. characters, trees),
                // prioritize the preferred cuboid (e.g. torso, foliage) to save performance.
                val marker = actor.miniMapMarker
                if (marker != null) {
                    val modelId = if (actor is PlanarCuboidRenderer) actor.id.removeSuffix("-${actor.cuboidId}") else actor.id.substringBeforeLast('-')
                    val existingIndex = modelIdToIndex[modelId]
                    val cuboid = actor.renderableCuboid.cuboid
                    val isPreferred = actor.isPreferredMiniMapMarker(cuboid.name)

                    if (existingIndex != null) {
                        if (isPreferred && !modelIdHasPreferred.contains(modelId)) {
                            updateTarget(target, existingIndex, actor, scale, marker)
                            modelIdHasPreferred.add(modelId)
                        }
                        return@forEach
                    }

                    target.ensureCapacity(count + 1)
                    updateTarget(target, count, actor, scale, marker)
                    modelIdToIndex[modelId] = count
                    if (isPreferred) modelIdHasPreferred.add(modelId)
                    count++
                } else {
                    target.ensureCapacity(count + 1)
                    updateTarget(target, count, actor, scale, MiniMapMarker.Rectangle)
                    count++
                }
            }
        }
        target.markerCount = count
        if (target.contentEquals(front)) return false
        back = front
        front = target
        return true
    }

    private fun updateTarget(target: MiniMapBuffer, index: Int, actor: RenderableCuboidHolder, scale: Float, marker: MiniMapMarker) {
        val renderable = actor.renderableCuboid
        val cuboid = renderable.cuboid

        // Use the model's root rotation and position to avoid animation jitter.
        val worldRotRaw = renderable.rotationZ.raw
        val localRotRaw = cuboid.rotationZ.raw
        val modelRotRaw = worldRotRaw - localRotRaw

        val cos = cos(modelRotRaw)
        val sin = sin(modelRotRaw)
        val localX = cuboid.positionX.raw
        val localY = cuboid.positionY.raw

        val worldX = renderable.positionInWorld.x.raw
        val worldY = renderable.positionInWorld.y.raw

        val modelX = worldX - (localX * cos - localY * sin)
        val modelY = worldY - (localX * sin + localY * cos)

        target.markerDrawers[index] = marker
        target.markerX[index] = quantize(modelX * scale)
        target.markerY[index] = quantize(modelY * scale)
        target.markerHalfWidth[index] = quantize((cuboid.sizeX.raw * 0.5f * scale).coerceAtLeast(MIN_MARKER_HALF_SIZE_PX))
        target.markerHalfHeight[index] = quantize((cuboid.sizeY.raw * 0.5f * scale).coerceAtLeast(MIN_MARKER_HALF_SIZE_PX))
        target.markerRotation[index] = quantize(modelRotRaw).rad.deg.raw
        target.markerColor[index] = (cuboid.colorZPlus ?: Color.Black).toArgb()
    }

    // Snaps to half-pixel steps: changes below 0.25 px don't count as new content.
    private fun quantize(value: Float) = round(value * 2f) * 0.5f

    private companion object {
        const val MIN_MARKER_HALF_SIZE_PX = 2f
    }
}
