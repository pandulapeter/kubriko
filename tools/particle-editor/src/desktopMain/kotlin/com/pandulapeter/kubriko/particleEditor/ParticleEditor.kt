/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particleEditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.particleEditor.implementation.InternalParticleEditor

/**
 * TODO: Documentation
 */
fun openParticleEditor() = application {
    ParticleEditor(
        onCloseRequest = ::exitApplication,
    )
}

/**
 * TODO: Documentation
 */
@Composable
fun ParticleEditor(
    title: String = "Particle Editor",
    onCloseRequest: () -> Unit,
) = InternalParticleEditor(
    title = title,
    onCloseRequest = onCloseRequest,
)