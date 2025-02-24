/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.highscore
import kubriko.examples.game_wallbreaker.generated.resources.ic_pause
import kubriko.examples.game_wallbreaker.generated.resources.pause
import kubriko.examples.game_wallbreaker.generated.resources.score
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameOverlay(
    gameAreaModifier: Modifier,
    isGameRunning: Boolean,
    score: Int,
    highScore: Int,
    onPauseButtonPressed: () -> Unit,
    onButtonHover: () -> Unit,
) {
    val shouldShowScore = remember { mutableStateOf(false) }
    if (score > 0) {
        shouldShowScore.value = true
    }
    AnimatedVisibility(
        visible = isGameRunning,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = gameAreaModifier.fillMaxSize().padding(16.dp),
        ) {
            WallbreakerButton(
                onButtonPressed = onPauseButtonPressed,
                icon = Res.drawable.ic_pause,
                contentDescription = Res.string.pause,
                containerColor = createButtonColor(0.5f),
                onPointerEnter = onButtonHover,
            )
        }
    }
    AnimatedVisibility(
        visible = shouldShowScore.value,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = gameAreaModifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WallbreakerCard {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                        text = stringResource(Res.string.score, score),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                    )
                }
                WallbreakerCard {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                        text = stringResource(Res.string.highscore, highScore),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}