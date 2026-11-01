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
 * One reference between parts of the game, like Ball's collision Event naming Brick.
 *
 * @param count How many times it is made in one place: the number of Actors a Scene places, for example.
 */
internal data class Relation(
    val place: RelationPlace,
    val kind: RelationKind,
    val subject: RelationSubject,
    val count: Int = 1,
) {
    override fun toString() = "$place ${kind.verb} $subject" + if (count > 1) " ($count)" else ""
}
