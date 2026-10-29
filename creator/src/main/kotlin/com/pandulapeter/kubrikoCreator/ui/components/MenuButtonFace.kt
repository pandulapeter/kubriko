/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_chevron
import org.jetbrains.compose.resources.painterResource

/**
 * The outlined button of a menu, showing the chosen option with a chevron.
 */
@Composable
internal fun MenuButtonFace(
    label: String,
    onClick: () -> Unit,
) = OutlinedButton(
    modifier = Modifier.heightIn(min = 32.dp),
    contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    onClick = onClick,
) {
    Text(
        modifier = Modifier.weight(1f, fill = false),
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Icon(
        modifier = Modifier.padding(start = 4.dp).size(14.dp),
        painter = painterResource(Res.drawable.ic_chevron),
        contentDescription = null,
    )
}
