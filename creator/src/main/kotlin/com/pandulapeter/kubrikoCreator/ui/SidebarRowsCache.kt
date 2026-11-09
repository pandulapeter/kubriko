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

/**
 * The sidebar's rows, worked out again only when what they come from changes, rather than every time the sidebar's list
 * is built again (which a selection or an AI step's flash does): each value by its key, with the inputs it was made from.
 */
internal class SidebarRowsCache {
    private class Entry(val inputs: List<Any?>, val value: Any?)

    private val entries = HashMap<String, Entry>()

    /**
     * The value for [key], made by [compute] unless the one made last for it came from equal [inputs].
     */
    fun <T> valueOf(key: String, vararg inputs: Any?, compute: () -> T): T {
        val inputList = inputs.asList()
        val cached = entries[key]
        if (cached != null && cached.inputs == inputList) {
            @Suppress("UNCHECKED_CAST")
            return cached.value as T
        }
        return compute().also { entries[key] = Entry(inputList, it) }
    }
}
