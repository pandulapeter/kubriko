package com.pandulapeter.kubriko.collision.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.implementation.extensions.isOverlapping
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
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
                    .filter { collidableType.isInstance(it) && collisionDetector.isOverlapping(it) }
                    .let {
                        if (it.isNotEmpty()) {
                            collisionDetector.onCollisionDetected(it)
                        }
                    }
            }
        }
    }
}