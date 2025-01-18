/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun EditorNumberInput(
    modifier: Modifier = Modifier,
    name: String,
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
    shouldUseHorizontalLayout: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorTextInput(
                modifier = Modifier.weight(1f),
                title = name,
                value = "%.2f".format(value) + suffix,
                onValueChanged = { newValue ->
                    newValue.toFloatOrNull()?.let {
                        onValueChanged(if (valueRange == null) it else min(valueRange.endInclusive, max(valueRange.start, it)))
                    }
                },
                enabled = enabled,
                extraContent = extraContent,
            )
            if (shouldUseHorizontalLayout) {
                EditorSlider(
                    modifier = Modifier.weight(2f),
                    value = value,
                    onValueChanged = onValueChanged,
                    enabled = enabled,
                    valueRange = valueRange,
                )
            }
        }
        if (!shouldUseHorizontalLayout) {
            EditorSlider(
                value = value,
                onValueChanged = onValueChanged,
                enabled = enabled,
                valueRange = valueRange,
            )
        }
    }
}