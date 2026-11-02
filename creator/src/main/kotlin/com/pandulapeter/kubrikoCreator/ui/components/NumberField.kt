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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A field for a number that keeps whatever is being typed, and reports the value whenever it is a valid number; a value
 * changed elsewhere shows at once, while the field's own reports coming back late leave what is typed alone.
 */
@Composable
internal fun NumberField(
    label: StringResource,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) = NumberField(
    label = stringResource(label),
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
)

@Composable
internal fun NumberField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(formatNumber(value)) }
    var isFocused by remember { mutableStateOf(false) }
    val sentValues = remember { SentValues<Float>() }
    LaunchedEffect(value) {
        if ((!sentValues.isEcho(value) || !isFocused) && parseNumber(text) != value) text = formatNumber(value)
    }
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            sentValues.clear()
            if (parseNumber(text) != value) text = formatNumber(value)
        }
    }
    CompactTextField(
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        value = text,
        onValueChange = { newText ->
            text = newText
            parseNumber(newText)?.let {
                sentValues.add(it)
                onValueChange(it)
            }
        },
        label = label,
        isError = parseNumber(text) == null,
    )
}

internal fun formatNumber(value: Float) = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

internal fun parseNumber(text: String) = text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
