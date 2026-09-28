/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.formatEditorNumber
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.parseEditorNumber

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
    shouldRound: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null,
) {
    var text by remember { mutableStateOf(formatEditorNumber(value, shouldRound)) }
    LaunchedEffect(value, shouldRound) {
        if (parseEditorNumber(text) != value) {
            text = formatEditorNumber(value, shouldRound)
        }
    }
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
                value = text,
                suffix = suffix.takeIf { it.isNotEmpty() },
                onValueChanged = { newText ->
                    text = newText
                    parseEditorNumber(newText)?.let { onValueChanged(valueRange?.let { range -> it.coerceIn(range) } ?: it) }
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