/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testCollision.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

internal class RayEmitter : DraggableActor(
    collisionMask = CircleCollisionMask(
        initialRadius = EmitterRadius,
    )
) {
    override val shouldClip = false
    private val rays = (0..RAY_COUNT).map { index ->
        val angle = (AngleRadians.TwoPi / RAY_COUNT) * index
        Ray(
            start = body.position + body.pivot,
            end = body.position + body.pivot + SceneOffset(
                x = RAY_LENGTH.sceneUnit * angle.cos,
                y = -RAY_LENGTH.sceneUnit * angle.sin,
            )
        )
    }

    override fun onAdded(kubriko: Kubriko) {
        super.onAdded(kubriko)
        drawingOrder = -Float.MAX_VALUE
    }

    private data class Ray(
        val start: SceneOffset,
        val end: SceneOffset,
    )

    override fun DrawScope.draw() {
        rays.forEach { ray ->
            drawLine(
                color = Color.White,
                start = ray.start.raw,
                end = ray.end.raw,
            )
        }
        drawCircle(
            color = Color.White,
            radius = EmitterRadius.raw,
            center = body.pivot.raw,
            style = Fill,
        )
        drawCircle(
            color = Color.Black,
            radius = EmitterRadius.raw,
            center = body.pivot.raw,
            style = Stroke(),
        )
    }

    companion object {
        private val EmitterRadius = 20.sceneUnit
        private const val RAY_COUNT = 32
        private const val RAY_LENGTH = 1024
    }
}