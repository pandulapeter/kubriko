/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

/**
 * Fills a template's placeholders (`{{NAME}}`) and package path tokens.
 */
internal object TemplateRenderer {

    private const val PACKAGE_PATH_TOKEN = "__PACKAGE__"
    private val placeholderPattern = Regex("\\{\\{([A-Z_]+)\\}\\}")
    private const val DOT_PREFIX = "dot-"

    fun renderContent(content: String, values: Map<String, String>) = placeholderPattern.replace(content) { match ->
        values[match.groupValues[1]] ?: throw IllegalArgumentException("The template uses an unknown placeholder: ${match.value}.")
    }

    /**
     * Resources can't reliably be dot files, so a template stores `.gitignore` as `dot-gitignore`, and `.kubriko/` as `dot-kubriko/`.
     */
    fun renderPath(path: String, packageName: String) = path.split('/').joinToString("/") { segment ->
        if (segment.startsWith(DOT_PREFIX)) "." + segment.removePrefix(DOT_PREFIX) else segment
    }.replace(PACKAGE_PATH_TOKEN, packageName.replace('.', '/'))
}
