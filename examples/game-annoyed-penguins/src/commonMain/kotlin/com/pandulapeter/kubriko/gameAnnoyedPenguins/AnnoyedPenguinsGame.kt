/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.AnnoyedPenguinsTheme

fun createAnnoyedPenguinsGameStateHolder(): AnnoyedPenguinsGameStateHolder = AnnoyedPenguinsGameStateHolderImpl()

/**
 * Music: https://opengameart.org/content/childrens-march-theme
 */
@Composable
fun AnnoyedPenguinsGame(
    modifier: Modifier = Modifier,
    stateHolder: AnnoyedPenguinsGameStateHolder = createAnnoyedPenguinsGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = AnnoyedPenguinsTheme {
    stateHolder as AnnoyedPenguinsGameStateHolderImpl
    KubrikoViewport(
        modifier = modifier.fillMaxSize().background(Color(0xff6bbfc9)),
        kubriko = stateHolder.backgroundKubriko,
        windowInsets = windowInsets,
    )
    val isGameLoaded = stateHolder.backgroundLoadingManager.isGameLoaded()
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko.value,
            windowInsets = windowInsets,
        )
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}