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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.foundation.ColorHex
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.actor_type_color
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.color_picker_title
import org.jetbrains.compose.resources.stringResource

/**
 * A hex code field (`#RRGGBB`, or `#AARRGGBB` for a translucent color) with a swatch of the current color, which
 * opens the color picker.
 *
 * @param isAlphaEnabled False for a field that takes only opaque colors: its picker has no opacity, it marks an
 *   `#AARRGGBB` code as an error rather than taking it, and it shows a translucent [color] as opaque.
 */
@Composable
public fun ColorField(
    color: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isAlphaEnabled: Boolean = true,
): Unit = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val shownColor = if (isAlphaEnabled) color else ColorHex.opaque(color)
    fun parse(text: String) = ColorHex.parse(text)?.takeIf { isAlphaEnabled || ColorHex.isOpaque(it) }
    var text by remember { mutableStateOf(ColorHex.format(shownColor)) }
    var isFocused by remember { mutableStateOf(false) }
    val sentColors = remember { SentValues<Long>() }
    LaunchedEffect(shownColor) {
        if ((!sentColors.isEcho(shownColor) || !isFocused) && parse(text) != shownColor) text = ColorHex.format(shownColor)
    }
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            sentColors.clear()
            if (parse(text) != shownColor) text = ColorHex.format(shownColor)
        }
    }
    var isPickerOpen by remember { mutableStateOf(false) }
    WithTooltip(
        label = stringResource(Res.string.color_picker_title),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .checkerboard()
                .background(Color(shownColor))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { isPickerOpen = true },
        )
    }
    if (isPickerOpen) {
        ColorPickerDialog(
            initialColor = shownColor,
            isAlphaEnabled = isAlphaEnabled,
            onColorSelected = {
                isPickerOpen = false
                text = ColorHex.format(it)
                onColorChange(it)
            },
            onDismiss = { isPickerOpen = false },
        )
    }
    CompactTextField(
        modifier = Modifier.width(150.dp).onFocusChanged { isFocused = it.isFocused },
        value = text,
        onValueChange = { newText ->
            text = newText
            parse(newText)?.let {
                sentColors.add(it)
                onColorChange(it)
            }
        },
        label = stringResource(Res.string.actor_type_color),
        isError = parse(text) == null,
    )
}
