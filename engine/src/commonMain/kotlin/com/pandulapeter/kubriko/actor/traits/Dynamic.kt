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
 * Should be implemented by [Actor]s that want to hook into the game loop.
 */
interface Dynamic : Actor {

    /**
     * If true, the engine will keep updating this actor even if it's far outside the viewport.
     */
    val isAlwaysActive: Boolean get() = false

    /**
     * Called on every frame while the game is running.
     *
     * @param deltaTimeInMilliseconds The time elapsed since the last frame.
     */
    fun update(deltaTimeInMilliseconds: Int)
}