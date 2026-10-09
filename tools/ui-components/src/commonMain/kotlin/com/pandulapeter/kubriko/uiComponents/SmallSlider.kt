/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:JvmName("SmallSliderKt")
@file:JvmMultifileClass

package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * A compact slider component used for fine-tuning values in tool panels.
 *
 * @param modifier The modifier to apply to the slider.
 * @param value The current value of the slider.
 * @param onValueChanged Callback to be invoked when the value changes.
 * @param valueRange The range of values the slider can represent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        modifier = modifier.height(24.dp),
        value = value,
        onValueChange = onValueChanged,
        valueRange = valueRange,
        interactionSource = interactionSource,
        thumb = {
            Spacer(
                Modifier
                    .size(4.dp, 16.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(4.dp),
                colors = SliderDefaults.colors().copy(
                    inactiveTrackColor = MaterialTheme.colorScheme.primary,
                ),
                sliderState = sliderState,
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
            )
        }
    )
}
