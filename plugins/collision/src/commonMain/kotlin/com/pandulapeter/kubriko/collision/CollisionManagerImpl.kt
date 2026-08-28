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
    private val collidablesByType = HashMap<KClass<out Collidable>, ArrayList<Collidable>>()
    private var lastCollidablesForTypeCache: ImmutableList<Collidable>? = null

    // Broad phase: one grid per type, indexed in step with that type's candidate list, so a detector
    // tests the masks near it rather than every candidate of the type. Types with few candidates get
    // no grid at all - bucketing them would cost more than the scan it replaces. The grids are also
    // held in a flat list, since every one of them has to be re-bucketed once per frame.
    private val gridsByType = HashMap<KClass<out Collidable>, SpatialHashGrid>()
    private val grids = ArrayList<SpatialHashGrid>()
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
            collidablesByType.clear()
            gridsByType.clear()
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
                val candidates = collidablesByType[type] ?: buildCandidates(type, allCollidables)
                collisionBuffer.clear()
                val grid = gridsByType[type]
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

    private fun buildCandidates(
        type: KClass<out Collidable>,
        allCollidables: ImmutableList<Collidable>,
    ): ArrayList<Collidable> {
        val candidates = ArrayList<Collidable>()
        for (candidate in allCollidables) {
            if (type.isInstance(candidate)) {
                candidates.add(candidate)
            }
        }
        collidablesByType[type] = candidates
        if (candidates.size >= MINIMUM_CANDIDATES_FOR_BROAD_PHASE) {
            val grid = SpatialHashGrid()
            for (candidateIndex in candidates.indices) {
                grid.add(candidates[candidateIndex].collisionMask)
            }
            grid.rebuild()
            gridsByType[type] = grid
            grids.add(grid)
        }
        return candidates
    }

    private companion object {
        const val INITIAL_CANDIDATE_CAPACITY = 64
        const val MINIMUM_CANDIDATES_FOR_BROAD_PHASE = 32
    }
}
