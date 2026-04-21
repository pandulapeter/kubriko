/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A container component used to group related UI elements in the Kubriko tools.
 *
 * It uses a standard [Card] with the surface color from the theme.
 *
 * @param modifier The modifier to apply to the panel.
 * @param content The composable content to display inside the panel.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    modifier = modifier,
    colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
    content = content,
)
