/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor

/**
 * A simple text input component used for editing string properties in the Scene Editor.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param value The current text to display.
 * @param onValueChanged Callback when the text changes.
 * @param enabled Whether the input is interactive.
 * @param onFocusChanged Callback invoked with the field's focus state. It is guaranteed to be called
 *  with `false` when the field leaves the composition while focused, so consumers can rely on the
 *  reported state being balanced.
 */
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (isFocused) {
                onFocusChanged(false)
            }
        }
    }
    BasicTextField(
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused != isFocused) {
                isFocused = focusState.isFocused
                onFocusChanged(focusState.isFocused)
            }
        },
        value = value,
        enabled = enabled,
        onValueChange = onValueChanged,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorationBox = { innerTextField -> innerTextField() }
    )
}