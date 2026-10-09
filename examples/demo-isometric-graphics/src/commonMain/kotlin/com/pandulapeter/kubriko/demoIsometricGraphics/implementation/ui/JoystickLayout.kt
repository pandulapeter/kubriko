/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui

import androidx.compose.ui.geometry.Offset

/**
 * The on-screen joystick's geometry in pixels, published by the UI as one immutable snapshot so pointer
 * handling never sees a half-updated layout.
 */
internal data class JoystickLayout(
    val isEnabled: Boolean,
    val visualRadiusPx: Float,
    val maxRadiusPx: Float,
    val triggerRadiusPx: Float,
    val paddingPx: Float,
    val leftInsetPx: Float,
    val bottomInsetPx: Float,
) {
    fun center(viewportHeight: Float) = Offset(
        x = leftInsetPx + paddingPx + visualRadiusPx,
        y = viewportHeight - bottomInsetPx - paddingPx - visualRadiusPx,
    )

    fun isWithinTriggerRegion(point: Offset, viewportHeight: Float): Boolean {
        val center = center(viewportHeight)
        // Clamp toward the bottom-left screen corner so touches in the inset / edge strip
        // (left of and below the visual center) still trigger the joystick. This keeps it
        // usable outside the system window insets while staying a bounded circle toward up/right.
        val dx = (point.x - center.x).coerceAtLeast(0f)
        val dy = (point.y - center.y).coerceAtMost(0f)
        return (dx * dx + dy * dy) <= (triggerRadiusPx * triggerRadiusPx)
    }
}
