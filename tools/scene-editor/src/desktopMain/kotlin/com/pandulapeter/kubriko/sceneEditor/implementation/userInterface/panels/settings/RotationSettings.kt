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
import kubriko.tools.scene_editor.generated.resources.angle_mode_degrees
import kubriko.tools.scene_editor.generated.resources.angle_mode_radians
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AngleSettings(
    angleEditorMode: AngleEditorMode,
    onAngleEditorModeChanged: (AngleEditorMode) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    AngleEditorMode.entries.forEach { mode ->
        EditorRadioButton(
            label = when (mode) {
                AngleEditorMode.RADIANS -> stringResource(Res.string.angle_mode_radians)
                AngleEditorMode.DEGREES -> stringResource(Res.string.angle_mode_degrees)
            },
            isSmall = true,
            isSelected = mode == angleEditorMode,
            onSelectionChanged = { onAngleEditorModeChanged(mode) },
        )
    }
}

internal enum class AngleEditorMode {
    DEGREES,
    RADIANS,
}