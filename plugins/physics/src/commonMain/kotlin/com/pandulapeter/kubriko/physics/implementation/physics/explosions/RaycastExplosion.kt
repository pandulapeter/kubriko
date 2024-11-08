package com.pandulapeter.kubriko.physics.implementation.physics.explosions

import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.physics.implementation.physics.rays.RayInformation

/**
 * Models raycast explosions.
 *
 * @param epicentre   The epicentre of the explosion.
 * @param noOfRays    Number of projected rays.
 * @param distance    Distance of projected rays.
 * @param worldBodies The world the rays effect and are projected in.
 */
class RaycastExplosion(epicentre: Vec2, noOfRays: Int, distance: Double, worldBodies: ArrayList<TranslatableBody>) : Explosion {
    val rayScatter: RayScatter

    /**
     * Sets the epicentre to a different coordinate.
     *
     * @param v The vector position of the new epicentre.
     */
    override fun setEpicentre(v: Vec2) {
        rayScatter.epicentre = v
    }

    private var raysInContact = ArrayList<RayInformation>()

    init {
        rayScatter = RayScatter(epicentre, noOfRays)
        rayScatter.castRays(distance)
        update(worldBodies)
    }

    /**
     * Updates the arraylist to reevaluate what objects are effected/within the proximity.
     *
     * @param bodiesToEvaluate Arraylist of bodies in the world to check.
     */
    override fun update(bodiesToEvaluate: ArrayList<TranslatableBody>) {
        raysInContact.clear()
        rayScatter.updateRays(bodiesToEvaluate)
        val rayArray = rayScatter.rays
        for (ray in rayArray) {
            val rayInfo = ray.rayInformation
            if (rayInfo != null) {
                raysInContact.add(rayInfo)
            }
        }
    }

    /**
     * Applies a blast impulse to the effected bodies.
     *
     * @param blastPower The impulse magnitude.
     */
    override fun applyBlastImpulse(blastPower: Double) {
        for (ray in raysInContact) {
            val blastDir = ray.coordinates.minus(rayScatter.epicentre)
            val distance = blastDir.length()
            if (distance == 0.0) return
            val invDistance = 1 / distance
            val impulseMag = blastDir.normalize().scalar(blastPower * invDistance)
            val b = ray.b
            if (b !is PhysicalBodyInterface) continue
            b.applyLinearImpulse(impulseMag, ray.coordinates.minus(b.position))
        }
    }
}