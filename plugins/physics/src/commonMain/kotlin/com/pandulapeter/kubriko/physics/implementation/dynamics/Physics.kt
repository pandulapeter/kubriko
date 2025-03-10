/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.dynamics

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit

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