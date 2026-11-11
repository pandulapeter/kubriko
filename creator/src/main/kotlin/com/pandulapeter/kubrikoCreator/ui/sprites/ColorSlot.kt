/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One of the two colors the mouse buttons draw with, named by [label], large; a click picks another ([onPick]).
 */
@Composable
internal fun ColorSlot(
    label: StringResource,
    color: Int,
    onPick: () -> Unit,
) = Column(
    modifier = Modifier.width(72.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    ColorSwatch(color = color, isChosen = false, onChoose = { onPick() }, size = 40.dp)
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
}
