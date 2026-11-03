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

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * Lets a dialog's content fade in while growing the last few percent to its size, instead of popping up within one frame.
 */
@Composable
public fun Modifier.dialogEntrance(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(progress) { progress.animateTo(1f, CreatorMotion.medium()) }
    return graphicsLayer {
        alpha = progress.value
        val scale = INITIAL_SCALE + (1f - INITIAL_SCALE) * progress.value
        scaleX = scale
        scaleY = scale
    }
}

private const val INITIAL_SCALE = 0.96f
