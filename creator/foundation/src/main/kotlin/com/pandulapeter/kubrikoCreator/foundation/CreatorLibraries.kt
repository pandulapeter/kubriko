/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

/**
 * The versions of the libraries Kubriko Creator runs with (`group:module` to version), bundled at build time from its
 * runtime classpath. A project's classes loaded next to Creator's share them ([com.pandulapeter.kubrikoCreator.scenes.SharedLibraries]).
 */
public object CreatorLibraries {
    private const val RESOURCE_NAME = "creator-libraries.txt"

    private val bundled by lazy {
        parse(
            CreatorLibraries::class.java.classLoader.getResourceAsStream(RESOURCE_NAME)?.use { it.readBytes().decodeToString() }
                ?: error("$RESOURCE_NAME is missing from the app's resources."),
        )
    }

    /**
     * The bundled libraries, read once.
     */
    public fun load(): Map<String, String> = bundled

    /**
     * The `group:module=version` lines of [text] as a map, skipping those without a version.
     */
    public fun parse(text: String): Map<String, String> = parseLines(text.lines())

    /**
     * `group:module=version` [lines] as a map, skipping those without a version.
     */
    public fun parseLines(lines: List<String>): Map<String, String> = lines
        .filter { '=' in it }
        .associate { it.substringBeforeLast('=').trim() to it.substringAfterLast('=').trim() }
}
