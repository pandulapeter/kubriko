/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.rays

import com.pandulapeter.kubriko.collision.implementation.Vec2
import com.pandulapeter.kubriko.helpers.extensions.normalize
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Ray class to define and project rays in a world.
 *
 * @param startPoint The origin of the rays projection.
 * @param direction  The direction of the ray points in radians.
 * @param distance   The distance the ray is projected
 */
internal class Ray(
    var startPoint: SceneOffset,
    direction: SceneOffset,
    val distance: SceneUnit,
) {
    var direction: SceneOffset = direction.normalize()

    /**
     * Convenience constructor with ray set at origin. Similar to
     * [.Ray]
     *
     * @param direction The direction of the ray points in radians.
     * @param distance  The distance the ray is projected
     */
    constructor(direction: AngleRadians, distance: SceneUnit) : this(SceneOffset.Companion.Zero, Vec2(direction).toSceneOffset(), distance)

    /**
     * Convenience constructor with ray set at origin. Similar to
     * [.Ray]
     *
     * @param direction The direction of the ray points.
     * @param distance  The distance the ray is projected
     */
    constructor(direction: SceneOffset, distance: SceneUnit) : this(SceneOffset.Companion.Zero, direction, distance)

    /**
     * Convenience constructor. Similar to
     * [.Ray]
     *
     * @param startPoint The origin of the rays projection.
     * @param direction  The direction of the ray points in radians.
     * @param distance   The distance the ray is projected
     */
    constructor(startPoint: SceneOffset, direction: AngleRadians, distance: SceneUnit) : this(
        startPoint,
        Vec2(direction).toSceneOffset(),
        distance
    )

    var rayInformation: RayInformation? = null
        private set

    /**
     * Updates the projection in world space and acquires information about the closest intersecting object with the ray projection.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjection(bodiesToEvaluate: Collection<PhysicsBody>) {
        rayInformation = null
        val endPoint = direction.scalar(distance).plus(startPoint)
        var minT1 = Float.POSITIVE_INFINITY.sceneUnit
        var minPx = SceneUnit.Companion.Zero
        var minPy = SceneUnit.Companion.Zero
        var intersectionFound = false
        var closestBody: PhysicsBody? = null
        for (body in bodiesToEvaluate) {
            val shape = body.shape
            val intersectionReturnElement = shape.rayIntersect(startPoint, endPoint, minT1, distance)
            if (intersectionReturnElement.intersectionFound) {
                minT1 = intersectionReturnElement.maxDistance
                minPx = intersectionReturnElement.minPx
                minPy = intersectionReturnElement.minPy
                intersectionFound = true
                closestBody = intersectionReturnElement.closestBody
            }
        }
        if (intersectionFound) {
            rayInformation = closestBody?.let { RayInformation(it, minPx, minPy, -1) }
        }
    }
}