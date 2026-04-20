/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.manager.Manager

/**
 * Manager responsible for handling particle systems.
 *
 * It manages the lifecycle and rendering of particles.
 */
sealed class ParticleManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "ParticleManager",
) {

    companion object {
        /**
         * Creates a new [ParticleManager] instance.
         *
         * @param cacheSize The maximum number of particles that can be active at once.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            cacheSize: Int = 1000,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): ParticleManager = ParticleManagerImpl(
            cacheSize = cacheSize,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
