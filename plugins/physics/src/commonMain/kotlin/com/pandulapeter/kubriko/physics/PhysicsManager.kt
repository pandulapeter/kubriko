package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TODO: Documentation
 */
sealed class PhysicsManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    abstract val gravity: MutableStateFlow<SceneOffset>
    abstract val simulationSpeed: MutableStateFlow<Float>

    companion object {
        fun newInstance(
            initialGravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit),
            initialSimulationSpeed: Float = 1f,
            isLoggingEnabled: Boolean = false,
        ): PhysicsManager = PhysicsManagerImpl(
            initialGravity = initialGravity,
            initialSimulationSpeed = initialSimulationSpeed,
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}