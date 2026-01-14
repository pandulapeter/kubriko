/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TODO: Documentation
 */
sealed class PhysicsManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "PhysicsManager",
) {
    abstract val gravity: MutableStateFlow<SceneOffset>
    abstract val simulationSpeed: MutableStateFlow<Float>

    companion object {
        fun newInstance(
            initialGravity: SceneOffset = SceneOffset(0f.sceneUnit, 9.81f.sceneUnit),
            initialSimulationSpeed: Float = 1f,
            penetrationCorrection: Float = 0.2f,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): PhysicsManager = PhysicsManagerImpl(
            initialGravity = initialGravity,
            initialSimulationSpeed = initialSimulationSpeed,
            penetrationCorrection = penetrationCorrection,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}