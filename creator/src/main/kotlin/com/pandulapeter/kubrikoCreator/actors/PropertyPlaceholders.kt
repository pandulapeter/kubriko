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
 * The placeholders of Log Blocks and on-screen texts: `{name}` for a Property of the type itself, and `{Other.name}`
 * for a Property of the Manager type Other.
 */
internal object PropertyPlaceholders {

    private val pattern = Regex("\\{(?:([A-Z][A-Za-z0-9]*)\\.)?([a-z][A-Za-z0-9]*)\\}")

    /**
     * @param manager The Manager type it names, or null for the type's own Property.
     */
    data class Placeholder(
        val range: IntRange,
        val manager: String?,
        val name: String,
    )

    fun find(text: String) = pattern.findAll(text).map { match ->
        Placeholder(match.range, match.groupValues[1].ifEmpty { null }, match.groupValues[2])
    }

    fun withPropertyRenamed(text: String, manager: String?, oldName: String, newName: String) = replace(text) { placeholder ->
        if (placeholder.manager == manager && placeholder.name == oldName) placeholder.copy(name = newName) else placeholder
    }

    fun withManagerRenamed(text: String, oldName: String, newName: String) = replace(text) { placeholder ->
        if (placeholder.manager == oldName) placeholder.copy(manager = newName) else placeholder
    }

    private fun replace(text: String, change: (Placeholder) -> Placeholder): String {
        var end = 0
        return buildString {
            find(text).forEach { placeholder ->
                append(text, end, placeholder.range.first)
                val changed = change(placeholder)
                append('{')
                changed.manager?.let { append(it).append('.') }
                append(changed.name).append('}')
                end = placeholder.range.last + 1
            }
            append(text, end, text.length)
        }
    }
}
