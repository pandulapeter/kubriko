/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.SceneOffset

@Composable
internal fun MiniMap(
    miniMapSize: Dp,
    dotRadius: Dp,
    gameTime: Long,
    visibleActorColor: Color,
    invisibleActorColor: Color,
    getViewportTopLeft: () -> SceneOffset,
    getViewportBottomRight: () -> SceneOffset,
    getAllVisibleActors: () -> List<Visible>,
    getAllVisibleActorsWithinViewport: () -> List<Visible>,
) {
    val sizeInPixels = miniMapSize.px
    val dotRadiusInPixels = dotRadius.px
    Canvas(
        modifier = Modifier.size(miniMapSize),
        onDraw = {
            withTransform(
                transformBlock = {
                    translate(
                        left = sizeInPixels / 2,
                        top = sizeInPixels / 2,
                    )
                },
                drawBlock = {
                    val allVisibleActors = getAllVisibleActors()
                    val visibleActorsWithinViewport = getAllVisibleActorsWithinViewport()
                    val size = Size(dotRadiusInPixels, dotRadiusInPixels)
                    val offset = Offset(dotRadiusInPixels, dotRadiusInPixels) / 2f
                    @Suppress("UNUSED_EXPRESSION") gameTime  // This line invalidates the Canvas, causing a refresh on every frame
                    allVisibleActors
                        .map { it.body.position.raw / SCALE_FACTOR * density to visibleActorsWithinViewport.contains(it) }
                        .forEach { (position, isVisible) ->
                            drawRect(
                                color = if (isVisible) visibleActorColor else invisibleActorColor,
                                size = size,
                                topLeft = position - offset,
                            )
                        }
                    val topLeft = getViewportTopLeft().raw / SCALE_FACTOR * density
                    val bottomRight = getViewportBottomRight().raw / SCALE_FACTOR * density
                    drawRect(
                        color = Color.Red,
                        size = Size(
                            width = bottomRight.x - topLeft.x,
                            height = bottomRight.y - topLeft.y,
                        ),
                        topLeft = topLeft,
                        style = Stroke(),
                    )
                }
            )
        }
    )
}

private const val SCALE_FACTOR = 75f

private inline val Dp.px: Float
    @Composable get() = with(LocalDensity.current) { this@px.toPx() }