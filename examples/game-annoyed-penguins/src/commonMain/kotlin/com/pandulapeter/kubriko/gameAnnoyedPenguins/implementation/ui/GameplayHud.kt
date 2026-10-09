/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_pause
import kubriko.examples.game_annoyed_penguins.generated.resources.pause
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameplayHud(
    windowInsets: WindowInsets,
    onPauseButtonPressed: () -> Unit,
    onButtonHover: () -> Unit,
    minimumScaleFactor: Float,
    maximumScaleFactor: Float,
    currentScaleFactor: Float,
    onScaleFactorChanged: (Float) -> Unit,
    collectedStarCount: Int,
    totalStarCount: Int,
) = Row(
    modifier = Modifier
        .windowInsetsPadding(windowInsets)
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
) {
    AnnoyedPenguinsButton(
        onButtonPressed = onPauseButtonPressed,
        icon = Res.drawable.ic_pause,
        title = stringResource(Res.string.pause),
        onPointerEnter = onButtonHover,
    )
    ZoomSlider(
        modifier = Modifier.weight(1f),
        minimumScaleFactor = minimumScaleFactor,
        maximumScaleFactor = maximumScaleFactor,
        currentScaleFactor = currentScaleFactor,
        updateScaleFactor = onScaleFactorChanged,
    )
    StarCounter(
        collectedStarCount = collectedStarCount,
        totalStarCount = totalStarCount,
    )
}
