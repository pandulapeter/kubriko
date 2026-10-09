/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
internal fun ProgressBar(
    value: Float,
    minColor: Color,
    maxColor: Color,
) = Box(
    modifier = Modifier
        .fillMaxWidth()
        .spaceSquadronUIElementBorder(),
) {
    val animatedValue = animateFloatAsState(
        targetValue = value,
        animationSpec = tween(),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction = animatedValue.value)
            .height(12.dp)
            .clip(SpaceSquadronUIElementShape)
            .background(lerp(minColor, maxColor, animatedValue.value).copy(alpha = 0.5f)),
    )
}
