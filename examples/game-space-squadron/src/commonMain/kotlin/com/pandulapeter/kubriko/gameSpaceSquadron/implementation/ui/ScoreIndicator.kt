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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.score
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ScoreIndicator(
    highScore: Int,
    score: Int,
) = Text(
    modifier = Modifier
        .padding(16.dp)
        .background(
            color = Color.Black.copy(alpha = 0.75f),
            shape = SpaceSquadronUIElementShape,
        )
        .spaceSquadronUIElementBorder()
        .padding(
            horizontal = 8.dp,
            vertical = 4.dp,
        ),
    style = MaterialTheme.typography.labelSmall,
    color = Color.White,
    text = stringResource(Res.string.score, highScore, score),
)
