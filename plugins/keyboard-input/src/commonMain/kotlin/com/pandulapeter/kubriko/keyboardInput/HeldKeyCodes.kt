/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key

/**
 * The codes of the keys held, in a primitive array, so checking one ([contains]) allocates nothing: a `Set<Key>` boxes
 * the `Key` value class on every lookup. A few keys are held at a time, so a linear scan is the fastest lookup too.
 */
internal class HeldKeyCodes {
    private var codes = LongArray(INITIAL_CAPACITY)
    private var count = 0

    operator fun contains(key: Key): Boolean {
        val code = key.keyCode
        for (index in 0 until count) {
            if (codes[index] == code) return true
        }
        return false
    }

    fun add(key: Key) {
        if (key in this) return
        if (count == codes.size) codes = codes.copyOf(count * 2)
        codes[count++] = key.keyCode
    }

    fun remove(key: Key) {
        val code = key.keyCode
        for (index in 0 until count) {
            if (codes[index] == code) {
                codes[index] = codes[--count]
                return
            }
        }
    }

    fun clear() {
        count = 0
    }

    private companion object {
        const val INITIAL_CAPACITY = 8
    }
}
