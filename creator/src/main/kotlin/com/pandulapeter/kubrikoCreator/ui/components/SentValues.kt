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

/**
 * The values a buffered field reported and hasn't seen come back from the model yet, telling its own late echoes from
 * changes made elsewhere (by undo, by the AI, or by another item taking its place).
 */
internal class SentValues<T> {
    private val pending = ArrayDeque<T>()

    fun add(value: T) = pending.addLast(value)

    fun clear() = pending.clear()

    /**
     * Whether [value] is one the field reported, forgetting it and everything reported before it; forgets all of them
     * when it isn't.
     */
    fun isEcho(value: T): Boolean {
        val index = pending.indexOf(value)
        if (index < 0) {
            pending.clear()
            return false
        }
        repeat(index + 1) { pending.removeFirst() }
        return true
    }
}
