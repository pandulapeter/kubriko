/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle

/**
 * A [CompactTextField] for a value whose changes may reach the model later: what is typed shows at once, and a value
 * changed elsewhere shows at once too, while the field's own reports coming back late leave what is typed alone.
 */
@Composable
public fun BufferedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    isSingleLine: Boolean = true,
    isWrapping: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    var text by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val sentValues = remember { SentValues<String>() }
    LaunchedEffect(value) {
        if (!sentValues.isEcho(value) || !isFocused) text = value
    }
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            sentValues.clear()
            text = value
        }
    }
    CompactTextField(
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        value = text,
        onValueChange = { newText ->
            text = newText
            sentValues.add(newText)
            onValueChange(newText)
        },
        label = label,
        placeholder = placeholder,
        isError = isError,
        isSingleLine = isSingleLine,
        isWrapping = isWrapping,
        textStyle = textStyle,
    )
}
