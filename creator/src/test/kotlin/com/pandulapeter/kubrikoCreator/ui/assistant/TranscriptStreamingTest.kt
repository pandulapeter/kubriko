/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class TranscriptStreamingTest {

    @Test
    fun aThrottledFlowEmitsTheFirstValueAtOnceAndTheLastOneAlways() = runTest {
        val chunks = flow {
            repeat(100) {
                emit(it)
                delay(1)
            }
        }
        val emitted = chunks.throttled(50.milliseconds).toList()
        assertEquals(0, emitted.first())
        assertEquals(99, emitted.last())
        assertTrue(emitted.size <= 4, "Emitted ${emitted.size} values")
    }

    @Test
    fun theListStopsFollowingWhenScrolledUpAndFollowsAgainAtTheEnd() {
        val top = TranscriptScroll(3, 10)
        val further = TranscriptScroll(3, 40)
        assertTrue(followsEnd(wasFollowing = true, previous = top, current = top, canScrollForward = true))
        assertFalse(followsEnd(wasFollowing = true, previous = further, current = top, canScrollForward = true))
        assertFalse(followsEnd(wasFollowing = false, previous = top, current = further, canScrollForward = true))
        assertTrue(followsEnd(wasFollowing = false, previous = top, current = further, canScrollForward = false))
        assertFalse(followsEnd(wasFollowing = true, previous = TranscriptScroll(4, 0), current = TranscriptScroll(3, 900), canScrollForward = true))
    }
}
