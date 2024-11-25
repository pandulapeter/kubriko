package com.pandulapeter.kubriko.collision.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class CollisionManagerImpl : CollisionManager() {

    private lateinit var actorManager: ActorManager
    private val collisionDetectors by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<CollisionDetector>()
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }
    private val collidables by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<Collidable>()
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        collisionDetectors.value.forEach { collisionDetector ->
            collisionDetector.collidableTypes.forEach { collidableType ->
                collidables.value
                    .filter { collidableType.isInstance(it) && collisionDetector.overlapAreaCenter(it) !=null }
                    .let {
                        if (it.isNotEmpty()) {
                            collisionDetector.onCollisionDetected(it)
                        }
                    }
            }
        }
    }

    // TODO: AABB - might be merged with the implementation from the physics module
    private fun Collidable.overlapAreaCenter(
        other: Collidable,
    ): SceneOffset? {
        // Calculate the top-left and bottom-right corners of the bounding box for this Collidable
        val thisTopLeft = position - pivotOffset
        val thisBottomRight = thisTopLeft + SceneOffset(boundingBox.width, boundingBox.height)

        // Calculate the top-left and bottom-right corners of the bounding box for the other Collidable
        val otherTopLeft = other.position - other.pivotOffset
        val otherBottomRight = otherTopLeft + SceneOffset(other.boundingBox.width, other.boundingBox.height)

        // Find the overlapping area's bounds
        val overlapTopLeft = SceneOffset(
            x = maxOf(thisTopLeft.x, otherTopLeft.x),
            y = maxOf(thisTopLeft.y, otherTopLeft.y)
        )
        val overlapBottomRight = SceneOffset(
            x = minOf(thisBottomRight.x, otherBottomRight.x),
            y = minOf(thisBottomRight.y, otherBottomRight.y)
        )

        // Check for overlap: if the overlap's width or height is negative, there is no collision
        if (overlapTopLeft.x >= overlapBottomRight.x || overlapTopLeft.y >= overlapBottomRight.y) {
            return null // No collision
        }

        // Calculate the center of the overlapping area
        return SceneOffset(
            x = (overlapTopLeft.x + overlapBottomRight.x) / 2,
            y = (overlapTopLeft.y + overlapBottomRight.y) / 2
        )
    }
}