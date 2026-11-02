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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SentValuesTest {

    @Test
    fun lateEchoesComeBackInOrder() {
        val sentValues = SentValues<String>()
        sentValues.add("a")
        sentValues.add("ab")
        assertTrue(sentValues.isEcho("a"))
        assertTrue(sentValues.isEcho("ab"))
        assertFalse(sentValues.isEcho("ab"))
    }

    @Test
    fun anEchoForgetsWhatWasSentBeforeIt() {
        val sentValues = SentValues<String>()
        sentValues.add("a")
        sentValues.add("ab")
        assertTrue(sentValues.isEcho("ab"))
        assertFalse(sentValues.isEcho("a"))
    }

    @Test
    fun aValueNeverSentForgetsEverything() {
        val sentValues = SentValues<String>()
        sentValues.add("a")
        assertFalse(sentValues.isEcho("Paddle"))
        assertFalse(sentValues.isEcho("a"))
    }
}
