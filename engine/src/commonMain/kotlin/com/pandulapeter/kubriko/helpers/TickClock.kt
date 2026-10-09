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

import kotlin.time.TimeSource

/**
 * The clock a coroutine-based [TickSource] measures its ticks with, in nanoseconds on an arbitrary origin. A primitive
 * return, so that reading it on every tick never boxes; tests pass one that follows their scheduler's virtual time.
 */
internal fun interface TickClock {
    fun nowInNanoseconds(): Long

    /** The monotonic system clock, measured from when it was first used. */
    object Monotonic : TickClock {
        private val start = TimeSource.Monotonic.markNow()

        override fun nowInNanoseconds() = start.elapsedNow().inWholeNanoseconds
    }
}
