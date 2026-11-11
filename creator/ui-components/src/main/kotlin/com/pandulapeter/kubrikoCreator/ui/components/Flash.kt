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
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * Draws a background that lights up in [color] whenever [key] becomes a new non-null value and then fades away, to show
 * what just changed. The animation is read while drawing, so it doesn't recompose what it is on.
 */
@Composable
public fun Modifier.flashBackground(key: Any?, color: Color = MaterialTheme.colorScheme.tertiary): Modifier {
    val strength = rememberFlashStrength(key)
    return drawBehind { drawRect(color.copy(alpha = FLASH_MAXIMUM_ALPHA * strength.value)) }
}

/**
 * How strongly a flash shows, from 1 right when [key] becomes a new non-null value down to 0 as it fades away, for
 * drawing one that [flashBackground] can't (over a canvas, around a shape). Read it while drawing, so that the flash
 * redraws without recomposing.
 */
@Composable
public fun rememberFlashStrength(key: Any?): State<Float> {
    val strength = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key != null) strength.snapTo(1f)
        strength.animateTo(0f, tween(if (key == null) FADE_OUT_DURATION else FLASH_DURATION))
    }
    return strength.asState()
}

/**
 * How opaque a flash's color is at its strongest.
 */
public const val FLASH_MAXIMUM_ALPHA: Float = 0.55f

private const val FLASH_DURATION = 1800
private const val FADE_OUT_DURATION = 200
