package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.actor.Actor

interface ParticleEmitter : Actor {

    var particleEmissionMode: EmissionMode
    val particleEmissionRate: Float

    fun createParticle(): Particle

    enum class EmissionMode {
        CONTINUOUS,
        BURST_INACTIVE,
        BURST_ACTIVE;
    }
}