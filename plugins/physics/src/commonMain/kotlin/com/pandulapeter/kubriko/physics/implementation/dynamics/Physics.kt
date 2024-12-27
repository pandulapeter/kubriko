package com.pandulapeter.kubriko.physics.implementation.dynamics

import com.pandulapeter.kubriko.extensions.sceneUnit

/**
 * Settings class where all the constants are stored for the physics engine.
 */
// TODO: Should be moved to PhysicsManager.newInstance()
object Physics {
    val PenetrationAllowance = 0.sceneUnit
    const val PENETRATION_CORRECTION = 0.2f
    const val BIAS_RELATIVE = 0.95f
    const val BIAS_ABSOLUTE = 0.01f
}