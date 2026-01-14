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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun AnnoyedPenguinsButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: DrawableResource? = null,
    onButtonPressed: () -> Unit,
    onPointerEnter: () -> Unit,
) {
    val isActive = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isActive.value) 1.125f else 1f)
    FloatingActionButton(
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .run { if (icon == null) defaultMinSize(minWidth = 72.dp) else width(40.dp) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                isActive.value = true
                                onPointerEnter()
                            }

                            PointerEventType.Exit -> {
                                isActive.value = false
                            }
                        }
                    }
                }
            },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        onClick = onButtonPressed,
    ) {
        if (icon == null) Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = title,
        )
        else Icon(
            painter = painterResource(icon),
            contentDescription = title,
        )
    }
}