/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.geometry

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.collision.implementation.Vec2
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Circle class to create a circle object.
 */
class Circle(
    var radius: SceneUnit,
) : Shape() {

    override fun calcMass(density: Float) {
        val physicalBody = this.body
        if (physicalBody !is PhysicalBodyInterface) return
        physicalBody.mass = PI.toFloat() * radius.raw * radius.raw * density
        physicalBody.invMass = if (physicalBody.mass != 0f) 1.0f / physicalBody.mass else 0f
        physicalBody.inertia = physicalBody.mass * radius.raw * radius.raw
        physicalBody.invInertia = if (physicalBody.inertia != 0f) 1.0f / physicalBody.inertia else 0f
    }

    override fun createAABB() {
        this.body.aabb = AxisAlignedBoundingBox(
            min = SceneOffset(-radius, -radius),
            max = SceneOffset(radius, radius),
        )
    }

    override fun isPointInside(startPoint: Vec2) = (body.position - startPoint).length() <= radius

    override fun rayIntersect(startPoint: Vec2, endPoint: Vec2, maxDistance: SceneUnit, rayLength: SceneUnit): IntersectionReturnElement {
        var minPx = SceneUnit.Zero
        var minPy = SceneUnit.Zero
        var intersectionFound = false
        var closestBody: TranslatableBody? = null
        var maxD = maxDistance

        val ray = endPoint.copy() - startPoint
        val circleCenter = body.position.copy()
        val r = radius
        val difInCenters = startPoint - circleCenter
        val a = ray.dot(ray)
        val b = difInCenters.dot(ray) * 2
        val c = difInCenters.dot(difInCenters) - r * r
        var discriminant = b * b - a * c * 4
        if (discriminant >= SceneUnit.Zero) {
            discriminant = sqrt(discriminant.raw).sceneUnit
            val t1 = (-b - discriminant) / (a * 2)
            if (t1.raw in 0.0..1.0) {
                if (t1 < maxDistance) {
                    maxD = t1
                    minPx = startPoint.x + endPoint.x * t1
                    minPy = startPoint.y + endPoint.y * t1
                    intersectionFound = true
                    closestBody = body
                }
            }
        }
        return IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
    }
}