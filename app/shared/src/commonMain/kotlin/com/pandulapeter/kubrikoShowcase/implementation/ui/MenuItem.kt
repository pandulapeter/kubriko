/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MenuItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: StringResource,
    subtitle: StringResource,
    onSelected: () -> Unit,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .selectable(
            selected = isSelected,
            onClick = onSelected,
        )
        .background(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        )
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues())
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val contentColor = if (isSelected) contentColorFor(MaterialTheme.colorScheme.primaryContainer) else LocalContentColor.current
    Text(
        modifier = Modifier.fillMaxWidth(),
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        text = stringResource(title),
    )
    Text(
        modifier = Modifier.fillMaxWidth(),
        color = contentColor.copy(alpha = 0.75f),
        style = MaterialTheme.typography.labelSmall,
        text = stringResource(subtitle),
    )
}
