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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_chevron
import org.jetbrains.compose.resources.painterResource

/**
 * The outlined button of a menu, showing the chosen option with a chevron, filled on a [Popover] so that it doesn't
 * melt into the popover's surface.
 *
 * @param isWrapping Whether [label] wraps onto further lines when it doesn't fit, rather than being cut off.
 * @param isMissing Whether the chosen option no longer exists, which shows the button in the error color.
 */
@Composable
public fun MenuButtonFace(
    label: String,
    onClick: () -> Unit,
    isWrapping: Boolean = false,
    isMissing: Boolean = false,
): Unit = OutlinedButton(
    modifier = Modifier.heightIn(min = 32.dp),
    contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (LocalIsInPopover.current) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Unspecified,
        contentColor = if (isMissing) MaterialTheme.colorScheme.error else Color.Unspecified,
    ),
    border = if (isMissing) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonBorder(),
    onClick = onClick,
) {
    Text(
        modifier = Modifier.weight(1f, fill = false),
        text = label,
        maxLines = if (isWrapping) Int.MAX_VALUE else 1,
        overflow = if (isWrapping) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
    Icon(
        modifier = Modifier.padding(start = 4.dp).size(14.dp),
        painter = painterResource(Res.drawable.ic_chevron),
        contentDescription = null,
    )
}
