/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.uiComponents.LargeButton
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.close_scene_editor
import kubriko.examples.demo_isometric_graphics.generated.resources.open_scene_editor

internal val isSceneEditorVisible = MutableStateFlow(false)
internal actual val sceneJson: MutableStateFlow<String>? = MutableStateFlow("")

@Composable
internal actual fun PlatformSpecificContent() {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    LargeButton(
        onButtonPressed = { isSceneEditorVisible.value = !isEditorVisible.value },
        title = if (isEditorVisible.value) Res.string.close_scene_editor else Res.string.open_scene_editor,
    )
}