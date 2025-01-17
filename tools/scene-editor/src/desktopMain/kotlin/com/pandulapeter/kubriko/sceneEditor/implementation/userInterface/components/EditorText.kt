/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false,
    isCenterAligned: Boolean = false,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.bodySmall,
    color = color,
    text = text,
    textAlign = if (isCenterAligned) TextAlign.Center else null,
    fontWeight = if (isBold) FontWeight.Bold else null,
)

@Composable
internal fun EditorTextTitle(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier.padding(bottom = 4.dp),
    style = MaterialTheme.typography.titleSmall,
    text = text,
)

@Composable
internal fun EditorTextLabel(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier,
    style = MaterialTheme.typography.labelSmall,
    text = text,
)
