/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.close_confirmation
import kubriko.examples.game_wallbreaker.generated.resources.close_confirmation_negative
import kubriko.examples.game_wallbreaker.generated.resources.close_confirmation_positive
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CloseConfirmationDialogOverlay(
    modifier: Modifier,
    isVisible: Boolean,
    onCloseConfirmed: () -> Unit,
    onCloseCancelled: () -> Unit,
    onButtonHover: () -> Unit,
) = Box(
    modifier = modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
        )
    }
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {},
        )
    }
    WallbreakerAnimatedVisibility(
        visible = isVisible,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            WallbreakerCard(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    )
                    .align(Alignment.Center),
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp,
                        )
                        .align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        color = MaterialTheme.colorScheme.primary,
                        text = stringResource(Res.string.close_confirmation),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WallbreakerTextButton(
                            onButtonPressed = onCloseCancelled,
                            onPointerEnter = onButtonHover,
                            stringResource = Res.string.close_confirmation_negative,
                            containerColor = createButtonColor(0.75f),
                        )
                        WallbreakerTextButton(
                            onButtonPressed = onCloseConfirmed,
                            onPointerEnter = onButtonHover,
                            stringResource = Res.string.close_confirmation_positive,
                            containerColor = createButtonColor(0.6f),
                        )
                    }
                }
            }
        }
    }
}