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

    /**
     * An Actor type reference Property of a type starting as an Actor type.
     */
    STARTS_AS("has a Property starting as"),

    /**
     * A Scene saving an Actor type reference Property's value naming an Actor type, which only [ProjectReferences] reports.
     */
    SAVES_REFERENCE_TO("saves a reference to"),
    COLLIDES_WITH("collides with"),

    /**
     * A Manager type's actorAdded or actorRemoved Event, about the Actors of a type.
     */
    REACTS_TO("reacts to"),

    /**
     * An If Block counting the Actors of a type (the actorCount condition).
     */
    COUNTS("counts"),

    /**
     * An If Block measuring the distance to the nearest Actor of a type (the distance condition), naming it.
     */
    MEASURES("measures the distance to"),
    ADDS("adds"),
    MOVES_TOWARDS("moves towards"),
    GOES_TO("goes to"),
    PLACES("places"),

    /**
     * A Scene giving the Properties of a Manager type values when it starts.
     */
    GIVES_VALUES_TO("gives values to"),
    READS("reads"),

    /**
     * A Block or condition reading a Property as one of its values, of any kind
     * ([com.pandulapeter.kubrikoCreator.actors.ValueTransform]): a number, a color, an Actor type, a text, or a toggle.
     */
    READS_VALUE("reads the value of"),
    CHANGES("changes"),
    NAMES_IN_KOTLIN("names in Kotlin"),

    /**
     * The game's settings starting with a Scene, which only [ProjectReferences] reports.
     */
    STARTS_WITH("starts with"),

    /**
     * A page's or document's link to what it names, which only [ProjectReferences] reports.
     */
    MENTIONS("mentions"),

    /**
     * A timerFinished Event, or a Block starting or stopping a Timer, which only [ProjectReferences] reports.
     */
    USES_TIMER("uses"),

    /**
     * An Actor type's sprite, which only [ProjectReferences] reports.
     */
    DRAWN_AS("is drawn as"),

    /**
     * A Block playing a sound or music, or a Scene's music, which only [ProjectReferences] reports.
     */
    PLAYS("plays"),

    /**
     * A page's image of a sprite, which only [ProjectReferences] reports.
     */
    SHOWS("shows"),

    /**
     * A type, Scene, or page in a folder, which only deleting the folder reports.
     */
    IS_IN("is in"),
}
