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
 * What deleting something does to a [Usage] of it, with the words tool results use for it.
 */
public enum class UsageEffect(
    public val english: String,
) {
    BLOCK_REMOVED("the Block is removed"),

    /**
     * An if Block whose condition reads what is deleted, removed with every Block up to its end Block, else Blocks included.
     */
    SECTION_REMOVED("the if Block is removed with the Blocks inside it"),
    EVENT_REMOVED("the Event is removed with its Blocks"),
    ACTORS_REMOVED("its Actors are removed"),
    PLACEHOLDER_TO_TEXT("the placeholder becomes plain text"),

    /**
     * Kotlin Kubriko Creator never rewrites, so the build fails until it is changed by hand.
     */
    KOTLIN_TO_CHANGE("left as it is, so the build fails until it is changed"),

    /**
     * What makes the delete refused: a child Actor type, or the game starting with the Scene.
     */
    PREVENTS_DELETING("prevents deleting it"),
}
