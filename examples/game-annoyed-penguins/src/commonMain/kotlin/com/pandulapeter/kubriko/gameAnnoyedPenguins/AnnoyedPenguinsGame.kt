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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.img_logo
import org.jetbrains.compose.resources.painterResource

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
) = MaterialTheme {
    stateHolder as AnnoyedPenguinsGameStateHolderImpl
    KubrikoViewport(
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
    Image(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        painter = painterResource(Res.drawable.img_logo),
        contentScale = ContentScale.Inside,
        contentDescription = null,
    )
}