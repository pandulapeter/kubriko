/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

/**
 * A request to light up [place] in what the center shows, as quick open and find usages go to a place. It compares by
 * identity, so going to the same place again lights it up again.
 */
public class Reveal(
    public val place: RevealedPlace,
)

/**
 * A place in a type or a Scene that the center can light up.
 */
public sealed interface RevealedPlace {

    /**
     * A type, or one of its Events (with [eventIndex]) or Blocks (with [blockIndex] too), or one of its Properties.
     */
    public data class InType(
        val type: String,
        val eventIndex: Int? = null,
        val blockIndex: Int? = null,
        val property: String? = null,
    ) : RevealedPlace

    /**
     * Actors of a Scene, by their IDs.
     */
    public data class InScene(
        val scene: String,
        val actorIds: List<String>,
    ) : RevealedPlace
}
