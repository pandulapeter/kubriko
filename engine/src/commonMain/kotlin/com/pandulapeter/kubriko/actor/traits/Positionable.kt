/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.Body

/**
 * [Actor]s that want to have a well defined position in the Scene should implement this interface.
 */
interface Positionable : Actor {

    val body: Body
}