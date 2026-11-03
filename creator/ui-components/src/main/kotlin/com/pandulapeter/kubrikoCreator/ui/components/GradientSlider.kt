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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * A slider from 0 to 1 whose track shows [gradient], the colors its values give, behind a thin line marking [value].
 *
 * @param isOverCheckerboard Whether the gradient has translucent colors, which then show the checkerboard behind them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GradientSlider(
    label: String,
    value: Float,
    gradient: Brush,
    onValueChange: (Float) -> Unit,
    isOverCheckerboard: Boolean = false,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        modifier = Modifier.width(80.dp),
        text = label,
        style = MaterialTheme.typography.bodyMedium,
    )
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(if (isOverCheckerboard) Modifier.checkerboard() else Modifier)
                .background(gradient),
        )
        Slider(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            value = value,
            onValueChange = onValueChange,
            thumb = {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(2.dp)),
                )
            },
            // The gradient behind is the track, but the slider needs one of full width to place the thumb along it.
            track = { Spacer(Modifier.fillMaxWidth().height(24.dp)) },
        )
    }
}
