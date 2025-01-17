/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    shouldShowLoadingIndicator: Boolean,
    enter: EnterTransition = fadeIn(animationSpec = tween(durationMillis = 0)),
    exit: ExitTransition = fadeOut(animationSpec = tween(durationMillis = 1000)),
    content: @Composable (() -> Unit)? = null,
) {
    if (content != null) {
        AnimatedVisibility(
            visible = !shouldShowLoadingIndicator,
            enter = enter,
            exit = exit,
        ) {
            Box(
                modifier = modifier,
            ) {
                content()
            }
        }
    }
    AnimatedVisibility(
        visible = shouldShowLoadingIndicator,
        enter = enter,
        exit = exit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            LoadingIndicator(
                modifier = modifier.align(Alignment.BottomStart),
            )
        }
    }
}