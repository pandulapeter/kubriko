/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
                ColorEditorMode.HSV -> "HSV"
                ColorEditorMode.RGB -> "RGB"
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