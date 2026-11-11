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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.foundation.ColorHex
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import com.pandulapeter.kubrikoCreator.ui.components.checkerboard

/**
 * One color of the sprite editor, over the checkerboard so a see-through one shows as such: a click chooses it for the
 * left button, a right-click for the right one ([onChoose] with whether it is the right one).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ColorSwatch(
    color: Int,
    isChosen: Boolean,
    onChoose: (isSecondary: Boolean) -> Unit,
    size: Dp = 20.dp,
) = WithTooltip(ColorHex.format(color.toLong() and 0xFFFFFFFFL)) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .checkerboard(4.dp)
            .background(Color(color))
            .border(if (isChosen) 2.dp else 1.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .onPointerEvent(PointerEventType.Press) { event -> onChoose(event.buttons.isSecondaryPressed) },
    )
}
