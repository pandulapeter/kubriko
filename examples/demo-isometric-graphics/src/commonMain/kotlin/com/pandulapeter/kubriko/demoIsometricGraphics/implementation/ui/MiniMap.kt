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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.collections.immutable.ImmutableList
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.GridMap
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.TopDownGridLineCache
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.drawTopDownGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

private const val SHOW_MINI_MAP = true
private const val MINI_MAP_REFRESH_MS = 64L
private const val MINI_MAP_SCALE = 0.04f

// 128 dp circular top-down minimap, drawn as a lightweight overlay instead of rendering
// `logicKubriko`'s actors, at the fixed MINI_MAP_SCALE: the camera offset is read live (full-frame-rate
// scrolling while moving, zero redraws while idle), while marker world positions are sampled by MiniMapSampler
// every MINI_MAP_REFRESH_MS. Markers tolerate the sampling lag because the main character is
// pinned to the center (camera = character position) and scenery is world-static.
@Composable
internal fun MiniMap(
    logicKubriko: Kubriko,
    logicVisibleActors: StateFlow<ImmutableList<Visible>>,
    worldRotation: StateFlow<AngleRadians>,
    cameraOffset: StateFlow<SceneOffset>,
    gridMap: GridMap?,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier.size(128.dp),
) {
    // Kept invisible but composed: this viewport sizes `logicKubriko`'s `ViewportManager` and keeps
    // `logicKubriko`'s loop running. Its scale is set by ControlOverlayManager so that
    // visibleActorsWithinViewport, the culling input for the volumetric pipeline, covers the isometric
    // view. The visible minimap is the overlay below.
    KubrikoViewport(
        modifier = Modifier
            .matchParentSize()
            .alpha(0f),
        kubriko = logicKubriko,
    )
    if (SHOW_MINI_MAP) {
        val worldRotationState = worldRotation.collectAsState()
        val cameraOffsetState = cameraOffset.collectAsState()
        val sampler = remember { MiniMapSampler() }
        val samplerVersion = remember { mutableStateOf(0) }
        val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
        val outlineColor = Color.Black
        LaunchedEffect(Unit) {
            while (true) {
                if (sampler.sample(
                        scale = MINI_MAP_SCALE,
                        actors = logicVisibleActors.value,
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
                    // The camera is read live so the minimap scrolls at full frame rate while
                    // moving; it stops changing when the character stands still, so being idle
                    // costs no redraws at all.
                    val scale = MINI_MAP_SCALE
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
