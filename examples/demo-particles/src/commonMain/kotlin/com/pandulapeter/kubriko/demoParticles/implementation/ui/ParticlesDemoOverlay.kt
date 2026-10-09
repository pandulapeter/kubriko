/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles.implementation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.ExpandableControlsOverlay
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.description

@Composable
internal fun ParticlesDemoOverlay(
    windowInsets: WindowInsets,
    areControlsExpanded: Boolean,
    emissionRate: Float,
    onEmissionRateChanged: (Float) -> Unit,
    isEmittingContinuously: Boolean,
    onEmittingContinuouslyChanged: () -> Unit,
    onBurstButtonPressed: () -> Unit,
    lifespan: Float,
    onLifespanChanged: (Float) -> Unit,
    onControlsToggled: () -> Unit,
) = ExpandableControlsOverlay(
    windowInsets = windowInsets,
    description = Res.string.description,
    isExpanded = areControlsExpanded,
    onToggle = onControlsToggled,
) { modifier ->
    EmitterPropertiesPanel(
        modifier = modifier.width(240.dp),
        emissionRate = emissionRate,
        onEmissionRateChanged = onEmissionRateChanged,
        isEmittingContinuously = isEmittingContinuously,
        onEmittingContinuouslyChanged = onEmittingContinuouslyChanged,
        onBurstButtonPressed = onBurstButtonPressed,
        lifespan = lifespan,
        onLifespanChanged = onLifespanChanged,
    )
}
