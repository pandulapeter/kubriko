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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The color that marks one kind of thing everywhere it shows (the cast, the crew, the Scenes, and each Block category),
 * so a beginner learns to recognize it at a glance. Each has a deeper shade for the light theme and a brighter one for
 * the dark theme.
 */
internal enum class Accent(
    private val light: Color,
    private val dark: Color,
) {
    CAST(Color(0xFFD9534F), Color(0xFFFF8A80)),
    CREW(Color(0xFF00897B), Color(0xFF5FD4C4)),
    SCENE(Color(0xFFB7791F), Color(0xFFFFC857)),
    EVENT(Color(0xFF5B5BB5), Color(0xFFA9A9F5)),
    CODE(Color(0xFF546E7A), Color(0xFF9DB2BF)),
    MOVEMENT(Color(0xFF3B73D9), Color(0xFF6FA8FF)),
    PROPERTIES(Color(0xFF8E44C9), Color(0xFFC39BFF)),
    FLOW(Color(0xFFC2185B), Color(0xFFFF7EB6)),
    TIMERS(Color(0xFF0288A8), Color(0xFF4DD0E1)),
    AUDIO(Color(0xFF2E8B3E), Color(0xFF6FD67F)),
    ACTION(Color(0xFF2E9E4F), Color(0xFF43B866)),
    ;

    /**
     * The shade that fits the current theme.
     */
    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) dark else light

    /**
     * A faint wash of [color], for backgrounds that carry it: a selected row, an icon's badge.
     */
    val container: Color
        @Composable
        @ReadOnlyComposable
        get() = color.copy(alpha = CONTAINER_ALPHA)
}

private const val CONTAINER_ALPHA = 0.18f
