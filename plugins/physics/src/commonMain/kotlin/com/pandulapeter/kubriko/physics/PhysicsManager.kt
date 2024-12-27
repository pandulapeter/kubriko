package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * TODO: Documentation
 */
sealed class PhysicsManager : Manager() {

    companion object {
        fun newInstance(
            gravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit),
            simulationSpeed: Float = 1f,
        ): PhysicsManager = PhysicsManagerImpl(
            gravity = gravity,
            simulationSpeed = simulationSpeed,
        )
    }
}