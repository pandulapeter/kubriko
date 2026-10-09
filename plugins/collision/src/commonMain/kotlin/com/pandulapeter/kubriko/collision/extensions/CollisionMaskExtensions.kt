/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.collision.CollisionResult
import com.pandulapeter.kubriko.collision.implementation.RESULT_NONE
import com.pandulapeter.kubriko.collision.implementation.RESULT_OBJECT
import com.pandulapeter.kubriko.collision.implementation.RESULT_SCRATCH
import com.pandulapeter.kubriko.collision.implementation.collisionCheck
import com.pandulapeter.kubriko.collision.implementation.scratchContactNormal
import com.pandulapeter.kubriko.collision.implementation.scratchPenetration
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

fun Collidable.isCollidingWith(
    other: Collidable
) = collisionMask.hasCollisionWith(other.collisionMask)

fun CollisionMask.collisionResultWith(
    other: CollisionMask,
    shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
): CollisionResult? = collisionResultWith(
    other = other,
    shouldSkipAxisAlignedBoundingBoxCheck = shouldSkipAxisAlignedBoundingBoxCheck,
    reusableResult = null,
)

/**
 * Allocation-free variant of [collisionResultWith] for callers that query the same pair every frame: when
 * [reusableResult] is not `null` it is overwritten with the new contact details and returned instead of a
 * new [CollisionResult]. Returns `null` (leaving [reusableResult] untouched) when the masks do not overlap.
 *
 * @param other The mask to test against.
 * @param shouldSkipAxisAlignedBoundingBoxCheck Skips the bounding box pre-check, for callers that already ran a broad phase.
 * @param reusableResult A result from an earlier call to overwrite, or `null` to allocate a new one.
 */
fun CollisionMask.collisionResultWith(
    other: CollisionMask,
    shouldSkipAxisAlignedBoundingBoxCheck: Boolean,
    reusableResult: CollisionResult?,
): CollisionResult? = collisionCheck(
    other = other,
    shouldSkipAxisAlignedBoundingBoxCheck = shouldSkipAxisAlignedBoundingBoxCheck,
    resultMode = RESULT_OBJECT,
    reusableResult = reusableResult,
)

/**
 * Computes the part of [desiredMovement] this mask can travel without ending up overlapping any of the
 * given [obstacles], sliding along their surfaces so an angled approach glides around an obstacle's edge
 * instead of stopping dead against it.
 *
 * The mask is advanced to the target position and then pushed back out of any overlap along the
 * obstacles' contact normals. For convex obstacles this push-out is what produces the slide: it cancels
 * the part of the movement aimed into the obstacle while leaving the tangential part, so the actor keeps
 * as much of its speed as it can while following the obstacle's contour. Only a head-on approach
 * (movement aimed straight at the contact) is pushed fully back and settles at the surface with no net
 * movement. The deepest overlap is resolved first and the step repeats up to [maximumSlideIterations]
 * times to settle corners where pushing out of one obstacle pushes into another.
 *
 * Intended for kinematic actors (ones moved by writing their position directly, without the physics
 * plugin) that should be blocked by solid scenery: apply the returned offset to the actor's position
 * instead of [desiredMovement]. The mask is probed at candidate positions during the call and restored to
 * its original position before returning, so the caller stays in control of when the movement is committed.
 *
 * @param desiredMovement The movement the actor would make if nothing were in the way.
 * @param obstacles The masks that should block movement. The receiver is ignored if it is present.
 * @param maximumSlideIterations The number of allowed overlap-resolution passes.
 * @return The largest collision-free movement: [desiredMovement] when the path is clear, a shorter
 * offset tangential to the blocking obstacle when it can slide, or [SceneOffset.Zero] when it cannot.
 */
fun CollisionMask.slidingMovement(
    desiredMovement: SceneOffset,
    obstacles: List<CollisionMask>,
    maximumSlideIterations: Int = 4,
): SceneOffset {
    val origin = position
    try {
        position = origin + desiredMovement
        repeat(maximumSlideIterations) {
            if (!findDeepestOverlapWith(obstacles)) {
                return position - origin
            }
            position -= deepestOverlapContactNormal * deepestOverlapPenetration
        }
        return position - origin
    } finally {
        position = origin
    }
}

/**
 * Convenience overload of [slidingMovement] that sources the obstacles from [collisionManager] instead
 * of a hand-built list: every current [Collidable] except this one (and except any rejected by
 * [isObstacle]) becomes an obstacle. Returns the part of [desiredMovement] this actor can travel,
 * sliding along the obstacles it would otherwise pass through.
 *
 * This rebuilds the obstacle list on every call, so for an actor that moves each frame the
 * [CollisionMask]-list overload of [slidingMovement] (fed a list the caller refreshes only when the set
 * of obstacles actually changes) avoids the per-frame allocation. Reach for this one when the
 * convenience outweighs that.
 *
 * @param desiredMovement The movement the actor would make if nothing were in the way.
 * @param collisionManager The manager whose [CollisionManager.collidables] supply the obstacles.
 * @param maximumSlideIterations The number of allowed overlap-resolution passes.
 * @param isObstacle Decides which collidables block this actor; defaults to all of them.
 * @return The largest collision-free movement, as described on [slidingMovement].
 */
fun Collidable.slidingMovement(
    desiredMovement: SceneOffset,
    collisionManager: CollisionManager,
    maximumSlideIterations: Int = 4,
    isObstacle: (Collidable) -> Boolean = { true },
): SceneOffset {
    val candidates = collisionManager.collidables.value
    val obstacles = ArrayList<CollisionMask>(candidates.size)
    for (index in candidates.indices) {
        val candidate = candidates[index]
        if (candidate !== this && isObstacle(candidate)) {
            obstacles.add(candidate.collisionMask)
        }
    }
    return collisionMask.slidingMovement(desiredMovement, obstacles, maximumSlideIterations)
}

/**
 * Computes an offset that pushes this mask out of any of the given [obstacles] it currently
 * overlaps, summing the resolution vector (contact normal scaled by penetration depth) of each
 * overlap.
 *
 * This recovers from overlaps a [slidingMovement] sweep cannot prevent, such as an actor that
 * spawned inside scenery or scenery that was placed on top of it: apply the returned offset to the
 * actor's position to separate it.
 *
 * @param obstacles The masks to separate from. The receiver is ignored if it is present.
 * @return The offset that resolves the overlaps, or [SceneOffset.Zero] when there is none.
 */
fun CollisionMask.depenetrationFrom(
    obstacles: List<CollisionMask>,
): SceneOffset {
    var push = SceneOffset.Zero
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this) {
            collisionResultWith(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
            )?.let { result ->
                push -= result.contactNormal * result.penetration
            }
        }
    }
    return push
}

/**
 * Returns whether this mask overlaps any of the given [obstacles], skipping the receiver if it appears
 * among them. Uses the allocation-free boolean narrow phase ([hasCollisionWith]) and stops at the first
 * overlap, so it is cheaper than collecting a [CollisionResult] when only a yes/no answer is needed.
 */
fun CollisionMask.collidesWithAny(
    obstacles: List<CollisionMask>,
): Boolean {
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this && hasCollisionWith(obstacle)) {
            return true
        }
    }
    return false
}

/**
 * Returns the [CollisionResult] for the obstacle this mask overlaps most deeply, or `null` when it
 * overlaps none of them. The receiver is skipped if it appears among [obstacles].
 *
 * Useful for custom kinematic response that needs the contact details (normal, depth, point): resolve
 * the worst overlap first and re-test, which is exactly how [slidingMovement] slides along obstacles.
 *
 * @param obstacles The masks to test against. The receiver is ignored if it is present.
 */
fun CollisionMask.deepestCollisionWith(
    obstacles: List<CollisionMask>,
): CollisionResult? {
    var deepest: CollisionResult? = null
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this) {
            val result = collisionResultWith(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
            )
            if (result != null && (deepest == null || result.penetration > deepest.penetration)) {
                deepest = result
            }
        }
    }
    return deepest
}

/**
 * The allocation-free counterpart of [deepestCollisionWith] behind [slidingMovement]: returns whether this mask overlaps
 * any of the [obstacles], leaving the deepest overlap's normal and depth in [deepestOverlapContactNormal] and
 * [deepestOverlapPenetration].
 */
private fun CollisionMask.findDeepestOverlapWith(
    obstacles: List<CollisionMask>,
): Boolean {
    var isOverlapping = false
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this && collisionCheck(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
                resultMode = RESULT_SCRATCH,
            ) != null && (!isOverlapping || scratchPenetration > deepestOverlapPenetration)
        ) {
            deepestOverlapContactNormal = scratchContactNormal
            deepestOverlapPenetration = scratchPenetration
            isOverlapping = true
        }
    }
    return isOverlapping
}

/**
 * Returns the [CollisionResult] for the first obstacle this mask overlaps, or `null` when it overlaps
 * none of them, stopping at the first hit. The receiver is skipped if it appears among [obstacles].
 *
 * Cheaper than [deepestCollisionWith] when any one contact is enough (a bullet that hits a wall, a
 * sensor that only needs to know what it touched first).
 *
 * @param obstacles The masks to test against. The receiver is ignored if it is present.
 */
fun CollisionMask.firstCollisionWith(
    obstacles: List<CollisionMask>,
): CollisionResult? {
    for (index in obstacles.indices) {
        val obstacle = obstacles[index]
        if (obstacle !== this) {
            collisionResultWith(
                other = obstacle,
                shouldSkipAxisAlignedBoundingBoxCheck = false,
            )?.let { return it }
        }
    }
    return null
}

/**
 * Boolean-only collision test: runs the same broad and narrow phase as [collisionResultWith] but
 * never constructs a [CollisionResult] (the detection loop in CollisionManagerImpl only needs the
 * yes/no answer, and the result object would otherwise be allocated for every colliding pair on
 * every frame).
 */
fun CollisionMask.hasCollisionWith(other: CollisionMask): Boolean = collisionCheck(
    other = other,
    shouldSkipAxisAlignedBoundingBoxCheck = false,
    resultMode = RESULT_NONE,
) != null

private var deepestOverlapContactNormal = SceneOffset.Zero
private var deepestOverlapPenetration = SceneUnit.Zero
