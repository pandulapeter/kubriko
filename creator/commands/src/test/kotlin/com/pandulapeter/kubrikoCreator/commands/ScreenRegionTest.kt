/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenRegionTest {

    @Test
    fun theMarginGrowsWithTheRegionAndStaysWithinTheView() {
        assertEquals(ScreenRegion(0.25f, 0.25f, 0.75f, 0.75f), ScreenRegion(0.375f, 0.375f, 0.625f, 0.625f).withMargin(0.5f, 0.0625f))
        assertEquals(ScreenRegion(0.4375f, 0f, 0.5703125f, 0.125f), ScreenRegion(0.5f, 0f, 0.5078125f, 0.0625f).withMargin(0.5f, 0.0625f))
        assertEquals(ScreenRegion(0f, 0f, 1f, 1f), ScreenRegion(0f, 0f, 1f, 1f).withMargin(0.5f, 0.0625f))
    }
}
