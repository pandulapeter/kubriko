/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

/**
 * The [options] whose [titleOf] (or one of [alsoMatching]'s names) contains [query], with or without its spaces: exact
 * matches first, then those starting with it, otherwise in their order.
 */
public fun <T> matchingOptions(
    query: String,
    options: List<T>,
    titleOf: (T) -> String,
    alsoMatching: (T) -> String? = { null },
): List<T> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return options
    val compact = trimmed.replace(" ", "")
    fun names(option: T) = listOfNotNull(titleOf(option), alsoMatching(option))
    return options
        .filter { option -> names(option).any { it.contains(trimmed, ignoreCase = true) || it.contains(compact, ignoreCase = true) } }
        .sortedBy { option ->
            when {
                names(option).any { it.equals(trimmed, ignoreCase = true) || it.equals(compact, ignoreCase = true) } -> 0
                names(option).any { it.startsWith(trimmed, ignoreCase = true) || it.startsWith(compact, ignoreCase = true) } -> 1
                else -> 2
            }
        }
}
