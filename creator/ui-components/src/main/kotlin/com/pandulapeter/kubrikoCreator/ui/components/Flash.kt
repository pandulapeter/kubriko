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
public fun Modifier.flashBackground(key: Any?, color: Color = MaterialTheme.colorScheme.tertiary): Modifier = flashBackground(rememberFlashStrength(key), color)

/**
 * Draws a background in [color] as strong as [strength] ([rememberFlashStrength]), for several things that light up
 * together from one flash, so that one shown later (a row scrolled into view) doesn't flash again.
 */
@Composable
public fun Modifier.flashBackground(strength: State<Float>, color: Color = MaterialTheme.colorScheme.tertiary): Modifier =
    drawBehind { drawRect(color.copy(alpha = FLASH_MAXIMUM_ALPHA * strength.value)) }

/**
 * How strongly a flash shows, from 1 right when [key] becomes a new non-null value down to 0 as it fades away, for
 * drawing one that [flashBackground] can't (over a canvas, around a shape). Read it while drawing, so that the flash
 * redraws without recomposing.
 *
 * @param startedAt When the flash started, in milliseconds since the epoch, for one that started before this was shown
 *   (what a Scene reopened since lit up), which then goes on from as far as it got, or null for one starting now.
 */
@Composable
public fun rememberFlashStrength(key: Any?, startedAt: Long? = null): State<Float> {
    val strength = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key == null) {
            strength.animateTo(0f, tween(FADE_OUT_DURATION))
            return@LaunchedEffect
        }
        val elapsed = startedAt?.let { (System.currentTimeMillis() - it).coerceIn(0L, FLASH_DURATION.toLong()).toInt() } ?: 0
        strength.snapTo(1f - elapsed.toFloat() / FLASH_DURATION)
        strength.animateTo(0f, tween(FLASH_DURATION - elapsed))
    }
    return strength.asState()
}

/**
 * How opaque a flash's color is at its strongest.
 */
public const val FLASH_MAXIMUM_ALPHA: Float = 0.55f

private const val FLASH_DURATION = 1800
private const val FADE_OUT_DURATION = 200
