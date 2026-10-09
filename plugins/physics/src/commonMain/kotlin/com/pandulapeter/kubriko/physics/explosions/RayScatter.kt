/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.explosions

import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.rays.Ray
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models rayscatter explosions.
 *
 * @param epicenter The point the rays are cast from.
 * @param noOfRays  The number of rays [castRays] spreads evenly around the epicenter.
 */
class RayScatter(epicenter: SceneOffset, private val noOfRays: Int) {
    /**
     * Getter for rays.
     *
     * @return Array of all rays part of the ray scatter.
     */
    internal val rays = mutableListOf<Ray>()

    /**
     * The point the rays are cast from. Setting it moves the start point of every ray already cast.
     */
    var epicenter: SceneOffset = epicenter
        set(value) {
            field = value
            for (ray in rays) {
                ray.startPoint = field
            }
        }

    /**
     * Casts rays in 360 degrees with equal spacing.
     *
     * @param distance Distance of projected rays.
     */
    fun castRays(distance: SceneUnit) {
        val angle = 6.28319f / noOfRays
        var direction = SceneOffset.UpRight
        val u = RotationMatrix(angle.rad)
        rays.clear()
        repeat(noOfRays) {
            rays.add(Ray(epicenter, direction, distance))
            direction = u.times(direction)
        }
    }

    /**
     * Updates all rays.
     *
     * @param worldBodies Arraylist of all bodies to update ray projections for.
     */
    fun updateRays(worldBodies: Collection<PhysicsBody>) {
        for (ray in rays) {
            ray.updateProjection(worldBodies)
        }
    }
}