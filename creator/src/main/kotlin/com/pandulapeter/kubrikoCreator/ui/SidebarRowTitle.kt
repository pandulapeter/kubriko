/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * The name of a sidebar row holding folders, with the buttons opening or closing every one of them ([subfolderActions])
 * laid over its end while [isPointedAt], on the row's [background]. They take no room while hidden, so a long name
 * (`desktopWindow`) is cut only while the row is pointed at, and only as far as the buttons reach.
 */
@Composable
internal fun SidebarRowTitle(
    text: String,
    style: TextStyle,
    isPointedAt: Boolean,
    subfolderActions: SubfolderActions?,
    background: Color,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier,
    contentAlignment = Alignment.CenterStart,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (subfolderActions != null) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = isPointedAt,
            enter = fadeIn(CreatorMotion.short()),
            exit = fadeOut(CreatorMotion.short()),
        ) {
            ExpandCollapseButtons(
                modifier = Modifier
                    .background(Brush.horizontalGradient(0f to background.copy(alpha = 0f), 0.2f to background))
                    .padding(start = 16.dp),
                isShown = true,
                actions = subfolderActions,
            )
        }
    }
}
