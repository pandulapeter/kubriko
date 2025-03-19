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

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Abstract class presenting a geometric shape.
 */
// TODO: Should be merged with CollisionMask
abstract class Shape {
    internal lateinit var body: PhysicsBody
    internal val rotationMatrix = RotationMatrix()

    /**
     * Calculates the mass of a shape.
     *
     * @param density The desired density to factor into the calculation.
     */
    abstract fun calcMass(density: Float)

    /**
     * Generates an AABB for the shape.
     */
    abstract fun createAABB()

    abstract fun isPointInside(startPoint: SceneOffset): Boolean

    /**
     * Checks if a ray intersects with the shape.
     *
     * @param startPoint The start point of the ray.
     * @param endPoint The end point of the ray.
     * @param maxDistance The ray information.
     * @param rayInformation The object to store the information in.
     * @return Float Returns the distance to the intersection point. maxDistance if no intersection was found.
     */
    abstract fun rayIntersect(startPoint: SceneOffset, endPoint: SceneOffset, maxDistance: SceneUnit, rayLength: SceneUnit): IntersectionReturnElement

    class IntersectionReturnElement(
        val minPx: SceneUnit,
        val minPy: SceneUnit,
        val intersectionFound: Boolean,
        val closestBody: PhysicsBody?,
        val maxDistance: SceneUnit
    )
}