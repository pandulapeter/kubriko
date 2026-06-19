/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.color_mode_hsv
import kubriko.tools.scene_editor.generated.resources.color_mode_rgb
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ColorSettings(
    colorEditorMode: ColorEditorMode,
    onColorEditorModeChanged: (ColorEditorMode) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    ColorEditorMode.entries.forEach { mode ->
        EditorRadioButton(
            label = when (mode) {
                ColorEditorMode.HSV -> stringResource(Res.string.color_mode_hsv)
                ColorEditorMode.RGB -> stringResource(Res.string.color_mode_rgb)
            },
            isSmall = true,
            isSelected = mode == colorEditorMode,
            onSelectionChanged = { onColorEditorModeChanged(mode) },
        )
    }
}

internal enum class ColorEditorMode {
    HSV,
    RGB,
}