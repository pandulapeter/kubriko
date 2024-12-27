package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class ParticleManager : Manager() {

    companion object {
        fun newInstance(): ParticleManager = ParticleManagerImpl()
    }
}