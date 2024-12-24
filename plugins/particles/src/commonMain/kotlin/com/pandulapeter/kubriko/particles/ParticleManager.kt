package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.particles.implementation.ParticleManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class ParticleManager : Manager() {

    companion object {
        fun newInstance(): ParticleManager = ParticleManagerImpl()
    }
}