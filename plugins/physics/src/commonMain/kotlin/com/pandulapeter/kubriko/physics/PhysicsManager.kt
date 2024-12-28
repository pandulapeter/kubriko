package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * TODO: Documentation
 */
sealed class PhysicsManager : Manager() {

    open var gravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit)
    var simulationSpeed: Float = 1f

    companion object {
        fun newInstance(
            initialGravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit),
            initialSimulationSpeed: Float = 1f,
        ): PhysicsManager = PhysicsManagerImpl(
        ).apply {
            gravity = initialGravity
            simulationSpeed = initialSimulationSpeed
        }
    }
}