package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.physics.implementation.PhysicsManagerImpl
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * TODO: Documentation
 */
abstract class PhysicsManager : Manager() {

    companion object {
        fun newInstance(
            gravity: SceneOffset = SceneOffset(0f.scenePixel, 9.81f.scenePixel),
        ): PhysicsManager = PhysicsManagerImpl(
            gravity = gravity,
        )
    }
}