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
import com.pandulapeter.kubriko.manager.ActorManager

/**
 * Should be implemented by [Actor]s that need to perform cleanup when removed from the scene.
 */
interface Disposable : Actor {

    /**
     * Called by the engine to release resources.
     * This is invoked immediately before [onRemoved].
     */
    fun dispose()
}