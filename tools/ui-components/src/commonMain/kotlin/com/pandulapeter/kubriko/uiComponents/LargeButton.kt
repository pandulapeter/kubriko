/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeButton(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource? = null,
    isEnabled: Boolean = true,
    onButtonPressed: () -> Unit,
) = CompositionLocalProvider(LocalRippleConfiguration provides if (isEnabled) LocalRippleConfiguration.current else null) {
    FloatingActionButton(
        modifier = modifier.alpha(if (isEnabled) 1f else 0.5f).height(40.dp),
        containerColor = if (isSystemInDarkTheme()) FloatingActionButtonDefaults.containerColor else MaterialTheme.colorScheme.primary,
        elevation = if (isEnabled) FloatingActionButtonDefaults.elevation() else FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        onClick = {
            if (isEnabled) {
                onButtonPressed()
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(title),
                )
            }
            Text(
                modifier = Modifier.padding(end = 8.dp, start = if (icon == null) 8.dp else 0.dp),
                text = stringResource(title),
            )
        }
    }
}