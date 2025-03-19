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
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.random.Random

internal class DraggableCollidableActor(
    collisionMask: ComplexCollisionMask,
) : DraggableActor(
    collisionMask = collisionMask,
) {

    override fun DrawScope.draw() = with(collisionMask) {
        drawDebugBounds(if (collisions.isNotEmpty()) Color.DarkGray else Color.Gray, Fill)
        drawDebugBounds(Color.Black, Stroke())
    }

    companion object {
        fun newRandomShape(initialPosition: SceneOffset) = DraggableCollidableActor(
            collisionMask = when (Random.nextInt(3)) {
                0 -> BoxCollisionMask(
                    initialPosition = initialPosition,
                    initialSize = SceneSize(
                        width = (20 + 80 * Random.nextFloat()).sceneUnit,
                        height = (20 + 80 * Random.nextFloat()).sceneUnit,
                    ),
                    initialRotation = AngleRadians.TwoPi * Random.nextFloat(),
                )

                1 -> PolygonCollisionMask(
                    initialOffset = initialPosition,
                    vertices = (3..10).random().let { sideCount ->
                        (0..sideCount).map { sideIndex ->
                            val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
                            SceneOffset(
                                x = (10..40).random().sceneUnit * angle.cos,
                                y = (10..40).random().sceneUnit * angle.sin,
                            )
                        }
                    },
                    initialRotation = AngleRadians.TwoPi * Random.nextFloat(),
                )

                else -> CircleCollisionMask(
                    initialPosition = initialPosition,
                    initialRadius = (10 + 40 * Random.nextFloat()).sceneUnit,
                )
            }
        )
    }
}