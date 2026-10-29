/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

/**
 * Keys that follow the items of a list from one version to the next, for items without an identity of their own
 * (Blocks, Events), so that a lazy list can animate moves and tell new items apart. Between two versions, the common
 * start and end keep their keys; in between, an item equal to an old one takes its key (a move), the others take the
 * keys of the old ones left, in order (an edit), and those left over get new keys, which count as added for
 * [ADDED_DURATION_IN_NANOSECONDS] (except on the first version).
 */
internal class ListItemKeys<T>(
    private val clock: () -> Long = System::nanoTime,
) {
    private var items: List<T>? = null
    private var keys: List<Long> = emptyList()
    private var nextKey = 0L
    private val addedAt = HashMap<Long, Long>()

    /**
     * The keys of [next], index by index.
     */
    fun update(next: List<T>): List<Long> {
        val previous = items
        if (previous == next) return keys
        val previousItems = previous.orEmpty()
        val nextKeys = LongArray(next.size)
        var start = 0
        while (start < previousItems.size && start < next.size && previousItems[start] == next[start]) {
            nextKeys[start] = keys[start]
            start++
        }
        var end = 0
        while (end < previousItems.size - start && end < next.size - start && previousItems[previousItems.lastIndex - end] == next[next.lastIndex - end]) {
            nextKeys[next.lastIndex - end] = keys[previousItems.lastIndex - end]
            end++
        }
        val unmatched = (start until previousItems.size - end).toMutableList()
        val withoutEqual = ArrayList<Int>()
        for (index in start until next.size - end) {
            val match = unmatched.indexOfFirst { previousItems[it] == next[index] }
            if (match >= 0) nextKeys[index] = keys[unmatched.removeAt(match)] else withoutEqual += index
        }
        val now = clock()
        addedAt.values.removeAll { now - it >= ADDED_DURATION_IN_NANOSECONDS }
        for (index in withoutEqual) {
            nextKeys[index] = if (unmatched.isNotEmpty()) {
                keys[unmatched.removeAt(0)]
            } else {
                nextKey++.also { if (previous != null) addedAt[it] = now }
            }
        }
        items = next
        keys = nextKeys.toList()
        return keys
    }

    /**
     * Whether the item of [key] was added by one of the last updates, recently enough to be pointed out.
     */
    fun isRecentlyAdded(key: Long) = addedAt[key]?.let { clock() - it < ADDED_DURATION_IN_NANOSECONDS } == true

    companion object {
        const val ADDED_DURATION_IN_NANOSECONDS = 2_000_000_000L
    }
}
