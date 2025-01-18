/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorSlider(
    modifier: Modifier = Modifier,
    name: String = "",
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
) = Column(
    modifier = modifier.padding(bottom = 4.dp),
) {
    val add = remember { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    if (name.isNotBlank()) {
        EditorTextLabel(
            text = "$name: ${"%.2f".format(value)}$suffix",
        )
    }
    val colors = SliderDefaults.colors().copy(
        inactiveTrackColor = MaterialTheme.colorScheme.primary,
    )
    SideEffect {
        if (valueRange == null && add.value != 0f) {
            onValueChanged(value + add.value.toDifference())
        }
    }
    Slider(
        modifier = Modifier.height(16.dp),
        value = if (valueRange == null) add.value else value,
        onValueChange = {
            if (valueRange == null) {
                add.value = it
            } else {
                onValueChanged(it)
            }
        },
        onValueChangeFinished = {
            add.value = 0f
        },
        valueRange = valueRange?.let { if (valueRange.isEmpty()) null else valueRange } ?: -5f..5f,
        enabled = valueRange?.isEmpty() != true && enabled,
        interactionSource = interactionSource,
        thumb = {
            Spacer(
                Modifier
                    .size(4.dp, 16.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(if (enabled) colors.thumbColor else colors.disabledThumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(4.dp),
                colors = colors,
                enabled = enabled,
                sliderState = sliderState,
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
            )
        }
    )
}

fun Float.toDifference(): Float {
    val a = absoluteValue / 1000
    val b = ln(2000.0) / 5
    return (a * exp(b * absoluteValue).toFloat() * sign)
}