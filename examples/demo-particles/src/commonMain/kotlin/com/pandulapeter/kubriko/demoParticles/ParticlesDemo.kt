/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoParticles

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolderImpl
import com.pandulapeter.kubriko.demoParticles.implementation.ui.EmitterPropertiesPanel

fun createParticlesDemoStateHolder(): ParticlesDemoStateHolder = ParticlesDemoStateHolderImpl()

@Composable
fun ParticlesDemo(
    modifier: Modifier = Modifier,
    stateHolder: ParticlesDemoStateHolder = createParticlesDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ParticlesDemoStateHolderImpl
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        KubrikoViewport(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            kubriko = stateHolder.kubriko.collectAsState().value,
            windowInsets = windowInsets,
        )
        VerticalDivider()
        EmitterPropertiesPanel(
            modifier = Modifier.width(240.dp).fillMaxHeight(),
            particlesDemoManager = stateHolder.particlesDemoManager,
        )
    }
}