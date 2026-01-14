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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.back
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_back
import kubriko.examples.game_annoyed_penguins.generated.resources.information_contents
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InfoDialog(
    modifier: Modifier = Modifier,
    onInfoButtonPressed: () -> Unit,
    onPointerEnter: () -> Unit = {},
    windowInsets: WindowInsets,
) = Box(
    modifier = modifier.fillMaxSize(),
) {
    Text(
        modifier = modifier
            .align(Alignment.Center)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(windowInsets)
            .padding(vertical = 16.dp)
            .padding(
                start = 80.dp,
                end = 16.dp,
            ),
        color = MaterialTheme.colorScheme.primary,
        text = stringResource(Res.string.information_contents),
    )
    AnnoyedPenguinsButton(
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.TopStart),
        icon = Res.drawable.ic_back,
        title = stringResource(Res.string.back),
        onButtonPressed = onInfoButtonPressed,
        onPointerEnter = onPointerEnter,
    )
}