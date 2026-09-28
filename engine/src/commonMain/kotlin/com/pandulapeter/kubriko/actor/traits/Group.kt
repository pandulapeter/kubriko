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
 *
 * Nested groups are flattened; an actor reachable through several groups (or through a cycle of groups) is added
 * once. Actors are compared with `equals`, so actors should not override it.
 * A child shared by several groups is added once; removing any of those groups removes it.
 */
interface Group : Actor {

    /**
     * The list of actors belonging to this group.
     */
    val actors: List<Actor>
}