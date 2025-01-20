package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter : Actor {

    val isEmittingParticles: Boolean
    val particleEmissionRate: Float

    fun createParticle(): Particle
}