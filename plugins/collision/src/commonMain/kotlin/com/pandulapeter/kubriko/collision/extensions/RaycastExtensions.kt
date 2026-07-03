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

import com.pandulapeter.kubriko.collision.RaycastHit
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Casts a ray from [origin] along [direction] (which need not be normalized) and returns where it first
 * enters this mask, or `null` if it does not within [maxDistance].
 *
 * Only the surface is hit: a ray that starts inside the mask returns `null` (it enters no surface). Point
 * masks are never hit (a ray meeting a single point has zero width). Useful for line-of-sight checks,
 * hitscan weapons, and picking shapes under a screen point.
 *
 * @param origin The starting point of the ray in scene units.
 * @param direction The direction the ray travels; only its orientation matters, not its length.
 * @param maxDistance How far along the ray to look.
 */
fun CollisionMask.raycast(
    origin: SceneOffset,
    direction: SceneOffset,
    maxDistance: SceneUnit,
): RaycastHit? {
    val directionX = direction.x.raw
    val directionY = direction.y.raw
    val length = sqrt(directionX * directionX + directionY * directionY)
    if (length <= 0f || maxDistance <= SceneUnit.Zero) return null
    return raycastHit(
        mask = this,
        originX = origin.x.raw,
        originY = origin.y.raw,
        unitDirectionX = directionX / length,
        unitDirectionY = directionY / length,
        maximumDistance = maxDistance.raw,
    )
}

/**
 * Casts a ray against every mask in this list and returns the nearest [RaycastHit], or `null` when the
 * ray reaches [maxDistance] without entering any of them. A cheap bounding-box test skips masks the ray
 * cannot reach before the per-shape math runs.
 *
 * When only the travel distance matters (line-of-sight or clearance checks), prefer [raycastDistance],
 * which never allocates.
 *
 * @param origin The starting point of the ray in scene units.
 * @param direction The direction the ray travels; only its orientation matters, not its length.
 * @param maxDistance How far along the ray to look.
 */
fun List<CollisionMask>.raycast(
    origin: SceneOffset,
    direction: SceneOffset,
    maxDistance: SceneUnit,
): RaycastHit? {
    val nearest = nearestMaskAlongRay(origin, direction, maxDistance) ?: return null
    // Only the winning mask pays for constructing the hit (entry point and surface normal included).
    val directionX = direction.x.raw
    val directionY = direction.y.raw
    val length = sqrt(directionX * directionX + directionY * directionY)
    return raycastHit(
        mask = nearest,
        originX = origin.x.raw,
        originY = origin.y.raw,
        unitDirectionX = directionX / length,
        unitDirectionY = directionY / length,
        maximumDistance = maxDistance.raw,
    )
}

/**
 * Returns how far a ray from [origin] along [direction] can travel before entering any mask in this list,
 * capped at [maxDistance]: the distance to the nearest surface it enters, or [maxDistance] itself when the
 * ray stays clear. The allocation-free variant of [raycast] for callers that only need the distance
 * (line-of-sight, clearance probes).
 *
 * @param origin The starting point of the ray in scene units.
 * @param direction The direction the ray travels; only its orientation matters, not its length.
 * @param maxDistance How far along the ray to look.
 */
fun List<CollisionMask>.raycastDistance(
    origin: SceneOffset,
    direction: SceneOffset,
    maxDistance: SceneUnit,
): SceneUnit {
    val directionX = direction.x.raw
    val directionY = direction.y.raw
    val length = sqrt(directionX * directionX + directionY * directionY)
    if (length <= 0f || maxDistance <= SceneUnit.Zero) return maxDistance
    val unitDirectionX = directionX / length
    val unitDirectionY = directionY / length
    val originX = origin.x.raw
    val originY = origin.y.raw
    val endpointX = originX + unitDirectionX * maxDistance.raw
    val endpointY = originY + unitDirectionY * maxDistance.raw
    val rayMinX = min(originX, endpointX)
    val rayMaxX = max(originX, endpointX)
    val rayMinY = min(originY, endpointY)
    val rayMaxY = max(originY, endpointY)
    var nearestDistance = maxDistance.raw
    for (index in indices) {
        val mask = this[index]
        val bounds = mask.axisAlignedBoundingBox
        if (bounds.maxXRaw < rayMinX || bounds.minXRaw > rayMaxX || bounds.maxYRaw < rayMinY || bounds.minYRaw > rayMaxY) {
            continue
        }
        val distance = raycastEntryDistance(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance = nearestDistance)
        if (distance >= 0f && distance < nearestDistance) {
            nearestDistance = distance
        }
    }
    return nearestDistance.sceneUnit
}

/**
 * Casts a ray along the segment from [start] to [end] and returns the nearest mask it enters, or `null`
 * when the segment is clear. Convenience over [raycast] for "is anything between these two points?".
 *
 * @param start The start of the segment in scene units.
 * @param end The end of the segment in scene units.
 */
fun List<CollisionMask>.segmentCast(
    start: SceneOffset,
    end: SceneOffset,
): RaycastHit? {
    val deltaX = end.x.raw - start.x.raw
    val deltaY = end.y.raw - start.y.raw
    return raycast(
        origin = start,
        direction = SceneOffset(deltaX.sceneUnit, deltaY.sceneUnit),
        maxDistance = sqrt(deltaX * deltaX + deltaY * deltaY).sceneUnit,
    )
}

// The shared scan behind the List overloads: normalizes the direction once, skips masks whose bounding
// box the ray cannot reach, and tracks the nearest entry with raw floats so no hit objects are created
// for losing candidates.
private fun List<CollisionMask>.nearestMaskAlongRay(
    origin: SceneOffset,
    direction: SceneOffset,
    maxDistance: SceneUnit,
): CollisionMask? {
    val directionX = direction.x.raw
    val directionY = direction.y.raw
    val length = sqrt(directionX * directionX + directionY * directionY)
    if (length <= 0f || maxDistance <= SceneUnit.Zero) return null
    val unitDirectionX = directionX / length
    val unitDirectionY = directionY / length
    val originX = origin.x.raw
    val originY = origin.y.raw
    val endpointX = originX + unitDirectionX * maxDistance.raw
    val endpointY = originY + unitDirectionY * maxDistance.raw
    val rayMinX = min(originX, endpointX)
    val rayMaxX = max(originX, endpointX)
    val rayMinY = min(originY, endpointY)
    val rayMaxY = max(originY, endpointY)
    var nearest: CollisionMask? = null
    var nearestDistance = Float.MAX_VALUE
    for (index in indices) {
        val mask = this[index]
        val bounds = mask.axisAlignedBoundingBox
        if (bounds.maxXRaw < rayMinX || bounds.minXRaw > rayMaxX || bounds.maxYRaw < rayMinY || bounds.minYRaw > rayMaxY) {
            continue
        }
        val distance = raycastEntryDistance(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance = maxDistance.raw)
        if (distance >= 0f && distance < nearestDistance) {
            nearestDistance = distance
            nearest = mask
        }
    }
    return nearest
}

// Distance to where the ray first enters the mask, or a negative value when it does not within
// maximumDistance. The allocation-free core shared by every raycast variant.
private fun raycastEntryDistance(
    mask: CollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): Float = when (mask) {
    is CircleCollisionMask -> raycastCircleEntryDistance(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance)
    is PolygonCollisionMask -> raycastPolygonEntryDistance(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance)
    else -> -1f
}

private fun raycastCircleEntryDistance(
    circle: CircleCollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): Float {
    val toOriginX = originX - circle.position.x.raw
    val toOriginY = originY - circle.position.y.raw
    // Quadratic |origin + t * direction - center|^2 = radius^2, with the direction normalized so its
    // squared term is 1; the nearest root is the entry point.
    val halfB = toOriginX * unitDirectionX + toOriginY * unitDirectionY
    val c = toOriginX * toOriginX + toOriginY * toOriginY - circle.radius.raw * circle.radius.raw
    val discriminant = halfB * halfB - c
    if (discriminant < 0f) {
        return -1f
    }
    val entry = -halfB - sqrt(discriminant)
    return if (entry < 0f || entry > maximumDistance) -1f else entry
}

private fun raycastPolygonEntryDistance(
    polygon: PolygonCollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): Float {
    var bestDistance = Float.MAX_VALUE
    val row1X = polygon.rotationMatrix.row1.x.raw
    val row1Y = polygon.rotationMatrix.row1.y.raw
    val row2X = polygon.rotationMatrix.row2.x.raw
    val row2Y = polygon.rotationMatrix.row2.y.raw
    val positionX = polygon.position.x.raw
    val positionY = polygon.position.y.raw
    val vertices = polygon.vertices
    val normals = polygon.normals
    for (index in vertices.indices) {
        val vertex = vertices[index]
        val startX = row1X * vertex.x.raw + row1Y * vertex.y.raw + positionX
        val startY = row2X * vertex.x.raw + row2Y * vertex.y.raw + positionY
        val nextVertex = vertices[if (index + 1 == vertices.size) 0 else index + 1]
        val endX = row1X * nextVertex.x.raw + row1Y * nextVertex.y.raw + positionX
        val endY = row2X * nextVertex.x.raw + row2Y * nextVertex.y.raw + positionY
        val edgeX = endX - startX
        val edgeY = endY - startY
        // Solve origin + t * direction = edgeStart + u * edge using 2D cross products.
        val denominator = unitDirectionX * edgeY - unitDirectionY * edgeX
        if (denominator == 0f) {
            continue
        }
        val toStartX = startX - originX
        val toStartY = startY - originY
        val distance = (toStartX * edgeY - toStartY * edgeX) / denominator
        if (distance < 0f || distance > maximumDistance || distance >= bestDistance) {
            continue
        }
        val edgeFraction = (toStartX * unitDirectionY - toStartY * unitDirectionX) / denominator
        if (edgeFraction < 0f || edgeFraction > 1f) {
            continue
        }
        val localNormal = normals[index]
        val normalX = row1X * localNormal.x.raw + row1Y * localNormal.y.raw
        val normalY = row2X * localNormal.x.raw + row2Y * localNormal.y.raw
        // Skip edges whose outward normal points along the ray: those are exits, not entries.
        if (normalX * unitDirectionX + normalY * unitDirectionY >= 0f) {
            continue
        }
        bestDistance = distance
    }
    return if (bestDistance == Float.MAX_VALUE) -1f else bestDistance
}

// Builds the full hit (entry point and surface normal) for a single mask; only called for the mask that
// won the scan, or from the single-mask public API.
private fun raycastHit(
    mask: CollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): RaycastHit? = when (mask) {
    is CircleCollisionMask -> raycastCircleHit(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance)
    is PolygonCollisionMask -> raycastPolygonHit(mask, originX, originY, unitDirectionX, unitDirectionY, maximumDistance)
    else -> null
}

private fun raycastCircleHit(
    circle: CircleCollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): RaycastHit? {
    val entry = raycastCircleEntryDistance(circle, originX, originY, unitDirectionX, unitDirectionY, maximumDistance)
    if (entry < 0f) {
        return null
    }
    val pointX = originX + unitDirectionX * entry
    val pointY = originY + unitDirectionY * entry
    var normalX = pointX - circle.position.x.raw
    var normalY = pointY - circle.position.y.raw
    val normalLength = sqrt(normalX * normalX + normalY * normalY)
    if (normalLength > NORMAL_EPSILON) {
        normalX /= normalLength
        normalY /= normalLength
    } else {
        normalX = -unitDirectionX
        normalY = -unitDirectionY
    }
    return RaycastHit(
        mask = circle,
        point = SceneOffset(pointX.sceneUnit, pointY.sceneUnit),
        normal = SceneOffset(normalX.sceneUnit, normalY.sceneUnit),
        distance = entry.sceneUnit,
    )
}

private fun raycastPolygonHit(
    polygon: PolygonCollisionMask,
    originX: Float,
    originY: Float,
    unitDirectionX: Float,
    unitDirectionY: Float,
    maximumDistance: Float,
): RaycastHit? {
    var bestDistance = Float.MAX_VALUE
    var bestNormalX = 0f
    var bestNormalY = 0f
    val row1X = polygon.rotationMatrix.row1.x.raw
    val row1Y = polygon.rotationMatrix.row1.y.raw
    val row2X = polygon.rotationMatrix.row2.x.raw
    val row2Y = polygon.rotationMatrix.row2.y.raw
    val positionX = polygon.position.x.raw
    val positionY = polygon.position.y.raw
    val vertices = polygon.vertices
    val normals = polygon.normals
    for (index in vertices.indices) {
        val vertex = vertices[index]
        val startX = row1X * vertex.x.raw + row1Y * vertex.y.raw + positionX
        val startY = row2X * vertex.x.raw + row2Y * vertex.y.raw + positionY
        val nextVertex = vertices[if (index + 1 == vertices.size) 0 else index + 1]
        val endX = row1X * nextVertex.x.raw + row1Y * nextVertex.y.raw + positionX
        val endY = row2X * nextVertex.x.raw + row2Y * nextVertex.y.raw + positionY
        val edgeX = endX - startX
        val edgeY = endY - startY
        // Solve origin + t * direction = edgeStart + u * edge using 2D cross products.
        val denominator = unitDirectionX * edgeY - unitDirectionY * edgeX
        if (denominator == 0f) {
            continue
        }
        val toStartX = startX - originX
        val toStartY = startY - originY
        val distance = (toStartX * edgeY - toStartY * edgeX) / denominator
        if (distance < 0f || distance > maximumDistance || distance >= bestDistance) {
            continue
        }
        val edgeFraction = (toStartX * unitDirectionY - toStartY * unitDirectionX) / denominator
        if (edgeFraction < 0f || edgeFraction > 1f) {
            continue
        }
        val localNormal = normals[index]
        val normalX = row1X * localNormal.x.raw + row1Y * localNormal.y.raw
        val normalY = row2X * localNormal.x.raw + row2Y * localNormal.y.raw
        // Skip edges whose outward normal points along the ray: those are exits, not entries.
        if (normalX * unitDirectionX + normalY * unitDirectionY >= 0f) {
            continue
        }
        bestDistance = distance
        bestNormalX = normalX
        bestNormalY = normalY
    }
    if (bestDistance == Float.MAX_VALUE) {
        return null
    }
    return RaycastHit(
        mask = polygon,
        point = SceneOffset((originX + unitDirectionX * bestDistance).sceneUnit, (originY + unitDirectionY * bestDistance).sceneUnit),
        normal = SceneOffset(bestNormalX.sceneUnit, bestNormalY.sceneUnit),
        distance = bestDistance.sceneUnit,
    )
}

private const val NORMAL_EPSILON = 1e-4f
