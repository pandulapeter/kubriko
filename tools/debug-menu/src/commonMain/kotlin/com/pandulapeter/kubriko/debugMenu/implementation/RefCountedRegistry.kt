/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps one value per key alive for as long as at least one holder has acquired that key.
 *
 * The value is created on the first [acquire] of a key and released when the matching last [release] happens.
 * Keys are compared with [equals]. Only used from the main thread (composition effects), so it does no locking.
 */
internal class RefCountedRegistry<K : Any, V : Any>(
    private val create: (K) -> V,
    private val release: (V) -> Unit,
) {
    private val entries = mutableMapOf<K, Entry<V>>()
    private val _values = MutableStateFlow<PersistentMap<K, V>>(persistentMapOf())
    val values: StateFlow<PersistentMap<K, V>> = _values.asStateFlow()

    fun acquire(key: K) {
        val entry = entries[key]
        if (entry == null) {
            val value = create(key)
            entries[key] = Entry(value, 1)
            _values.value = _values.value.put(key, value)
        } else {
            entry.count++
        }
    }

    fun release(key: K) {
        val entry = entries[key] ?: return
        entry.count--
        if (entry.count <= 0) {
            entries.remove(key)
            _values.value = _values.value.remove(key)
            release(entry.value)
        }
    }

    private class Entry<V>(
        val value: V,
        var count: Int,
    )
}
