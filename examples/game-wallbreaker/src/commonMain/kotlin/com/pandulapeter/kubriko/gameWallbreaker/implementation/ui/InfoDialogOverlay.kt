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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.close
import kubriko.examples.game_wallbreaker.generated.resources.ic_close
import kubriko.examples.game_wallbreaker.generated.resources.information_contents
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InfoDialogOverlay(
    modifier: Modifier,
    isVisible: Boolean,
    onInfoDialogClosed: () -> Unit,
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
                .background(Color.Black.copy(alpha = 0.75f))
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
                    .padding(16.dp)
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(
                        color = MaterialTheme.colorScheme.primary,
                        text = stringResource(Res.string.information_contents),
                    )
                }
            }
        }
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            WallbreakerButton(
                onButtonPressed = onInfoDialogClosed,
                icon = Res.drawable.ic_close,
                contentDescription = Res.string.close,
                containerColor = createButtonColor(0.5f),
                onPointerEnter = onButtonHover,
            )
        }
    }
}