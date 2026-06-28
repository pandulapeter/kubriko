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

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.collision.RaycastHit
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
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
    val length = direction.length()
    if (length <= SceneUnit.Zero || maxDistance <= SceneUnit.Zero) return null
    val unitDirection = direction.scalar(1f / length.raw)
    return when (this) {
        is CircleCollisionMask -> raycastCircle(this, origin, unitDirection, maxDistance)
        is PolygonCollisionMask -> raycastPolygon(this, origin, unitDirection, maxDistance)
        else -> null
    }
}

/**
 * Casts a ray against every mask in this list and returns the nearest [RaycastHit], or `null` when the
 * ray reaches [maxDistance] without entering any of them. A cheap bounding-box test skips masks the ray
 * cannot reach before the per-shape math runs.
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
    val length = direction.length()
    if (length <= SceneUnit.Zero || maxDistance <= SceneUnit.Zero) return null
    val endpoint = origin + direction.scalar(maxDistance.raw / length.raw)
    val rayBounds = AxisAlignedBoundingBox(
        min = SceneOffset(minOf(origin.x.raw, endpoint.x.raw).sceneUnit, minOf(origin.y.raw, endpoint.y.raw).sceneUnit),
        max = SceneOffset(maxOf(origin.x.raw, endpoint.x.raw).sceneUnit, maxOf(origin.y.raw, endpoint.y.raw).sceneUnit),
    )
    var nearest: RaycastHit? = null
    for (index in indices) {
        val mask = this[index]
        if (!mask.axisAlignedBoundingBox.isOverlapping(rayBounds)) {
            continue
        }
        val hit = mask.raycast(origin, direction, maxDistance) ?: continue
        if (nearest == null || hit.distance < nearest.distance) {
            nearest = hit
        }
    }
    return nearest
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
): RaycastHit? = (end - start).let { delta ->
    raycast(origin = start, direction = delta, maxDistance = delta.length())
}

private fun raycastCircle(
    circle: CircleCollisionMask,
    origin: SceneOffset,
    unitDirection: SceneOffset,
    maxDistance: SceneUnit,
): RaycastHit? {
    val originX = origin.x.raw
    val originY = origin.y.raw
    val directionX = unitDirection.x.raw
    val directionY = unitDirection.y.raw
    val centerX = circle.position.x.raw
    val centerY = circle.position.y.raw
    val toOriginX = originX - centerX
    val toOriginY = originY - centerY
    // Quadratic |origin + t * direction - center|^2 = radius^2, with the direction normalized so its
    // squared term is 1; the nearest root is the entry point.
    val halfB = toOriginX * directionX + toOriginY * directionY
    val c = toOriginX * toOriginX + toOriginY * toOriginY - circle.radius.raw * circle.radius.raw
    val discriminant = halfB * halfB - c
    if (discriminant < 0f) {
        return null
    }
    val entry = -halfB - sqrt(discriminant)
    if (entry < 0f || entry > maxDistance.raw) {
        return null
    }
    val pointX = originX + directionX * entry
    val pointY = originY + directionY * entry
    var normalX = pointX - centerX
    var normalY = pointY - centerY
    val normalLength = sqrt(normalX * normalX + normalY * normalY)
    if (normalLength > NORMAL_EPSILON) {
        normalX /= normalLength
        normalY /= normalLength
    } else {
        normalX = -directionX
        normalY = -directionY
    }
    return RaycastHit(
        mask = circle,
        point = SceneOffset(pointX.sceneUnit, pointY.sceneUnit),
        normal = SceneOffset(normalX.sceneUnit, normalY.sceneUnit),
        distance = entry.sceneUnit,
    )
}

private fun raycastPolygon(
    polygon: PolygonCollisionMask,
    origin: SceneOffset,
    unitDirection: SceneOffset,
    maxDistance: SceneUnit,
): RaycastHit? {
    val originX = origin.x.raw
    val originY = origin.y.raw
    val directionX = unitDirection.x.raw
    val directionY = unitDirection.y.raw
    val maximumDistance = maxDistance.raw
    var bestDistance = Float.MAX_VALUE
    var bestNormalX = 0f
    var bestNormalY = 0f
    var bestPointX = 0f
    var bestPointY = 0f
    for (index in polygon.vertices.indices) {
        val edgeStart = polygon.rotationMatrix.times(polygon.vertices[index]) + polygon.position
        val nextIndex = if (index + 1 == polygon.vertices.size) 0 else index + 1
        val edgeEnd = polygon.rotationMatrix.times(polygon.vertices[nextIndex]) + polygon.position
        val startX = edgeStart.x.raw
        val startY = edgeStart.y.raw
        val edgeX = edgeEnd.x.raw - startX
        val edgeY = edgeEnd.y.raw - startY
        // Solve origin + t * direction = edgeStart + u * edge using 2D cross products.
        val denominator = directionX * edgeY - directionY * edgeX
        if (denominator == 0f) {
            continue
        }
        val toStartX = startX - originX
        val toStartY = startY - originY
        val distance = (toStartX * edgeY - toStartY * edgeX) / denominator
        if (distance < 0f || distance > maximumDistance || distance >= bestDistance) {
            continue
        }
        val edgeFraction = (toStartX * directionY - toStartY * directionX) / denominator
        if (edgeFraction < 0f || edgeFraction > 1f) {
            continue
        }
        val edgeNormal = polygon.rotationMatrix.times(polygon.normals[index])
        val normalX = edgeNormal.x.raw
        val normalY = edgeNormal.y.raw
        // Skip edges whose outward normal points along the ray: those are exits, not entries.
        if (normalX * directionX + normalY * directionY >= 0f) {
            continue
        }
        bestDistance = distance
        bestNormalX = normalX
        bestNormalY = normalY
        bestPointX = originX + directionX * distance
        bestPointY = originY + directionY * distance
    }
    if (bestDistance == Float.MAX_VALUE) {
        return null
    }
    return RaycastHit(
        mask = polygon,
        point = SceneOffset(bestPointX.sceneUnit, bestPointY.sceneUnit),
        normal = SceneOffset(bestNormalX.sceneUnit, bestNormalY.sceneUnit),
        distance = bestDistance.sceneUnit,
    )
}

private const val NORMAL_EPSILON = 1e-4f
