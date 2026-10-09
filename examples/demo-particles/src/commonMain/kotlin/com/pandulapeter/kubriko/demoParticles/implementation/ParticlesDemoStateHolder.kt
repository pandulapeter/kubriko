/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.shared.ui.areExpandControlsButtonResourcesLoaded
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.burst
import kubriko.examples.demo_particles.generated.resources.description
import kubriko.examples.demo_particles.generated.resources.emit_continuously
import kubriko.examples.demo_particles.generated.resources.lifespan
import kubriko.examples.demo_particles.generated.resources.rate

sealed interface ParticlesDemoStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areExpandControlsButtonResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.description).value.isNotBlank()
                && preloadedString(Res.string.emit_continuously).value.isNotBlank()
                && preloadedString(Res.string.rate).value.isNotBlank()
                && preloadedString(Res.string.lifespan).value.isNotBlank()
                && preloadedString(Res.string.burst).value.isNotBlank()
    }
}
