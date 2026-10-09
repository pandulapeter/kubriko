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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.LargeButton
import com.pandulapeter.kubriko.uiComponents.Panel
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import com.pandulapeter.kubriko.uiComponents.SmallSwitch
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.burst
import kubriko.examples.demo_particles.generated.resources.emit_continuously
import kubriko.examples.demo_particles.generated.resources.lifespan
import kubriko.examples.demo_particles.generated.resources.rate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmitterPropertiesPanel(
    modifier: Modifier,
    emissionRate: Float,
    onEmissionRateChanged: (Float) -> Unit,
    isEmittingContinuously: Boolean,
    onEmittingContinuouslyChanged: () -> Unit,
    onBurstButtonPressed: () -> Unit,
    lifespan: Float,
    onLifespanChanged: (Float) -> Unit,
) = Panel(
    modifier = modifier,
) {
    EmitterControls(
        emissionRate = emissionRate,
        onEmissionRateChanged = onEmissionRateChanged,
        isEmittingContinuously = isEmittingContinuously,
        onEmittingContinuouslyChanged = onEmittingContinuouslyChanged,
        onBurstButtonPressed = onBurstButtonPressed,
        lifespan = lifespan,
        onLifespanChanged = onLifespanChanged,
    )
}

@Composable
private fun EmitterControls(
    emissionRate: Float,
    onEmissionRateChanged: (Float) -> Unit,
    isEmittingContinuously: Boolean,
    onEmittingContinuouslyChanged: () -> Unit,
    onBurstButtonPressed: () -> Unit,
    lifespan: Float,
    onLifespanChanged: (Float) -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.rate),
        modifier = Modifier.padding(horizontal = 16.dp),
        value = emissionRate,
        onValueChanged = onEmissionRateChanged,
        valueRange = 0.06f..0.45f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.lifespan),
        modifier = Modifier.padding(horizontal = 16.dp),
        value = lifespan,
        onValueChanged = onLifespanChanged,
        valueRange = 50f..1500f,
    )
    SmallSwitch(
        title = stringResource(Res.string.emit_continuously),
        isEnabled = emissionRate > 0f,
        isChecked = isEmittingContinuously,
        onCheckedChanged = onEmittingContinuouslyChanged,
    )
    Spacer(modifier = Modifier.height(4.dp))
    LargeButton(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = Res.string.burst,
        isEnabled = !isEmittingContinuously && emissionRate > 0f,
        onButtonPressed = onBurstButtonPressed,
    )
    Spacer(modifier = Modifier.height(4.dp))
}