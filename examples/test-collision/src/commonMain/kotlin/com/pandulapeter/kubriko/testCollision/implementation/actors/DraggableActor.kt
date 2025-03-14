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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.extensions.isCollidingWith
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ViewportManager
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
    private var collisions = emptyList<DraggableActor>()
    private var isBeingDragged = false
    private var trackingPointerId: PointerId? = null
    private lateinit var viewportManager: ViewportManager
    private var dragOffset = body.position
    override var drawingOrder = 0f
        private set


    init {
        (AngleRadians.TwoPi * Random.nextFloat()).let { rotation ->
            collisionMask.rotation = rotation
            body.rotation = rotation
        }
    }

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        if (!isBeingDragged) {
            val sceneOffset = screenOffset.toSceneOffset(viewportManager)
            isBeingDragged = sceneOffset.isCollidingWith(collisionMask)
            if (isBeingDragged) {
                drawingOrder -= 1f
                dragOffset = sceneOffset - body.position
                trackingPointerId = pointerId
            }
        }
    }

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        if (pointerId == trackingPointerId) {
            isBeingDragged = false
        }
    }

    override fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) {
        if (isBeingDragged && pointerId == trackingPointerId) {
            body.position = screenOffset.toSceneOffset(viewportManager) - dragOffset
            collisionMask.position = body.position
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        collisions = collidables.filterIsInstance<DraggableActor>()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        collisions = emptyList()
    }

    override fun DrawScope.draw() = with(collisionMask) {
        drawDebugBounds(if (collisions.isNotEmpty()) Color.DarkGray else Color.Gray, Fill)
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