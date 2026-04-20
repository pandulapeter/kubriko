/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class CollisionManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : CollisionManager(isLoggingEnabled, instanceNameForLogging) {

    private val actorManager by manager<ActorManager>()
    private val collisionDetectors by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors.filterIsInstance<CollisionDetector>().toImmutableList()
        }
            .flowOn(Dispatchers.Default)
            .asStateFlow(persistentListOf())
    }
    private val collidablesByType by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors.filterIsInstance<Collidable>()
                .groupBy { it::class }
        }
            .flowOn(Dispatchers.Default)
            .asStateFlow(emptyMap())
    }
    private val collisionBuffer = mutableListOf<Collidable>()

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val detectors = collisionDetectors.value
        val allCollidables = collidablesByType.value
        detectors.forEach { detector ->
            detector.collidableTypes.forEach { type ->
                val candidates = allCollidables[type] ?: return@forEach
                collisionBuffer.clear()
                candidates.forEach { candidate ->
                    if (candidate !== detector && detector.isCollidingWith(candidate)) {
                        collisionBuffer.add(candidate)
                    }
                }
                if (collisionBuffer.isNotEmpty()) {
                    detector.onCollisionDetected(collisionBuffer.toList())
                }
            }
        }
    }
}