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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

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
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val collidables by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors.filterIsInstance<Collidable>().toImmutableList()
        }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }

    private val collisionBuffer = mutableListOf<Collidable>()

    // Iteration mirror of the published detector list: an ArrayList is indexable in O(1), while
    // iterating the persistent list directly would allocate a trie iterator every frame.
    private val detectorsMirror = ArrayList<CollisionDetector>()
    private var lastMirroredDetectors: ImmutableList<CollisionDetector>? = null

    // Candidates pre-filtered per collidable type. KClass.isInstance is reflective (notably slow on
    // Wasm) and the result cannot change while the actor list is unchanged, so the filtering runs
    // once per type per actor-list change instead of per candidate per detector per frame.
    private val collidablesByType = HashMap<KClass<out Collidable>, ArrayList<Collidable>>()
    private var lastCollidablesForTypeCache: ImmutableList<Collidable>? = null

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val detectors = collisionDetectors.value
        if (detectors !== lastMirroredDetectors) {
            lastMirroredDetectors = detectors
            detectorsMirror.clear()
            detectorsMirror.addAll(detectors)
        }
        val allCollidables = collidables.value
        if (allCollidables !== lastCollidablesForTypeCache) {
            lastCollidablesForTypeCache = allCollidables
            collidablesByType.clear()
        }
        for (detectorIndex in detectorsMirror.indices) {
            val detector = detectorsMirror[detectorIndex]
            val types = detector.collidableTypes
            for (typeIndex in types.indices) {
                val type = types[typeIndex]
                val candidates = collidablesByType.getOrPut(type) {
                    val filtered = ArrayList<Collidable>()
                    for (candidate in allCollidables) {
                        if (type.isInstance(candidate)) {
                            filtered.add(candidate)
                        }
                    }
                    filtered
                }
                collisionBuffer.clear()
                for (candidateIndex in candidates.indices) {
                    val candidate = candidates[candidateIndex]
                    if (candidate !== detector && detector.isCollidingWith(candidate)) {
                        collisionBuffer.add(candidate)
                    }
                }
                if (collisionBuffer.isNotEmpty()) {
                    detector.onCollisionDetected(collisionBuffer)
                }
            }
        }
    }
}