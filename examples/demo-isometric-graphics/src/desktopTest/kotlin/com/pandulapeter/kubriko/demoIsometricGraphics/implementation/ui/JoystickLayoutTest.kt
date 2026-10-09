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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JoystickLayoutTest {

    private val layout = JoystickLayout(
        isEnabled = true,
        visualRadiusPx = 64f,
        maxRadiusPx = 64f,
        triggerRadiusPx = 128f,
        paddingPx = 16f,
        leftInsetPx = 10f,
        bottomInsetPx = 20f,
    )

    @Test
    fun centerSitsPaddingAndVisualRadiusAwayFromTheInsetBottomLeftCorner() {
        assertEquals(Offset(90f, 900f), layout.center(VIEWPORT_HEIGHT))
    }

    @Test
    fun theVisualCenterIsWithinTheTriggerRegion() {
        assertTrue(layout.isWithinTriggerRegion(Offset(90f, 900f), VIEWPORT_HEIGHT))
    }

    @Test
    fun aPointInsideTheTriggerRadiusIsWithinTheTriggerRegion() {
        assertTrue(layout.isWithinTriggerRegion(Offset(170f, 820f), VIEWPORT_HEIGHT))
    }

    @Test
    fun aPointJustOutsideTheTriggerRadiusIsNotWithinTheTriggerRegion() {
        assertFalse(layout.isWithinTriggerRegion(Offset(219f, 900f), VIEWPORT_HEIGHT))
        assertFalse(layout.isWithinTriggerRegion(Offset(90f, 771f), VIEWPORT_HEIGHT))
    }

    @Test
    fun aPointInTheBottomLeftInsetStripIsClampedIntoTheTriggerRegion() {
        assertTrue(layout.isWithinTriggerRegion(Offset(5f, 995f), VIEWPORT_HEIGHT))
    }

    private companion object {
        const val VIEWPORT_HEIGHT = 1000f
    }
}
