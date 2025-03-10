/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.types.AngleRadians

@Composable
internal fun RotationPropertyEditor(
    name: String,
    value: AngleRadians,
    onValueChanged: (AngleRadians) -> Unit,
    angleEditorMode: AngleEditorMode,
    shouldUseHorizontalLayout: Boolean = false,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    when (angleEditorMode) {
        AngleEditorMode.DEGREES -> FloatPropertyEditor(
            modifier = Modifier.weight(1f),
            name = name,
            suffix = "°",
            value = value.deg.normalized,
            onValueChanged = { onValueChanged(it.deg.rad) },
            valueRange = 0f..359.99f,
            shouldUseHorizontalLayout = shouldUseHorizontalLayout,
        )

        AngleEditorMode.RADIANS -> FloatPropertyEditor(
            modifier = Modifier.weight(1f),
            name = name,
            value = value.normalized,
            onValueChanged = { onValueChanged(it.rad) },
            valueRange = 0f..6.27f,
            shouldUseHorizontalLayout = shouldUseHorizontalLayout,
        )
    }
}