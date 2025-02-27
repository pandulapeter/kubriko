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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubriko.actor.traits.Visible


@Composable
internal fun MiniMap(
    size: Dp,
    gameTime: Long,
    getAllVisibleActors: () -> List<Visible>,
    getAllVisibleActorsWithinViewport: () -> List<Visible>,
) = Canvas(
    modifier = Modifier.size(size),
    onDraw = {
        withTransform(
            transformBlock = {
                translate(
                    left = size.value,
                    top = size.value,
                )
            },
            drawBlock = {
                val allVisibleActors = getAllVisibleActors()
                val visibleActorsWithinViewport = getAllVisibleActorsWithinViewport()
                @Suppress("UNUSED_EXPRESSION") gameTime  // This line invalidates the Canvas, causing a refresh on every frame
                allVisibleActors.forEachIndexed { index, actor ->
                    if (index % 4 == 0) {
                        drawCircle(
                            color = if (visibleActorsWithinViewport.contains(actor)) Color.Green else Color.Red,
                            radius = 1.5f,
                            center = actor.body.position.raw / 70f,
                        )
                    }
                }
            }
        )
    }
)