/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

/**
 * The last value worked out, kept while its key stays the same.
 */
internal class KeyedCache<T> {
    private var key: Any? = null
    private var value: T? = null

    @Synchronized
    fun get(key: Any, compute: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        if (key == this.key) return value as T
        return compute().also {
            this.key = key
            value = it
        }
    }
}
