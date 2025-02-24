/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation_negative
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation_positive
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CloseConfirmationDialog(
    onCloseConfirmed: () -> Unit,
    onCloseCanceled: () -> Unit,
    onButtonHover: () -> Unit,
) = Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color.White,
            fontSize = 12.sp,
            text = stringResource(Res.string.close_confirmation)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpaceSquadronButton(
                onButtonPressed = onCloseConfirmed,
                title = Res.string.close_confirmation_positive,
                onPointerEnter = onButtonHover,
            )
            SpaceSquadronButton(
                onButtonPressed = onCloseCanceled,
                title = Res.string.close_confirmation_negative,
                onPointerEnter = onButtonHover,
            )
        }
    }
}