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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ColorHex
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_color
import org.jetbrains.compose.resources.stringResource

/**
 * A `#RRGGBB` field with a swatch of the current color.
 */
@Composable
internal fun ColorField(
    color: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    var text by remember { mutableStateOf(ColorHex.format(color)) }
    if (ColorHex.parse(text) != color) {
        text = ColorHex.format(color)
    }
    Box(
        Modifier
            .size(24.dp)
            .background(Color(color), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
    CompactTextField(
        modifier = Modifier.width(150.dp),
        value = text,
        onValueChange = { newText ->
            text = newText
            ColorHex.parse(newText)?.let(onColorChange)
        },
        label = stringResource(Res.string.actor_type_color),
        isError = ColorHex.parse(text) == null,
    )
}
