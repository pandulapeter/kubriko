/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.rays

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.physics.PhysicsBody
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
            if (body.collisionMask.isSceneOffsetInside(startPoint)) {
                rayData.clear()
                break
            }
            when (body.physicalShape.collisionMask) {
                is CircleCollisionMask -> {
                    val circle = body.physicalShape.collisionMask
                    val d = body.position.minus(startPoint)
                    val angle = asin((circle.radius / d.length()).raw)
                    val u = RotationMatrix(angle.rad)
                    projectRays(u.times(d.normalized()), bodiesToEvaluate)
                    val u2 = RotationMatrix(-angle.rad)
                    projectRays(u2.times(d.normalized()), bodiesToEvaluate)
                }

                is PolygonCollisionMask -> {
                    val polygon = body.physicalShape.collisionMask
                    for (v in polygon.vertices) {
                        val direction = polygon.rotationMatrix.times(v).plus(body.position).minus(startPoint)
                        projectRays(direction, bodiesToEvaluate)
                    }
                }
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
        val m = RotationMatrix(0.001f.rad)
        m.transpose().times(direction)
        for (i in 0..2) {
            val ray = Ray(startPoint, direction, distance)
            ray.updateProjection(bodiesToEvaluate)
            rayData.add(RayAngleInformation(ray, atan2(direction.y.raw.toDouble(), direction.x.raw.toDouble())))
            m.times(direction)
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