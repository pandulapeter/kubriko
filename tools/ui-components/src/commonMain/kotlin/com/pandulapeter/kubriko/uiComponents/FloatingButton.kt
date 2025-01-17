/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun FloatingButton(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    isSelected: Boolean = false,
    contentDescription: String? = null,
    onButtonPressed: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    FloatingActionButton(
        modifier = modifier.size(40.dp),
        containerColor = containerColor,
        onClick = onButtonPressed,
    ) {
        preloadedImageVector(icon).value?.let { iconImageVector ->
            Icon(
                imageVector = iconImageVector,
                tint = contentColorFor(containerColor),
                contentDescription = contentDescription,
            )
        }
    }
}