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

import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models proximity explosions.
 */
class ProximityExplosion(
    private var epicenter: SceneOffset,
    val proximity: SceneUnit,
) : Explosion {
    /**
     * Sets the epicenter to a different coordinate.
     *
     * @param epicenter The vector position of the new epicenter.
     */
    override fun setEpicenter(epicenter: SceneOffset) {
        this.epicenter = epicenter
    }

    private var bodiesEffected = mutableListOf<PhysicsBody>()

    /**
     * Updates the arraylist to reevaluate what bodies are effected/within the proximity.
     *
     * @param bodiesToEvaluate Arraylist of bodies in the world to check.
     */
    override fun update(bodiesToEvaluate: Collection<PhysicsBody>) {
        bodiesEffected.clear()
        for (b in bodiesToEvaluate) {
            val blastDist = b.position - epicenter
            if (blastDist.length() <= proximity) {
                bodiesEffected.add(b)
            }
        }
    }

    val linesToBodies = mutableListOf<SceneOffset>()

    /**
     * Updates the lines to body array for the debug drawer.
     */
    fun updateLinesToBody() {
        linesToBodies.clear()
        for (b in bodiesEffected) {
            linesToBodies.add(b.position)
        }
    }

    /**
     * Applies blast impulse to all effected bodies center of mass.
     *
     * @param blastPower Blast magnitude.
     */
    override fun applyBlastImpulse(blastPower: SceneUnit) {
        for (b in bodiesEffected) {
            val blastDir = b.position - epicenter
            val distance = blastDir.length()
            if (distance == SceneUnit.Zero) return

            //Not physically correct as it should be blast * radius to object ^ 2 as the pressure of an explosion in 2D dissipates
            val invDistance = SceneUnit.Unit / distance
            val impulseMag = blastPower * invDistance
            b.applyLinearImpulse(blastDir.normalized().scalar(impulseMag))
        }
    }
}