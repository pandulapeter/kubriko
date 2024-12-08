package com.pandulapeter.kubriko.physics.implementation.physics.dynamics

/**
 * Settings class where all the constants are stored for the physics engine.
 */
// TODO: Should be moved to PhysicsManager.newInstance()
object Physics {
    const val PENETRATION_ALLOWANCE = 0.01f // TODO: SceneUnit
    const val PENETRATION_CORRECTION = 0.5f
    const val BIAS_RELATIVE = 0.95f
    const val BIAS_ABSOLUTE = 0.01f
    const val ITERATIONS = 100
    const val EPSILON = 1E-12 // TODO: SceneUnit
}