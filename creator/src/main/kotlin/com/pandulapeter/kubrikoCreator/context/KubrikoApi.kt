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
 * Kubriko's API for the AI, which writes Kotlin Blocks and Kotlin UI against it: an overview of the engine and its
 * plugins, taken from the repository's `CLAUDE.md` bundled with Kubriko Creator.
 */
internal object KubrikoApi {
    private const val OVERVIEW_RESOURCE = "/kubriko-overview.md"
    private val sections = listOf("## Core architecture" to "## Tools", "## Common patterns" to "## Performance")

    val overview by lazy { KubrikoApi::class.java.getResource(OVERVIEW_RESOURCE)?.readText()?.let(::overviewOf).orEmpty() }

    /**
     * The parts of [text] (the repository's `CLAUDE.md`) about using Kubriko: its architecture, plugins, and common
     * patterns, without how the repository is built.
     */
    internal fun overviewOf(text: String) = sections.mapNotNull { (start, end) ->
        val from = text.indexOf(start).takeIf { it >= 0 } ?: return@mapNotNull null
        val to = text.indexOf(end, from).takeIf { it >= 0 } ?: text.length
        text.substring(from, to).trim()
    }.joinToString("\n\n")
}
