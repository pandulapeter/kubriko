/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoParticles.implementation.managers.ParticlesDemoManager
import com.pandulapeter.kubriko.particles.ParticleManager
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ParticlesDemoStateHolder : StateHolder

internal class ParticlesDemoStateHolderImpl : ParticlesDemoStateHolder {

    private val particleManager = ParticleManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            particleManager,
            ParticlesDemoManager(),
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko get() = _kubriko.asStateFlow()

    override fun dispose() = kubriko.value.dispose()
}

private const val LOG_TAG = "Particles"