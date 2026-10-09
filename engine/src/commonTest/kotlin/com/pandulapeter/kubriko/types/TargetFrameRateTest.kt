/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TargetFrameRateTest {

    @Test
    fun limitRequiresAPositiveRate() {
        assertFailsWith<IllegalArgumentException> { TargetFrameRate.Limit(0) }
        assertFailsWith<IllegalArgumentException> { TargetFrameRate.Limit(-30) }
    }

    @Test
    fun displayDividerRequiresAPositiveDivisor() {
        assertFailsWith<IllegalArgumentException> { TargetFrameRate.DisplayDivider(0) }
        assertFailsWith<IllegalArgumentException> { TargetFrameRate.DisplayDivider(-2) }
    }

    @Test
    fun smallestValidValuesAreAccepted() {
        assertEquals(1, TargetFrameRate.Limit(1).framesPerSecond)
        assertEquals(1, TargetFrameRate.DisplayDivider(1).divisor)
    }
}
