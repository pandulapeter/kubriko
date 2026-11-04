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
 * How one part of the game refers to another, with the words tool results and the digest use for it.
 */
public enum class RelationKind(
    public val verb: String,
) {
    INHERITS_FROM("inherits from"),
    COLLIDES_WITH("collides with"),
    ADDS("adds"),
    MOVES_TOWARDS("moves towards"),
    GOES_TO("goes to"),
    PLACES("places"),
    READS("reads"),
    CHANGES("changes"),
    NAMES_IN_KOTLIN("names in Kotlin"),

    /**
     * The game's settings starting with a Scene, which only [ProjectReferences] reports.
     */
    STARTS_WITH("starts with"),
}
