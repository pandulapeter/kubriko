/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.isPointOnLine
import com.pandulapeter.kubriko.physics.implementation.lineIntersect
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.PI
import kotlin.math.sqrt

class PhysicsBody(
    val collisionMask: ComplexCollisionMask,
    var position: SceneOffset = collisionMask.position,
    var dynamicFriction: Float = 0.2f,
    var staticFriction: Float = 0.5f,
    rotation: AngleRadians = AngleRadians.Zero,
    velocity: SceneOffset = SceneOffset.Zero,
    force: SceneOffset = SceneOffset.Zero,
    density: Float = 1f,
    torque: SceneUnit = SceneUnit.Zero,
    var restitution: Float = 0.8f,
    angularVelocity: SceneUnit = SceneUnit.Zero,
    var linearDampening: Float = 0f,
    var isAffectedByGravity: Boolean = true,
) {
    internal val rotationMatrix = RotationMatrix()
    var rotation = rotation
        set(value) {
            field = value
            rotationMatrix.set(rotation)
        }
    var velocity = velocity
        set(value) {
            // TODO: Make the minimum velocity configurable
            field = if (abs(value.length()) < 0.1f.sceneUnit) SceneOffset.Zero else value
        }
    var force = force
        set(value) {
            // TODO: Make the minimum force configurable
            field = if (abs(value.length()) < 0.1f.sceneUnit) SceneOffset.Zero else value
        }
    var angularVelocity = angularVelocity
        set(value) {
            // TODO: Make the minimum angular velocity configurable
            field = if (abs(value) < 0.01f.sceneUnit) SceneUnit.Zero else value
        }
    var torque = torque
        set(value) {
            // TODO: Make the minimum torque configurable
            field = if (abs(value) < 0.1f.sceneUnit) SceneUnit.Zero else value
        }
    var density = density
        set(value) {
            field = value
            if (density == 0f) {
                setStatic()
            } else if (true) {
                calculateMass(value)
            }
        }
    internal var mass = 0f
        private set
    internal var invMass = 0f
        private set
    internal var inertia = 0f
        private set(value) {
            field = if (value < 0.05f) 0f else value
        }
    internal var invInertia = 0f
        private set
    internal var isParticle = false

    init {
        rotationMatrix.set(rotation)
        this.density = density
        this.velocity = velocity
        this.force = force
        this.angularVelocity = angularVelocity
        this.torque = torque
    }

    /**
     * Applies force ot body.
     *
     * @param force        Force vector to apply.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyForce(force: SceneOffset, contactPoint: SceneOffset) {
        this.force += force
        torque += contactPoint.cross(force)
    }

    /**
     * Apply force to the center of mass.
     *
     * @param force Force vector to apply.
     */
    fun applyForce(force: SceneOffset) {
        this.force += force
    }

    /**
     * Applies impulse to a point relative to the body's center of mass.
     *
     * @param impulse      Magnitude of impulse vector.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyLinearImpulse(impulse: SceneOffset, contactPoint: SceneOffset) {
        velocity += impulse.scalar(invMass)
        angularVelocity += contactPoint.cross(impulse) * invInertia
    }

    /**
     * Applies impulse to body's center of mass.
     *
     * @param impulse Magnitude of impulse vector.
     */
    fun applyLinearImpulse(impulse: SceneOffset) {
        if (density > 0f) {
            velocity += impulse.scalar(invMass)
        }
    }

    /**
     * Sets all mass and inertia variables to zero. Object cannot be moved.
     */
    fun setStatic() {
        mass = 0f
        invMass = 0f
        inertia = 0f
        invInertia = 0f
    }

    private fun calculateMass(density: Float) = when (collisionMask) {
        is CircleCollisionMask -> {
            mass = PI.toFloat() * collisionMask.radius.raw * collisionMask.radius.raw * density
            invMass = if (mass != 0f) 1.0f / mass else 0f
            inertia = mass * collisionMask.radius.raw * collisionMask.radius.raw
            invInertia = if (inertia != 0f) 1.0f / inertia else 0f
        }

        is PolygonCollisionMask -> {
            var area = SceneUnit.Zero
            var inertia = SceneUnit.Zero
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
            mass = density * area.raw
            invMass = if (mass != 0f) 1f / mass else 0f
            this.inertia = inertia.raw * density
            invInertia = if (this.inertia != 0f) 1f / this.inertia else 0f
        }
    }


    internal fun rayIntersect(
        startPoint: SceneOffset,
        endPoint: SceneOffset,
        maxDistance: SceneUnit,
        physicalBody: PhysicsBody,
    ): IntersectionReturnElement = when (collisionMask) {
        is CircleCollisionMask -> {
            var minPx = SceneUnit.Zero
            var minPy = SceneUnit.Zero
            var intersectionFound = false
            var closestBody: PhysicsBody? = null
            var maxD = maxDistance

            val ray = endPoint - startPoint
            val circleCenter = position
            val r = collisionMask.radius
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
                        closestBody = physicalBody
                    }
                }
            }
            IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
        }

        is PolygonCollisionMask -> {
            var minPx = SceneUnit.Zero
            var minPy = SceneUnit.Zero
            var intersectionFound = false
            var closestBody: PhysicsBody? = null
            var maxD = maxDistance

            for (i in collisionMask.vertices.indices) {
                var startOfPolyEdge = collisionMask.vertices[i]
                var endOfPolyEdge = collisionMask.vertices[if (i + 1 == collisionMask.vertices.size) 0 else i + 1]
                startOfPolyEdge = rotationMatrix.times(startOfPolyEdge) + position
                endOfPolyEdge = rotationMatrix.times(endOfPolyEdge) + position

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

    internal class IntersectionReturnElement(
        val minPx: SceneUnit,
        val minPy: SceneUnit,
        val intersectionFound: Boolean,
        val closestBody: PhysicsBody?,
        val maxDistance: SceneUnit
    )
}