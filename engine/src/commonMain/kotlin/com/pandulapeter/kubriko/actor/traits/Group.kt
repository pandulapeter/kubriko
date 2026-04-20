/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

/**
 * Represents an actor that contains a list of other actors.
 * It is useful for adding or removing multiple Actors simultaneously to / from the scene.
 */
interface Group : Actor {

    /**
     * The list of actors belonging to this group.
     */
    val actors: List<Actor>
}