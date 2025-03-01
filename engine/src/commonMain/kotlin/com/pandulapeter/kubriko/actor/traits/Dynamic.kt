/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

/**
 * Should be implemented by [Actor]s that want to hook into the game loop.
 */
interface Dynamic : Actor {

    /**
     * The engine stops updating [Positionable] Dynamic Actors if they are too far away from the viewport.
     * Setting this flag to {true} prevents that behavior.
     */
    val isAlwaysActive: Boolean get() = false

    /**
     * Called in every frame unless the game state is paused.
     *
     * @param deltaTimeInMilliseconds - The number of milliseconds since the previous frame.
     */
    fun update(deltaTimeInMilliseconds: Int)
}