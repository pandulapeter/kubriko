package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.extensions.isOverlapping
import com.pandulapeter.kubriko.manager.ActorManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class CollisionManagerImpl : CollisionManager() {

    private val actorManager by manager<ActorManager>()
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

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        collisionDetectors.value.forEach { collisionDetector ->
            collisionDetector.collidableTypes.forEach { collidableType ->
                collidables.value
                    .filter { collidableType.isInstance(it) && collisionDetector.body.isOverlapping(it.body) }
                    .let {
                        if (it.isNotEmpty()) {
                            collisionDetector.onCollisionDetected(it)
                        }
                    }
            }
        }
    }
}