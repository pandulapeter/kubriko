/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

/**
 * Represents an object in the game world managed by [ActorManager].
 * Actors can have various traits and behaviors defined by implementing additional interfaces.
 */
interface Actor {

    /**
     * Called from the main thread right before the actor is added to the [ActorManager].
     *
     * @param kubriko The [Kubriko] instance the actor was added to. Could be used to get references to [Manager] instances.
     */
    fun onAdded(kubriko: Kubriko) = Unit

    /**
     * Called from the main thread right after the actor is removed from the [ActorManager].
     */
    fun onRemoved() = Unit
}