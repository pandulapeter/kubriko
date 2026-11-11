/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.ui.components.ColorPickerDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_frame_colors
import kubriko.creator.generated.resources.sprite_editor_palette
import kubriko.creator.generated.resources.sprite_editor_palette_add
import kubriko.creator.generated.resources.sprite_editor_palette_empty
import kubriko.creator.generated.resources.sprite_editor_palette_remove
import kubriko.creator.generated.resources.sprite_editor_primary_color
import kubriko.creator.generated.resources.sprite_editor_secondary_color
import kubriko.creator.generated.resources.sprite_editor_see_through
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The two colors the mouse buttons draw with, each picked in the shared color picker, the project's palette, and the
 * colors the frame shown uses: a click on one makes it the left button's color, a right-click the right one's.
 *
 * @param palette The project's palette, each `0xAARRGGBB`.
 * @param onPaletteChange Makes the given colors the palette.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpriteColorsColumn(
    state: SpriteEditorState,
    palette: List<Long>,
    frameColors: List<Int>,
    onPaletteChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    var pickedSlot by remember { mutableStateOf<Boolean?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ColorSlot(Res.string.sprite_editor_primary_color, state.primaryColor) { pickedSlot = false }
        ColorSlot(Res.string.sprite_editor_secondary_color, state.secondaryColor) { pickedSlot = true }
    }
    TextButton(onClick = { state.primaryColor = PixelImage.TRANSPARENT }) { Text(stringResource(Res.string.sprite_editor_see_through)) }
    SectionTitle(Res.string.sprite_editor_palette)
    if (palette.isEmpty()) {
        Text(stringResource(Res.string.sprite_editor_palette_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Swatches(palette.map { it.toInt() }, state)
    val primary = state.primaryColor.toLong() and 0xFFFFFFFFL
    if (primary in palette) {
        TextButton(onClick = { onPaletteChange(palette - primary) }) { Text(stringResource(Res.string.sprite_editor_palette_remove)) }
    } else if (state.primaryColor != PixelImage.TRANSPARENT) {
        TextButton(onClick = { onPaletteChange(palette + primary) }) { Text(stringResource(Res.string.sprite_editor_palette_add)) }
    }
    if (frameColors.isNotEmpty()) {
        SectionTitle(Res.string.sprite_editor_frame_colors)
        Swatches(frameColors, state)
    }
    pickedSlot?.let { isSecondary ->
        ColorPickerDialog(
            initialColor = (if (isSecondary) state.secondaryColor else state.primaryColor).toLong() and 0xFFFFFFFFL,
            onColorSelected = { color ->
                if (isSecondary) state.secondaryColor = color.toInt() else state.primaryColor = color.toInt()
                pickedSlot = null
            },
            onDismiss = { pickedSlot = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(colors: List<Int>, state: SpriteEditorState) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    colors.forEach { color ->
        ColorSwatch(
            color = color,
            isChosen = color == state.primaryColor || color == state.secondaryColor,
            onChoose = { isSecondary -> if (isSecondary) state.secondaryColor = color else state.primaryColor = color },
        )
    }
}

@Composable
private fun SectionTitle(title: StringResource) = Text(
    text = stringResource(title),
    style = MaterialTheme.typography.labelMedium,
)
