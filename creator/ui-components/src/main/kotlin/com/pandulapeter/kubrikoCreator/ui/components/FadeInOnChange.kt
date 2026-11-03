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
 * Fades this element in whenever [key] changes, so content that replaces other content (another type, another Scene)
 * arrives instead of popping up. Unlike a crossfade, it never keeps the old content around, which for a Scene would mean
 * two live previews.
 */
@Composable
public fun Modifier.fadeInOnChange(key: Any?): Modifier {
    val alpha = remember(key) { Animatable(0f) }
    LaunchedEffect(alpha) { alpha.animateTo(1f, CreatorMotion.medium()) }
    return graphicsLayer { this.alpha = alpha.value }
}
