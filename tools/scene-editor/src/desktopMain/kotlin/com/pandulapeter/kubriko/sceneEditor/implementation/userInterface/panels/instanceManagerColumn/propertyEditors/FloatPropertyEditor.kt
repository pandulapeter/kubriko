/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput

@Composable
internal fun FloatPropertyEditor(
    modifier: Modifier = Modifier,
    name: String,
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
    shouldUseHorizontalLayout: Boolean = false,
) = Column(
    modifier = modifier,
) {
    EditorNumberInput(
        name = name,
        suffix = suffix,
        value = value,
        onValueChanged = onValueChanged,
        valueRange = valueRange,
        enabled = enabled,
        shouldUseHorizontalLayout = shouldUseHorizontalLayout,
    )
}