/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testFixtures

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.actor.traits.Dynamic
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * An actor that counts its lifecycle callbacks and updates, and optionally runs an action in each callback.
 */
open class CountingActor(
    var onAddedAction: (() -> Unit)? = null,
    var onRemovedAction: (() -> Unit)? = null,
    var disposeAction: (() -> Unit)? = null,
) : Actor, Dynamic, Disposable {
    /** The number of times [onAdded] ran. */
    val added = AtomicInteger()

    /** The number of times [onRemoved] ran. */
    val removed = AtomicInteger()

    /** The number of times [dispose] ran. */
    val disposed = AtomicInteger()

    /** The number of times [update] ran. */
    val updates = AtomicInteger()

    /** The thread [onAdded] last ran on. */
    val onAddedThread = AtomicReference<Thread?>(null)

    override fun onAdded(kubriko: Kubriko) {
        onAddedThread.set(Thread.currentThread())
        added.incrementAndGet()
        onAddedAction?.invoke()
    }

    override fun onRemoved() {
        removed.incrementAndGet()
        onRemovedAction?.invoke()
    }

    override fun dispose() {
        disposed.incrementAndGet()
        disposeAction?.invoke()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        updates.incrementAndGet()
    }
}
