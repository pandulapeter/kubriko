/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.extensions.isCollidingWith
import com.pandulapeter.kubriko.manager.ActorManager
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class CollisionManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : CollisionManager(isLoggingEnabled, instanceNameForLogging) {
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

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        collisionDetectors.value.forEach { collisionDetector ->
            collisionDetector.collidableTypes.forEach { collidableType ->
                collidables.value
                    .filter { collidableType.isInstance(it) && collisionDetector.collisionMask.isCollidingWith(it.collisionMask) && it != collisionDetector }
                    .let {
                        if (it.isNotEmpty()) {
                            collisionDetector.onCollisionDetected(it)
                        }
                    }
            }
        }
    }
}