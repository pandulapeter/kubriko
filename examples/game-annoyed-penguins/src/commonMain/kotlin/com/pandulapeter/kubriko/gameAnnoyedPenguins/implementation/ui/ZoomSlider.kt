/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ZoomSlider(
    modifier: Modifier = Modifier,
    minimumScaleFactor: Float,
    maximumScaleFactor: Float,
    currentScaleFactor: Float,
    updateScaleFactor: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        modifier = modifier.height(24.dp),
        value = currentScaleFactor,
        onValueChange = updateScaleFactor,
        valueRange = minimumScaleFactor..maximumScaleFactor,
        interactionSource = interactionSource,
        thumb = {
            Spacer(
                modifier
                    .size(16.dp, 16.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(8.dp),
                colors = SliderDefaults.colors().copy(
                    inactiveTrackColor = MaterialTheme.colorScheme.primary,
                ),
                sliderState = sliderState,
                trackInsideCornerSize = 8.dp,
                thumbTrackGapSize = 8.dp,
            )
        }
    )
}