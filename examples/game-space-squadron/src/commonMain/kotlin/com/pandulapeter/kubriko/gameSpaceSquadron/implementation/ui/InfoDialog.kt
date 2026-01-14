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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.information_contents
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InfoDialog() = Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(top = 48.dp)
) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .background(
                color = Color.Black.copy(alpha = 0.75f),
                shape = SpaceSquadronUIElementShape,
            )
            .spaceSquadronUIElementBorder()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color.White,
            fontSize = 12.sp,
            text = stringResource(Res.string.information_contents)
        )
    }
}