/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

/**
 * Where Kubriko Creator moved its pages and documents to, by their paths from the project's root, so that what shows one
 * follows it rather than closing. Only the latest moves are kept.
 */
public class MovedFiles {
    private val destinations = LinkedHashMap<String, String>()

    /**
     * Records that the file at [from] is at [to] now. What moved away from [to] before no longer applies, since another
     * file is there now.
     */
    public fun record(from: String, to: String): Unit = synchronized(destinations) {
        destinations.remove(to)
        destinations.remove(from)
        destinations[from] = to
        while (destinations.size > MAXIMUM_SIZE) destinations.remove(destinations.keys.first())
    }

    /**
     * Where the file that was at [path] went, following one move after another, or null if it wasn't moved.
     */
    public fun whereIs(path: String): String? = synchronized(destinations) {
        var current = path
        val visited = hashSetOf(path)
        while (true) {
            val next = destinations[current] ?: break
            if (!visited.add(next)) break
            current = next
        }
        current.takeIf { it != path }
    }

    private companion object {
        const val MAXIMUM_SIZE = 256
    }
}
