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
    override val collisionDetectors by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors.filterIsInstance<CollisionDetector>().toImmutableList()
        }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    override val collidables by autoInitializingLazy {
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
    //
    // Broad phase: one grid per type, indexed in step with that type's candidate list, so a detector
    // tests the masks near it rather than every candidate of the type. Types with few candidates get
    // no grid at all - bucketing them would cost more than the scan it replaces. The grids are also
    // held in a flat list, since every one of them has to be re-bucketed once per frame.
    private val cachesByType = HashMap<KClass<out Collidable>, CollidableTypeCache>()
    private val grids = ArrayList<SpatialHashGrid>()
    private var lastCollidablesForTypeCache: ImmutableList<Collidable>? = null
    private var candidateGeneration = 0
    private var candidateIndices = IntArray(INITIAL_CANDIDATE_CAPACITY)

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
            // Every type index is now stale, but its candidate list and grid are refilled rather than
            // replaced, so a spawn- or despawn-heavy scene keeps the storage that makes the steady-state
            // broad phase cheap. Emptying them here is what releases the actors that left the scene.
            candidateGeneration++
            for (cache in cachesByType.values) {
                cache.candidates.clear()
                cache.grid?.clear()
            }
            grids.clear()
        }
        for (gridIndex in grids.indices) {
            grids[gridIndex].rebuild()
        }
        for (detectorIndex in detectorsMirror.indices) {
            val detector = detectorsMirror[detectorIndex]
            val types = detector.collidableTypes
            for (typeIndex in types.indices) {
                val type = types[typeIndex]
                val cache = cachesByType[type] ?: CollidableTypeCache().also { cachesByType[type] = it }
                if (cache.generation != candidateGeneration) {
                    refreshCandidates(cache, type, allCollidables)
                }
                val candidates = cache.candidates
                collisionBuffer.clear()
                val grid = if (cache.isGridInUse) cache.grid else null
                if (grid == null) {
                    for (candidateIndex in candidates.indices) {
                        collectIfColliding(detector, candidates[candidateIndex])
                    }
                } else {
                    val bounds = detector.collisionMask.axisAlignedBoundingBox
                    val candidateCount = grid.findCandidates(bounds.left, bounds.top, bounds.right, bounds.bottom)
                    if (candidateIndices.size < candidateCount) {
                        candidateIndices = IntArray(candidateCount * 2)
                    }
                    grid.candidateIndices.copyInto(candidateIndices, 0, 0, candidateCount)
                    // A detector's collision list stays in candidate order, which the grid's cell walk
                    // reaches the same masks in an arbitrary order of.
                    candidateIndices.sort(0, candidateCount)
                    for (candidateIndex in 0 until candidateCount) {
                        collectIfColliding(detector, candidates[candidateIndices[candidateIndex]])
                    }
                }
                if (collisionBuffer.isNotEmpty()) {
                    detector.onCollisionDetected(collisionBuffer)
                }
            }
        }
    }

    private fun collectIfColliding(detector: CollisionDetector, candidate: Collidable) {
        if (candidate !== detector && detector.isCollidingWith(candidate)) {
            collisionBuffer.add(candidate)
        }
    }

    private fun refreshCandidates(
        cache: CollidableTypeCache,
        type: KClass<out Collidable>,
        allCollidables: ImmutableList<Collidable>,
    ) {
        val candidates = cache.candidates
        candidates.clear()
        for (candidate in allCollidables) {
            if (type.isInstance(candidate)) {
                candidates.add(candidate)
            }
        }
        cache.generation = candidateGeneration
        if (candidates.size >= MINIMUM_CANDIDATES_FOR_BROAD_PHASE) {
            val grid = cache.grid ?: SpatialHashGrid().also { cache.grid = it }
            grid.clear()
            for (candidateIndex in candidates.indices) {
                grid.add(candidates[candidateIndex].collisionMask)
            }
            grid.rebuild()
            grids.add(grid)
            cache.isGridInUse = true
        } else {
            cache.grid?.clear()
            cache.isGridInUse = false
        }
    }

    private class CollidableTypeCache {
        val candidates = ArrayList<Collidable>()
        var generation = -1

        // Kept across membership changes rather than discarded, so its buckets and entry arrays survive.
        // Null until the type first has enough candidates to be worth bucketing, and retained (but unused)
        // if it later drops below that threshold, so a type hovering around it does not churn grids.
        var grid: SpatialHashGrid? = null
        var isGridInUse = false
    }

    private companion object {
        const val INITIAL_CANDIDATE_CAPACITY = 64
        const val MINIMUM_CANDIDATES_FOR_BROAD_PHASE = 32
    }
}
