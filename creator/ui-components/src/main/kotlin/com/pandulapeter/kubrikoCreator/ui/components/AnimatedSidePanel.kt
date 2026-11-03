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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * The side panel at the end of a row that [panel] names, if any (its splitter and itself, which [content] lays out in a
 * row). The row makes room for it at once, so what is beside it is laid out once rather than on every frame, while the
 * panel slides in from the edge; one that closes or gives way to another fades out over its neighbour.
 */
@Composable
public fun <T : Any> AnimatedSidePanel(
    panel: T?,
    content: @Composable RowScope.(T) -> Unit,
): Unit = AnimatedContent(
    targetState = panel,
    contentAlignment = Alignment.CenterEnd,
    transitionSpec = {
        (slideInHorizontally(CreatorMotion.medium()) { it / SLIDE_FRACTION } + fadeIn(CreatorMotion.medium()))
            .togetherWith(fadeOut(CreatorMotion.short()))
            .using(SizeTransform(clip = false) { _, _ -> snap() })
    },
) { shown ->
    if (shown != null) {
        Row(
            modifier = Modifier.fillMaxHeight(),
        ) {
            content(shown)
        }
    }
}

private const val SLIDE_FRACTION = 4
