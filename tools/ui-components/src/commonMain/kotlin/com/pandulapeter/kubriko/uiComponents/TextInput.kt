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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor

/**
 * A simple text input component used for editing string properties in the Scene Editor.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param value The current text to display.
 * @param onValueChanged Callback when the text changes.
 * @param enabled Whether the input is interactive.
 */
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true,
) {
    // TODO: Focusing this fields should take focus away from KubrikoViewport to avoid reacting to key presses
    BasicTextField(
        modifier = modifier,
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