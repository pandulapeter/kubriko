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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.RenderableCuboidHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.GridMap
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.TopDownGridLineCache
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.drawTopDownGrid
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val SHOW_MINI_MAP = true
private const val MINI_MAP_REFRESH_MS = 64L

// 128 dp circular top-down minimap, drawn as a lightweight overlay instead of rendering
// stateHolder.logicKubriko's actors: camera offset and scale are read live (full-frame-rate scrolling while
// moving, zero redraws while idle), while marker world positions are sampled by MiniMapSampler
// every MINI_MAP_REFRESH_MS. Markers tolerate the sampling lag because the main character is
// pinned to the center (camera = character position) and scenery is world-static.
@Composable
internal fun MiniMap(
    stateHolder: IsometricGraphicsDemoStateHolderImpl,
    gridMap: GridMap?,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.size(128.dp),
) {
    // Kept invisible but composed: this viewport sizes stateHolder.logicViewportManager and keeps
    // stateHolder.logicKubriko's loop running — its visibleActorsWithinViewport flow is the culling
    // input for the volumetric pipeline. The visible minimap is the overlay below.
    KubrikoViewport(
        modifier = Modifier
            .matchParentSize()
            .alpha(0f),
        kubriko = stateHolder.logicKubriko,
    )
    if (SHOW_MINI_MAP) {
        val worldRotationState = stateHolder.volumetricRenderManager.worldRotation.collectAsState()
        val cameraOffsetState = stateHolder.controlManager.cameraOffset.collectAsState()
        val scaleFactorState = stateHolder.logicViewportManager.scaleFactor.collectAsState()
        val sampler = remember { MiniMapSampler() }
        val samplerVersion = remember { mutableStateOf(0) }
        val surfaceColor = MaterialTheme.colorScheme.surface
        val outlineColor = Color.Black
        LaunchedEffect(Unit) {
            val actorManager = stateHolder.logicKubriko.get<ActorManager>()
            while (true) {
                val scale = stateHolder.logicViewportManager.scaleFactor.value.horizontal
                if (scale > 0f && sampler.sample(
                        scale = scale,
                        actors = actorManager.visibleActorsWithinViewport.value,
                    )
                ) {
                    samplerVersion.value++
                }
                delay(MINI_MAP_REFRESH_MS.milliseconds)
            }
        }
        val topDownPath = remember { Path() }
        val topDownLineCache = remember { TopDownGridLineCache() }
        // Square caps close grid corners like Round did, without an arc tessellation per segment.
        val topDownStroke = remember { Stroke(width = PlanarCuboidRenderer.STROKE_WIDTH, cap = StrokeCap.Square) }
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawCircle(color = surfaceColor)
                    drawContent()
                    drawCircle(
                        color = outlineColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                .clip(CircleShape)
                .graphicsLayer { rotationZ = worldRotationState.value.deg.normalized + 45f }
                .drawBehind {
                    samplerVersion.value // Establishes the invalidation dependency on new samples.
                    // Camera and scale are read live so the minimap scrolls at full frame
                    // rate while moving; both stop changing when the character stands
                    // still, so being idle costs no redraws at all.
                    val scale = scaleFactorState.value.horizontal
                    if (scale <= 0f) return@drawBehind
                    val cameraOffset = cameraOffsetState.value
                    drawTopDownGrid(
                        gridLinesPath = topDownPath,
                        gridColor = Color.Black,
                        cellSize = 100.sceneUnit,
                        cameraPosition = cameraOffset,
                        multiplier = scale,
                        gridMap = gridMap,
                        gridStroke = topDownStroke,
                        lineCache = topDownLineCache,
                    )
                    val camX = cameraOffset.x.raw * scale
                    val camY = cameraOffset.y.raw * scale
                    val viewCenterX = size.width / 2f
                    val viewCenterY = size.height / 2f
                    val buffer = sampler.buffer
                    for (i in 0 until buffer.markerCount) {
                        val centerX = buffer.markerX[i] - camX + viewCenterX
                        val centerY = buffer.markerY[i] - camY + viewCenterY

                        // Circular culling: skip markers that are completely outside the minimap's visible area.
                        val dx = centerX - viewCenterX
                        val dy = centerY - viewCenterY
                        val halfWidth = buffer.markerHalfWidth[i]
                        val halfHeight = buffer.markerHalfHeight[i]
                        val maxMarkerHalfSize = if (halfWidth > halfHeight) halfWidth else halfHeight
                        val combinedRadius = viewCenterX + maxMarkerHalfSize
                        if (dx * dx + dy * dy > combinedRadius * combinedRadius) continue

                        buffer.markerDrawers[i]?.apply {
                            draw(
                                centerX = centerX,
                                centerY = centerY,
                                halfWidth = halfWidth,
                                halfHeight = halfHeight,
                                rotation = buffer.markerRotation[i],
                                color = Color(buffer.markerColor[i]),
                                stroke = topDownStroke,
                            )
                        }
                    }
                },
        )
    }
}

// Reusable sample storage for the minimap overlay. Marker positions and footprint half-sizes are
// stored in screen-scaled pixels (world * viewport scale), quantized to half-pixel steps so the
// sub-pixel wiggle of idle animations doesn't register as a content change. Colors are stored as
// ARGB ints so writing them doesn't box Color values; arrays only ever grow.
private class MiniMapBuffer {
    var markerCount = 0
    var markerX = FloatArray(INITIAL_CAPACITY)
    var markerY = FloatArray(INITIAL_CAPACITY)
    var markerHalfWidth = FloatArray(INITIAL_CAPACITY)
    var markerHalfHeight = FloatArray(INITIAL_CAPACITY)
    var markerRotation = FloatArray(INITIAL_CAPACITY)
    var markerColor = IntArray(INITIAL_CAPACITY)
    var markerDrawers = arrayOfNulls<MiniMapMarker>(INITIAL_CAPACITY)

    fun ensureCapacity(capacity: Int) {
        if (markerX.size < capacity) {
            val newSize = maxOf(capacity, markerX.size * 2)
            markerX = markerX.copyOf(newSize)
            markerY = markerY.copyOf(newSize)
            markerHalfWidth = markerHalfWidth.copyOf(newSize)
            markerHalfHeight = markerHalfHeight.copyOf(newSize)
            markerRotation = markerRotation.copyOf(newSize)
            markerColor = markerColor.copyOf(newSize)
            markerDrawers = markerDrawers.copyOf(newSize)
        }
    }

    fun contentEquals(other: MiniMapBuffer): Boolean {
        if (markerCount != other.markerCount) return false
        for (i in 0 until markerCount) {
            if (markerX[i] != other.markerX[i] || markerY[i] != other.markerY[i]
                || markerHalfWidth[i] != other.markerHalfWidth[i]
                || markerHalfHeight[i] != other.markerHalfHeight[i]
                || markerRotation[i] != other.markerRotation[i]
                || markerColor[i] != other.markerColor[i]
                || markerDrawers[i] != other.markerDrawers[i]
            ) return false
        }
        return true
    }

    private companion object {
        const val INITIAL_CAPACITY = 16
    }
}

// Double-buffered, allocation-free sampling: each tick writes into the back buffer and the
// buffers are swapped only when the content actually changed, so steady-state sampling neither
// allocates nor invalidates the minimap canvas. The camera offset is deliberately NOT part of the
// sample — the draw reads it live for smooth full-frame-rate scrolling. Both sample() and buffer
// reads happen on the main thread (LaunchedEffect + draw), so no synchronization is needed.
private class MiniMapSampler {
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
