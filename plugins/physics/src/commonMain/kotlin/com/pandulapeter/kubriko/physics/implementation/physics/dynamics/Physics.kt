package com.pandulapeter.kubriko.physics.implementation.physics.dynamics

import com.pandulapeter.kubriko.implementation.extensions.sceneUnit

/**
 * Settings class where all the constants are stored for the physics engine.
 */
// TODO: Should be moved to PhysicsManager.newInstance()
object Physics {
    val PenetrationAllowance = 0.01f.sceneUnit
    const val PENETRATION_CORRECTION = 0.5f
    const val BIAS_RELATIVE = 0.95f
    const val BIAS_ABSOLUTE = 0.01f
    const val ITERATIONS = 1
    val Epsilon = 1.sceneUnit
}