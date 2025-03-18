/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.explosions

import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalize
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.helpers.toVec2
import com.pandulapeter.kubriko.physics.implementation.rays.RayInformation
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models raycast explosions.
 *
 * @param epicenter   The epicenter of the explosion.
 * @param noOfRays    Number of projected rays.
 * @param distance    Distance of projected rays.
 * @param worldBodies The world the rays effect and are projected in.
 */
class RaycastExplosion(
    epicenter: SceneOffset,
    noOfRays: Int,
    distance: SceneUnit,
    worldBodies: List<TranslatableBody>,
) : Explosion {
    val rayScatter: RayScatter = RayScatter(epicenter, noOfRays)

    /**
     * Sets the epicenter to a different coordinate.
     *
     * @param epicenter The vector position of the new epicenter.
     */
    override fun setEpicenter(epicenter: SceneOffset) {
        rayScatter.epicenter = epicenter
    }

    private var raysInContact = ArrayList<RayInformation>()

    init {
        rayScatter.castRays(distance)
        update(worldBodies)
    }

    /**
     * Updates the arraylist to reevaluate what objects are effected/within the proximity.
     *
     * @param bodiesToEvaluate Arraylist of bodies in the world to check.
     */
    override fun update(bodiesToEvaluate: Collection<TranslatableBody>) {
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
    override fun applyBlastImpulse(blastPower: SceneUnit) {
        for (ray in raysInContact) {
            val blastDir = ray.coordinates.minus(rayScatter.epicenter)
            val distance = blastDir.length()
            if (distance == SceneUnit.Zero) return
            val invDistance = 1f / distance.raw
            val impulseMag = blastDir.normalize().scalar(blastPower * invDistance)
            val b = ray.b
            if (b !is PhysicalBodyInterface) continue
            b.applyLinearImpulse(impulseMag.toVec2(), ray.coordinates.minus(b.position.toSceneOffset()).toVec2())
        }
    }
}