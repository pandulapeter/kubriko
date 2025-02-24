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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SpaceSquadronButton(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource? = null,
    shouldShowTitle: Boolean = icon == null,
    onButtonPressed: () -> Unit,
    onPointerEnter: () -> Unit,
) {
    val alpha = remember { mutableStateOf(IDLE_BUTTON_ALPHA) }
    FloatingActionButton(
        modifier = modifier
            .spaceSquadronUIElementBorder()
            .height(40.dp)
            .run {
                if (shouldShowTitle) this else width(40.dp)
            }
            .alpha(alpha.value)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                alpha.value = 1f
                                onPointerEnter()
                            }

                            PointerEventType.Exit -> {
                                alpha.value = IDLE_BUTTON_ALPHA
                            }
                        }
                    }
                }
            },
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        contentColor = if (alpha.value == IDLE_BUTTON_ALPHA) MaterialTheme.colorScheme.onPrimary else Color.White,
        onClick = onButtonPressed,
    ) {
        if (shouldShowTitle) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = stringResource(title),
                    )
                }
                Text(
                    text = stringResource(title),
                )
            }
        } else if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(title),
            )
        }
    }
}

private const val IDLE_BUTTON_ALPHA = 0.7f