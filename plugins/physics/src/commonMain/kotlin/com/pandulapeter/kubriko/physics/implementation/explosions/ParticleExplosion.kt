/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.explosions

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models particle explosions.
 *
 * @param epicentre     Vector location of explosion epicenter.
 * @param noOfParticles Total number of particles the explosion has.
 * @param lifespan          The life time of the particle.
 */
class ParticleExplosion(private val epicentre: Vec2, private val noOfParticles: Int, private val lifespan: Float) {
    /**
     * Getter to return the list of particles in the world.
     *
     * @return Array of bodies.
     */
    val particles = MutableList(noOfParticles) { Body(Circle(SceneUnit.Zero), SceneUnit.Zero, SceneUnit.Zero) }

    /**
     * Creates particles in the supplied world.
     *
     * @param size    The size of the particles.
     * @param density The density of the particles.
     * @param radius  The distance away from the epicenter the particles are placed.
     * @param world   The world the particles are created in.
     */
    fun createParticles(size: SceneUnit, density: Int, radius: SceneUnit) {
        val separationAngle = AngleRadians.TwoPi / noOfParticles
        var distanceFromCentre = Vec2(0.sceneUnit, radius)
        val rotate = Mat2(separationAngle)
        for (i in 0 until noOfParticles) {
            val particlePlacement = epicentre.plus(distanceFromCentre)
            val b = Body(Circle(size), particlePlacement.x, particlePlacement.y)
            b.density = density.toFloat()
            b.restitution = 1f
            b.staticFriction = 0f
            b.dynamicFriction = 0f
            b.affectedByGravity = false
            b.linearDampening = 0f
            b.particle = true
            //TODO: world.addBody(b)
            particles[i] = b
            distanceFromCentre = rotate.mul(distanceFromCentre)
        }
    }

    /**
     * Applies a blast impulse to all particles created.
     *
     * @param blastPower The impulse magnitude.
     */
    fun applyBlastImpulse(blastPower: Float) {
        var line: Vec2
        for (b in particles) {
            line = b.position.minus(epicentre)
            b.velocity.set(line.scalar(blastPower))
        }
    }
}