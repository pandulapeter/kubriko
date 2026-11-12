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
 * One place that refers to something about to be deleted, and what deleting it does there ([ProjectReferences]).
 *
 * @param subject What it refers to: the thing deleted, or one of its Properties.
 * @param count How many times it refers to it there: the number of Actors a Scene places, for example.
 * @param blocksInside For an if Block removed with its section, how many Blocks the section holds.
 */
public data class Usage(
    val place: UsagePlace,
    val kind: RelationKind,
    val subject: RelationSubject,
    val effect: UsageEffect,
    val count: Int = 1,
    val blocksInside: Int = 0,
) {
    /**
     * Where this usage is and how it refers to its subject, without what a delete does there: what find usages lists.
     */
    public val reference: String get() = "$place ${kind.verb} $subject" + if (count > 1) " ($count)" else ""

    override fun toString(): String = "$place ${kind.verb} $subject" + (if (count > 1) " ($count)" else "") + ": " + when (effect) {
        UsageEffect.SECTION_REMOVED -> "${effect.english} ($blocksInside inside)."
        else -> "${effect.english}."
    }
}
