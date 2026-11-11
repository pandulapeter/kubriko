/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a command wasn't applied, shown to the user and returned to the AI.
 *
 * @param arguments The values of the [reason]'s placeholders.
 */
public data class Rejection(
    val reason: DomainText,
    val arguments: List<String> = emptyList(),
) {
    public constructor(reason: DomainText, vararg arguments: String) : this(reason, arguments.toList())
}

/**
 * This reason, with the arguments of its message, as a rejection.
 */
internal fun Pair<DomainText, List<String>>.toRejection(): Rejection = Rejection(first, second)

/**
 * Why [index] can't be used where the valid indexes are 0 to [lastIndex], or null if it can.
 */
internal fun validateIndex(index: Int, lastIndex: Int): Rejection? = if (index in 0..lastIndex) null else Rejection(LookupProblem.INDEX_INVALID, index.toString(), lastIndex.toString())
