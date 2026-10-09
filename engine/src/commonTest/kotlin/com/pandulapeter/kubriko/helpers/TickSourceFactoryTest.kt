/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlin.test.Test
import kotlin.test.assertFailsWith

class TickSourceFactoryTest {

    @Test
    fun fixedFrequencyRejectsANonPositiveFrequency() {
        assertFailsWith<IllegalArgumentException> { TickSource.fixedFrequency(0) }
        assertFailsWith<IllegalArgumentException> { TickSource.fixedFrequency(-1) }
    }

    @Test
    fun fixedRateRejectsANonPositiveInterval() {
        assertFailsWith<IllegalArgumentException> { TickSource.fixedRate(0L) }
        assertFailsWith<IllegalArgumentException> { TickSource.fixedRate(-1L) }
    }
}
