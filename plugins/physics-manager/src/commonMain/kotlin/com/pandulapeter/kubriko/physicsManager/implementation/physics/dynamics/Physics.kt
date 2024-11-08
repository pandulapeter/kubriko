package com.pandulapeter.kubriko.physicsManager.implementation.physics.dynamics

/**
 * Settings class where all the constants are stored for the physics engine.
 */
object Physics {
    const val PENETRATION_ALLOWANCE = 0.01
    const val PENETRATION_CORRECTION = 0.5
    const val BIAS_RELATIVE = 0.95
    const val BIAS_ABSOLUTE = 0.01
    const val ITERATIONS = 100
    const val EPSILON = 1E-12
}