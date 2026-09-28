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
 *
 * An actor added and removed before the batch is applied still receives [onAdded] followed by [onRemoved]; an actor
 * removed and re-added in one batch stays in the scene without either callback.
 */
interface Actor {

    /**
     * Called right before the actor is added to the [ActorManager]. Every Manager of the instance is initialized by the time this is called. Called on the background
     * batch thread, or — for operations issued before the instance started — on the thread that called
     * `TickSource.start()` (the main thread when `KubrikoViewport` starts it). Either way, anything main-thread-confined
     * must be dispatched explicitly.
     *
     * An exception thrown here does not prevent the rest of the batch from being applied; it is rethrown afterwards
     * on the Kubriko scope.
     *
     * @param kubriko The [Kubriko] instance the actor was added to. Could be used to get references to [Manager] instances.
     */
    fun onAdded(kubriko: Kubriko) = Unit

    /**
     * Called right after the actor is removed from the [ActorManager]. Every Manager of the instance is initialized by the time this is called. Called on the background
     * batch thread, or — for operations issued before the instance started — on the thread that called
     * `TickSource.start()` (the main thread when `KubrikoViewport` starts it). Either way, anything main-thread-confined
     * must be dispatched explicitly.
     *
     * An exception thrown here does not prevent the rest of the batch from being applied; it is rethrown afterwards
     * on the Kubriko scope.
     *
     * The actor receives no further `update()` from ticks that start after this is called, and is not drawn in frames
     * rendered after such a tick. A frame already prepared before that tick may still draw it once, so `draw()` must not
     * fail on state released here.
     */
    fun onRemoved() = Unit
}