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

import com.pandulapeter.kubriko.collision.implementation.Mat2
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalize
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.physics.implementation.dynamics.PhysicsBody
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.helpers.isPointInside
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.asin
import kotlin.math.atan2

/**
 * A class for generating polygons that can mimic line of sight around objects and cast shadows.
 */
internal class ShadowCasting(var startPoint: SceneOffset, private val distance: SceneUnit) {

    val rayData = mutableListOf<RayAngleInformation>()

    /**
     * Updates the all projections in world space and acquires information about all intersecting rays.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjections(bodiesToEvaluate: List<PhysicsBody>) {
        rayData.clear()
        for (body in bodiesToEvaluate) {
            if (isPointInside(body, startPoint)) {
                rayData.clear()
                break
            }
            if (body.shape is Polygon) {
                val poly1 = body.shape as Polygon
                for (v in poly1.vertices) {
                    val direction = poly1.orientation.mul(v).plus(body.position).minus(startPoint)
                    projectRays(direction, bodiesToEvaluate)
                }
            } else {
                val circle = body.shape as Circle
                val d = body.position.minus(startPoint)
                val angle = asin((circle.radius / d.length()).raw)
                val u = Mat2(angle.rad)
                projectRays(u.mul(d.normalize()), bodiesToEvaluate)
                val u2 = Mat2(-angle.rad)
                projectRays(u2.mul(d.normalize()), bodiesToEvaluate)
            }
        }
        rayData.sortWith { lhs: RayAngleInformation, rhs: RayAngleInformation ->
            rhs.angle.compareTo(lhs.angle)
        }
    }

    /**
     * Projects a ray and evaluates it against all objects supplied in world space.
     *
     * @param direction        Direction of ray to project.
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    private fun projectRays(direction: SceneOffset, bodiesToEvaluate: List<PhysicsBody>) {
        val m = Mat2(0.001f.rad)
        m.transpose().mul(direction)
        for (i in 0..2) {
            val ray = Ray(startPoint, direction, distance)
            ray.updateProjection(bodiesToEvaluate)
            rayData.add(RayAngleInformation(ray, atan2(direction.y.raw.toDouble(), direction.x.raw.toDouble())))
            m.mul(direction)
        }
    }

    /**
     * Getter for number of rays projected.
     *
     * @return Returns size of raydata.
     */
    val noOfRays: Int
        get() = rayData.size
}