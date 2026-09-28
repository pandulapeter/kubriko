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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.manager.ActorManager

/**
 * Should be implemented by [Actor]s that need to perform cleanup when removed from the scene.
 */
interface Disposable : Actor {

    /**
     * Called by the engine to release resources.
     * This is invoked immediately before [onRemoved], on the same thread.
     * Also called, without a following `onRemoved()`, for every actor still in the scene when the owning [Kubriko] instance
     * is disposed — on the thread that called `Kubriko.dispose()`.
     *
     * An exception thrown here does not prevent [onRemoved] or the rest of the batch from being applied; it is
     * rethrown afterwards on the Kubriko scope.
     *
     * The actor receives no further `update()` from ticks that start after this is called, and is not drawn in frames
     * rendered after such a tick. A frame already prepared before that tick may still draw it once, so `draw()` must not
     * fail on state released here.
     */
    fun dispose()
}