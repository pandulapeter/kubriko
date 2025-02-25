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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WallbreakerTextButton(
    modifier: Modifier = Modifier,
    onButtonPressed: () -> Unit,
    onPointerEnter: () -> Unit,
    stringResource: StringResource,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val resolvedContainerColor = containerColor ?: if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary
    val scale = remember { mutableStateOf(1f) }
    FloatingActionButton(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .height(32.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                scale.value = 0.9f
                                onPointerEnter()
                            }

                            PointerEventType.Exit -> {
                                scale.value = 1f
                            }
                        }
                    }
                }
            },
        containerColor = resolvedContainerColor,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary,
        onClick = onButtonPressed,
    ) {
        Text(
            modifier = Modifier
                .defaultMinSize(minWidth = 72.dp)
                .padding(horizontal = 16.dp)
                .scale(scale.value),
            textAlign = TextAlign.Center,
            text = stringResource(stringResource),
        )
    }
}