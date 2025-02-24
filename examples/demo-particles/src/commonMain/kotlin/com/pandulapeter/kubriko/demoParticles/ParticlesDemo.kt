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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolder
import com.pandulapeter.kubriko.demoParticles.implementation.ParticlesDemoStateHolderImpl

fun createParticlesDemoStateHolder(): ParticlesDemoStateHolder = ParticlesDemoStateHolderImpl()

@Composable
fun ParticlesDemo(
    modifier: Modifier = Modifier,
    stateHolder: ParticlesDemoStateHolder = createParticlesDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ParticlesDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(Color.Black),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}