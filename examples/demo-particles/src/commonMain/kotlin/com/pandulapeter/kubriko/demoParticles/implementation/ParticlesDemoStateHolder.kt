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
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.burst
import kubriko.examples.demo_particles.generated.resources.collapse_controls
import kubriko.examples.demo_particles.generated.resources.description
import kubriko.examples.demo_particles.generated.resources.emit_continuously
import kubriko.examples.demo_particles.generated.resources.expand_controls
import kubriko.examples.demo_particles.generated.resources.ic_brush
import kubriko.examples.demo_particles.generated.resources.lifespan
import kubriko.examples.demo_particles.generated.resources.rate

sealed interface ParticlesDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_brush).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.expand_controls).value.isNotBlank()
                && preloadedString(Res.string.collapse_controls).value.isNotBlank()
                && preloadedString(Res.string.emit_continuously).value.isNotBlank()
                && preloadedString(Res.string.rate).value.isNotBlank()
                && preloadedString(Res.string.lifespan).value.isNotBlank()
                && preloadedString(Res.string.burst).value.isNotBlank()
    }
}

internal class ParticlesDemoStateHolderImpl(
    isLoggingEnabled: Boolean,
) : ParticlesDemoStateHolder {

    private val particleManager = ParticleManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val particlesDemoManager = ParticlesDemoManager()
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            particleManager,
            particlesDemoManager,
            isLoggingEnabled = isLoggingEnabled,
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