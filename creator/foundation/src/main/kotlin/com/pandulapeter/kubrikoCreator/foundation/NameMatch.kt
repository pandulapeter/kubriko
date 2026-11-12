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
 * How a name matched a query ([NameMatching]).
 *
 * @param ranges The characters of the name that matched, in order, to highlight.
 */
public data class NameMatch(
    val quality: Quality,
    val ranges: List<IntRange>,
) {
    /**
     * How close a match is, the closest first, which is how matches are ranked.
     */
    public enum class Quality {
        /** The whole name. */
        EXACT,

        /** The start of the name. */
        PREFIX,

        /** The start of one of its words (`ball` in `BigBall`). */
        WORD_START,

        /** Anywhere in it (`all` in `Ball`). */
        CONTAINS,

        /** Its letters spread over the name, the first starting a word (`brk` in `Brick`). */
        SCATTERED,
    }
}
