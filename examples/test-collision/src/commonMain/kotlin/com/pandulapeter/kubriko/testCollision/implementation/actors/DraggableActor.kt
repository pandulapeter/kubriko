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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.random.Random

internal class DraggableActor(
    override val collisionMask: ComplexCollisionMask,
) : Visible, CollisionDetector, PointerInputAware, Dynamic {

    override val body = BoxBody(
        initialPosition = collisionMask.position,
        initialRotation = collisionMask.rotation,
        initialPivot = collisionMask.pivot,
        initialSize = collisionMask.axisAlignedBoundingBox.size,
    )
    override val collidableTypes = listOf(DraggableActor::class)
    private var isColliding = false

    init {
        (AngleRadians.TwoPi * Random.nextFloat()).let { rotation ->
            collisionMask.rotation = rotation
            body.rotation = rotation
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        isColliding = true
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        isColliding = false
    }

    override fun DrawScope.draw() = with(collisionMask) {
        drawDebugBounds(if (isColliding) Color.Gray else Color.DarkGray, Fill)
        drawDebugBounds(Color.Black, Stroke())
    }

    companion object {
        fun newRandomShape(initialPosition: SceneOffset) = DraggableActor(
            collisionMask = when (Random.nextInt(2)) {
                0 -> BoxCollisionMask(
                    initialPosition = initialPosition,
                    initialSize = SceneSize(
                        width = (20 + 80 * Random.nextFloat()).sceneUnit,
                        height = (20 + 80 * Random.nextFloat()).sceneUnit,
                    ),
                )

                else -> CircleCollisionMask(
                    initialPosition = initialPosition,
                    initialRadius = (10 + 40 * Random.nextFloat()).sceneUnit,
                )

                // TODO: PolygonCollisionMask
            }
        )
    }
}