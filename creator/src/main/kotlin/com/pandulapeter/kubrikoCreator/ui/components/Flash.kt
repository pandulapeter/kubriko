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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * A background color that lights up whenever [key] becomes a new non-null value and then fades away, to show what
 * just changed.
 */
@Composable
internal fun rememberFlashColor(key: Any?): Color {
    val strength = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key != null) strength.snapTo(1f)
        strength.animateTo(0f, tween(if (key == null) FADE_OUT_DURATION else FLASH_DURATION))
    }
    return MaterialTheme.colorScheme.tertiary.copy(alpha = MAXIMUM_ALPHA * strength.value)
}

private const val FLASH_DURATION = 1800
private const val FADE_OUT_DURATION = 200
private const val MAXIMUM_ALPHA = 0.55f
