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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_done
import kubriko.creator.generated.resources.color_picker_brightness
import kubriko.creator.generated.resources.color_picker_hex
import kubriko.creator.generated.resources.color_picker_hue
import kubriko.creator.generated.resources.color_picker_opacity
import kubriko.creator.generated.resources.color_picker_saturation
import kubriko.creator.generated.resources.color_picker_title
import org.jetbrains.compose.resources.stringResource

/**
 * Picks a color by its hue, saturation, brightness, and opacity, or by its hex code. Nothing changes until it is
 * confirmed.
 *
 * @param initialColor As `0xAARRGGBB`.
 * @param onColorSelected Called with the picked color, as `0xAARRGGBB`.
 * @param isAlphaEnabled Whether the color may be translucent.
 */
@Composable
internal fun ColorPickerDialog(
    initialColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    isAlphaEnabled: Boolean = true,
) {
    var value by remember { mutableStateOf(ColorPickerValue.of(initialColor, isAlphaEnabled)) }
    val hsv = value.color
    AlertDialog(
        modifier = Modifier.width(420.dp),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.color_picker_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .checkerboard()
                        .background(Color(value.argb))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
                )
                CompactTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value.text,
                    onValueChange = { value = value.withText(it) },
                    label = stringResource(Res.string.color_picker_hex),
                    isError = !value.isTextValid,
                )
                GradientSlider(
                    label = stringResource(Res.string.color_picker_hue),
                    value = hsv.hue / 360f,
                    gradient = Brush.horizontalGradient(HUES.map { Color.hsv(it, 1f, 1f) }),
                    onValueChange = { value = value.withColor(hsv.copy(hue = it * 360f)) },
                )
                GradientSlider(
                    label = stringResource(Res.string.color_picker_saturation),
                    value = hsv.saturation,
                    gradient = Brush.horizontalGradient(listOf(Color.hsv(hsv.hue, 0f, hsv.brightness), Color.hsv(hsv.hue, 1f, hsv.brightness))),
                    onValueChange = { value = value.withColor(hsv.copy(saturation = it)) },
                )
                GradientSlider(
                    label = stringResource(Res.string.color_picker_brightness),
                    value = hsv.brightness,
                    gradient = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hsv.hue, hsv.saturation, 1f))),
                    onValueChange = { value = value.withColor(hsv.copy(brightness = it)) },
                )
                if (isAlphaEnabled) {
                    GradientSlider(
                        label = stringResource(Res.string.color_picker_opacity),
                        value = hsv.alpha,
                        gradient = Brush.horizontalGradient(listOf(Color.hsv(hsv.hue, hsv.saturation, hsv.brightness, 0f), Color.hsv(hsv.hue, hsv.saturation, hsv.brightness))),
                        onValueChange = { value = value.withColor(hsv.copy(alpha = it)) },
                        isOverCheckerboard = true,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(value.argb) }) { Text(stringResource(Res.string.action_done)) }
        },
    )
}

private val HUES = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
