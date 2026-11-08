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
 * A file or folder of the project moved from [from] to [to], both paths from the project's root with `/` between their
 * parts; a folder's move moves everything in it.
 */
public data class PathMove(
    val from: String,
    val to: String,
) {
    /**
     * Where [path] is after this move, or null if the move doesn't touch it.
     */
    public fun applyTo(path: String): String? = when {
        path == from -> to
        path.startsWith("$from/") -> to + path.substring(from.length)
        else -> null
    }

    /**
     * The move that undoes this one.
     */
    public fun reversed(): PathMove = PathMove(to, from)
}

/**
 * Where [path] is after the first of these moves that touches it, or null if none does.
 */
public fun List<PathMove>.applyTo(path: String): String? = firstNotNullOfOrNull { it.applyTo(path) }
