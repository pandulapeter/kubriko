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

import com.pandulapeter.kubrikoCreator.foundation.NameMatching

/**
 * The [options] whose [titleOf] (or one of [alsoMatching]'s names) matches [query] ([NameMatching]), the closest matches
 * first, otherwise in their order.
 */
public fun <T> matchingOptions(
    query: String,
    options: List<T>,
    titleOf: (T) -> String,
    alsoMatching: (T) -> String? = { null },
): List<T> {
    if (query.isBlank()) return options
    return options
        .mapNotNull { option ->
            listOfNotNull(titleOf(option), alsoMatching(option))
                .mapNotNull { NameMatching.match(it, query)?.quality }
                .minOrNull()
                ?.let { quality -> option to quality }
        }
        .sortedBy { (_, quality) -> quality }
        .map { (option, _) -> option }
}
