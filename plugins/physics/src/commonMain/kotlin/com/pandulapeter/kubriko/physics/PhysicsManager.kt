package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TODO: Documentation
 */
sealed class PhysicsManager : Manager() {

    abstract val gravity: MutableStateFlow<SceneOffset>
    abstract val simulationSpeed: MutableStateFlow<Float>

    companion object {
        fun newInstance(
            initialGravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit),
            initialSimulationSpeed: Float = 1f,
        ): PhysicsManager = PhysicsManagerImpl(
            initialGravity = initialGravity,
            initialSimulationSpeed = initialSimulationSpeed,
        )
    }
}