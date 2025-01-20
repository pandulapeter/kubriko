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
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.uiComponents.LargeButton
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.demo_particles.generated.resources.Res
import kubriko.examples.demo_particles.generated.resources.close_particle_editor
import kubriko.examples.demo_particles.generated.resources.open_particle_editor

internal val isParticleEditorVisible = MutableStateFlow(false)

@Composable
internal actual fun PlatformSpecificContent() {
    val isEditorVisible = isParticleEditorVisible.collectAsState()
    LargeButton(
        title = if (isEditorVisible.value) Res.string.close_particle_editor else Res.string.open_particle_editor,
        onButtonPressed = { isParticleEditorVisible.value = !isEditorVisible.value },
    )
}