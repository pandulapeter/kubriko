/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * How Compose resources' Gradle plugin names the accessors of the files right in one of its folders (`drawable`, `font`).
 */
internal object ResourceAccessors {
    private val accessorPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")

    /**
     * The name of the accessor of the file at [path] (from `composeResources`), as the plugin makes it (`ball-1` becomes
     * `ball_1`), or null if it isn't a file with one of [extensions] right in [folder].
     */
    fun accessorOf(path: String, folder: String, extensions: Set<String>): String? {
        val segments = path.split('/')
        if (segments.size != 2 || segments[0] != folder) return null
        val name = segments[1].substringBeforeLast('.', "")
        if (segments[1].substringAfterLast('.', "").lowercase() !in extensions) return null
        return name.replace('-', '_').let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }.takeIf(accessorPattern::matches)
    }
}
