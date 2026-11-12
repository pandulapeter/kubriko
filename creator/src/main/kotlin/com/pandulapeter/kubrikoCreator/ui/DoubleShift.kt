/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.input.key.Key

/**
 * Tells Shift pressed twice in quick succession, with no other key between, as IDEs open their search with: a Shift held
 * while typing a capital, or pressed and released with another key, never counts.
 *
 * @param now The time in milliseconds on a clock that only moves forward, which the system clock's adjustments don't
 *   move.
 */
internal class DoubleShift(
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private var lastRelease: Long? = null
    private var isPressedAlone = false

    /**
     * Notes [key] going down ([isDown]) or up.
     *
     * @return Whether it was the second press of a double Shift.
     */
    fun onKey(key: Key, isDown: Boolean): Boolean {
        if (key != Key.ShiftLeft && key != Key.ShiftRight) {
            if (isDown) reset()
            return false
        }
        if (!isDown) {
            lastRelease = if (isPressedAlone) now() else null
            isPressedAlone = false
            return false
        }
        if (isPressedAlone) return false
        val release = lastRelease
        if (release != null && now() - release <= WINDOW_IN_MILLISECONDS) {
            reset()
            return true
        }
        isPressedAlone = true
        return false
    }

    private fun reset() {
        lastRelease = null
        isPressedAlone = false
    }

    private companion object {
        const val WINDOW_IN_MILLISECONDS = 400L
    }
}
