package com.pandulapeter.kubriko.physics.implementation.physics.explosions

import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.physics.implementation.physics.rays.Ray
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models rayscatter explosions.
 */
class RayScatter(epicentre: Vec2, private val noOfRays: Int) {
    /**
     * Getter for rays.
     *
     * @return Array of all rays part of the ray scatter.
     */
    val rays = mutableListOf<Ray>()
    var epicentre: Vec2 = epicentre
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
        val angle = AngleRadians.TwoPi / noOfRays
        var direction = Vec2(SceneUnit.Unit, SceneUnit.Unit)
        val u = Mat2(angle)
        for (i in rays.indices) {
            rays.add(Ray(epicentre, direction, distance))
            direction = u.mulV2(direction)
        }
    }

    /**
     * Updates all rays.
     *
     * @param worldBodies Arraylist of all bodies to update ray projections for.
     */
    fun updateRays(worldBodies: ArrayList<TranslatableBody>) {
        for (ray in rays) {
            ray.updateProjection(worldBodies)
        }
    }
}