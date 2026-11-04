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
public object PropertyPlaceholders {

    private val pattern = Regex("\\{(?:([A-Z][A-Za-z0-9]*)\\.)?([a-z][A-Za-z0-9]*)\\}")

    /**
     * @param manager The Manager type it names, or null for the type's own Property.
     */
    public data class Placeholder(
        val range: IntRange,
        val manager: String?,
        val name: String,
    )

    public fun find(text: String): Sequence<PropertyPlaceholders.Placeholder> = pattern.findAll(text).map { match ->
        Placeholder(match.range, match.groupValues[1].ifEmpty { null }, match.groupValues[2])
    }

    internal fun withPropertyRenamed(text: String, manager: String?, oldName: String, newName: String) = replace(text) { placeholder ->
        if (placeholder.manager == manager && placeholder.name == oldName) placeholder.copy(name = newName) else placeholder
    }

    internal fun withManagerRenamed(text: String, oldName: String, newName: String) = replace(text) { placeholder ->
        if (placeholder.manager == oldName) placeholder.copy(manager = newName) else placeholder
    }

    /**
     * [text] with each placeholder [isDeleted] accepts written as the plain name of its Property, so the sentence still
     * reads once the Property is gone.
     */
    public fun withPlaceholdersAsText(text: String, isDeleted: (Placeholder) -> Boolean): String {
        var end = 0
        return buildString {
            find(text).forEach { placeholder ->
                append(text, end, placeholder.range.first)
                if (isDeleted(placeholder)) append(placeholder.name) else append(text, placeholder.range.first, placeholder.range.last + 1)
                end = placeholder.range.last + 1
            }
            append(text, end, text.length)
        }
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
