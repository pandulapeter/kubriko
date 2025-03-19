/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.PI
import kotlin.math.sqrt

// TODO: Merge with PhysicsBody
class PhysicalShape(
    val collisionMask: ComplexCollisionMask,
) {
    internal val rotationMatrix = RotationMatrix()

    fun calculateMass(
        density: Float,
        physicalBody: PhysicsBody,
    ) = when (collisionMask) {
        is CircleCollisionMask -> {
            physicalBody.mass = PI.toFloat() * collisionMask.radius.raw * collisionMask.radius.raw * density
            physicalBody.invMass = if (physicalBody.mass != 0f) 1.0f / physicalBody.mass else 0f
            physicalBody.inertia = physicalBody.mass * collisionMask.radius.raw * collisionMask.radius.raw
            physicalBody.invInertia = if (physicalBody.inertia != 0f) 1.0f / physicalBody.inertia else 0f
        }

        is PolygonCollisionMask -> {
            var area = SceneUnit.Companion.Zero
            var inertia = SceneUnit.Companion.Zero
            val k = 1f / 3f
            for (i in collisionMask.vertices.indices) {
                val point1 = collisionMask.vertices[i]
                val point2 = collisionMask.vertices[(i + 1) % collisionMask.vertices.size]
                val areaOfParallelogram = point1.cross(point2)
                val triangleArea = areaOfParallelogram * 0.5f
                area += triangleArea
                val intx2 = point1.x * point1.x + point2.x * point1.x + point2.x * point2.x
                val inty2 = point1.y * point1.y + point2.y * point1.y + point2.y * point2.y
                inertia += areaOfParallelogram * (intx2.raw + inty2.raw) * 0.25f * k
            }
            physicalBody.mass = density * area.raw
            physicalBody.invMass = if (physicalBody.mass != 0f) 1f / physicalBody.mass else 0f
            physicalBody.inertia = inertia.raw * density
            physicalBody.invInertia = if (physicalBody.inertia != 0f) 1f / physicalBody.inertia else 0f
        }
    }


    fun rayIntersect(
        startPoint: SceneOffset,
        endPoint: SceneOffset,
        maxDistance: SceneUnit,
        physicalBody: PhysicsBody,
    ): IntersectionReturnElement = when (collisionMask) {
        is CircleCollisionMask -> {
            var minPx = SceneUnit.Companion.Zero
            var minPy = SceneUnit.Companion.Zero
            var intersectionFound = false
            var closestBody: PhysicsBody? = null
            var maxD = maxDistance

            val ray = endPoint - startPoint
            val circleCenter = physicalBody.position
            val r = collisionMask.radius
            val difInCenters = startPoint - circleCenter
            val a = ray.dot(ray)
            val b = difInCenters.dot(ray) * 2
            val c = difInCenters.dot(difInCenters) - r * r
            var discriminant = b * b - a * c * 4
            if (discriminant >= SceneUnit.Companion.Zero) {
                discriminant = sqrt(discriminant.raw).sceneUnit
                val t1 = (-b - discriminant) / (a * 2)
                if (t1.raw in 0.0..1.0) {
                    if (t1 < maxDistance) {
                        maxD = t1
                        minPx = startPoint.x + endPoint.x * t1
                        minPy = startPoint.y + endPoint.y * t1
                        intersectionFound = true
                        closestBody = physicalBody
                    }
                }
            }
            IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
        }

        is PolygonCollisionMask -> {
            var minPx = SceneUnit.Companion.Zero
            var minPy = SceneUnit.Companion.Zero
            var intersectionFound = false
            var closestBody: PhysicsBody? = null
            var maxD = maxDistance

            for (i in collisionMask.vertices.indices) {
                var startOfPolyEdge = collisionMask.vertices[i]
                var endOfPolyEdge = collisionMask.vertices[if (i + 1 == collisionMask.vertices.size) 0 else i + 1]
                startOfPolyEdge = rotationMatrix.times(startOfPolyEdge) + physicalBody.position
                endOfPolyEdge = rotationMatrix.times(endOfPolyEdge) + physicalBody.position

                //detect if line (startPoint -> endpoint) intersects with the current edge (startOfPolyEdge -> endOfPolyEdge)
                val intersection = lineIntersect(startPoint, endPoint, startOfPolyEdge, endOfPolyEdge)
                if (intersection != null) {
                    val distance = startPoint.distanceTo(intersection)
                    if (isPointOnLine(startPoint, endPoint, intersection) && isPointOnLine(
                            startOfPolyEdge,
                            endOfPolyEdge,
                            intersection
                        ) && distance < maxD
                    ) {
                        maxD = distance
                        minPx = intersection.x
                        minPy = intersection.y
                        intersectionFound = true
                        closestBody = physicalBody
                    }
                }
            }
            IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
        }
    }

    class IntersectionReturnElement(
        val minPx: SceneUnit,
        val minPy: SceneUnit,
        val intersectionFound: Boolean,
        val closestBody: PhysicsBody?,
        val maxDistance: SceneUnit
    )
}