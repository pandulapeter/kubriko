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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.collapse_controls
import kubriko.examples.demo_particles.generated.resources.description
import kubriko.examples.demo_particles.generated.resources.expand_controls
import kubriko.examples.demo_particles.generated.resources.ic_brush
import org.jetbrains.compose.resources.stringResource

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
) = Column(
    modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(windowInsets)
        .padding(16.dp),
) {
    InfoPanel(
        stringResource = Res.string.description,
        isVisible = StateHolder.isInfoPanelVisible.value,
    )
    Spacer(modifier = Modifier.weight(1f))
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        this@Column.AnimatedVisibility(
            visible = areControlsExpanded,
            enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
            exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                EmitterPropertiesPanel(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .width(240.dp),
                    emissionRate = emissionRate,
                    onEmissionRateChanged = onEmissionRateChanged,
                    isEmittingContinuously = isEmittingContinuously,
                    onEmittingContinuouslyChanged = onEmittingContinuouslyChanged,
                    onBurstButtonPressed = onBurstButtonPressed,
                    lifespan = lifespan,
                    onLifespanChanged = onLifespanChanged,
                )
            }
        }
        FloatingButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            icon = Res.drawable.ic_brush,
            isSelected = areControlsExpanded,
            contentDescription = stringResource(if (areControlsExpanded) Res.string.collapse_controls else Res.string.expand_controls),
            onButtonPressed = onControlsToggled,
        )
    }
}
