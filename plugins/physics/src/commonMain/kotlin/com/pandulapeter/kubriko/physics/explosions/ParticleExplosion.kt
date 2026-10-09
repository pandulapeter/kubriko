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
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Models particle explosions.
 *
 * @param epicenter     Vector location of explosion epicenter.
 * @param noOfParticles Total number of particles the explosion has.
 * @param lifespan      Currently unused (kept for signature compatibility).
 */
class ParticleExplosion(private val epicenter: SceneOffset, private val noOfParticles: Int, private val lifespan: Float) {
    /**
     * Getter to return the list of particles in the world.
     *
     * @return Array of bodies.
     */
    val particles = MutableList(noOfParticles) { PhysicsBody(CircleCollisionMask()) }

    /**
     * Creates the particles around the epicenter. They are not added to any simulation: the caller adds them
     * (e.g. through [RigidBody][com.pandulapeter.kubriko.physics.RigidBody] actors).
     *
     * @param size    The size of the particles.
     * @param density The density of the particles.
     * @param radius  The distance away from the epicenter the particles are placed.
     */
    fun createParticles(size: SceneUnit, density: Int, radius: SceneUnit) {
        val separationAngle = AngleRadians.TwoPi / noOfParticles
        var distanceFromCentre = SceneOffset(0.sceneUnit, radius)
        val rotate = RotationMatrix(separationAngle)
        for (i in 0 until noOfParticles) {
            val particlePlacement = epicenter + distanceFromCentre
            val b = PhysicsBody(
                CircleCollisionMask(
                    initialRadius = size,
                    initialPosition = particlePlacement,
                )
            )
            b.density = density.toFloat()
            b.restitution = 1f
            b.staticFriction = 0f
            b.dynamicFriction = 0f
            b.isAffectedByGravity = false
            b.linearDampening = 0f
            b.isParticle = true
            particles[i] = b
            distanceFromCentre = rotate.times(distanceFromCentre)
        }
    }

    /**
     * Applies a blast impulse to all particles created.
     *
     * @param blastPower The impulse magnitude.
     */
    fun applyBlastImpulse(blastPower: Float) {
        var line: SceneOffset
        for (b in particles) {
            line = b.position - epicenter
            b.velocity = line.scalar(blastPower)
        }
    }
}