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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoParticles.implementation.managers.ParticlesDemoManager
import com.pandulapeter.kubriko.particles.ParticleManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.ic_brush

sealed interface ParticlesDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = preloadedImageVector(Res.drawable.ic_brush).value != null
    }
}

internal class ParticlesDemoStateHolderImpl : ParticlesDemoStateHolder {

    private val particleManager = ParticleManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val particlesDemoManager = ParticlesDemoManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            particleManager,
            particlesDemoManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun dispose() = kubriko.value.dispose()

    override fun navigateBack(
        isInFullscreenMode: Boolean,
        onFullscreenModeToggled: () -> Unit,
    ) = particlesDemoManager.areControlsExpanded.value.also {
        if (it) {
            particlesDemoManager.toggleControlsExpanded()
        }
    }
}

private const val LOG_TAG = "Particles"