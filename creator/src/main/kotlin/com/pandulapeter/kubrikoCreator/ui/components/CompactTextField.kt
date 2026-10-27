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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * A desktop-sized text field: one line high, with an optional [label] inside it on the left and an optional
 * [placeholder] while it is empty. [isError] draws its outline in the error color.
 */
@Composable
internal fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    isSingleLine: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = isSingleLine,
        textStyle = textStyle.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .border(1.dp, if (isError) colors.error else colors.outlineVariant, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = if (isSingleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
