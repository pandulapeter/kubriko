/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.unfinished_disclaimer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UnfinishedDisclaimer(
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .background(
            shape = BlockysJourneyUIElementShape,
            color = Color.Black.copy(alpha = 0.9f)
        )
        .border(
            shape = BlockysJourneyUIElementShape,
            color = MaterialTheme.colorScheme.primary,
            width = 2.dp,
        )
        .padding(
            vertical = 16.dp,
            horizontal = 24.dp,
        ),
) {
    Text(
        color = MaterialTheme.colorScheme.primary,
        text = stringResource(Res.string.unfinished_disclaimer),
        textAlign = TextAlign.Center,
    )
}