/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A text field for a number that keeps whatever is being typed, and reports the value whenever it is a valid number.
 */
@Composable
internal fun NumberField(
    label: StringResource,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(formatNumber(value)) }
    if (parseNumber(text) != value) {
        text = formatNumber(value)
    }
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = { newText ->
            text = newText
            parseNumber(newText)?.let(onValueChange)
        },
        label = { Text(stringResource(label)) },
        isError = parseNumber(text) == null,
        singleLine = true,
    )
}

internal fun formatNumber(value: Float) = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

internal fun parseNumber(text: String) = text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
