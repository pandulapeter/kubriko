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

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.manager.ActorManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail

/**
 * Blocks until every operation issued on this [ActorManager] before the call, on the calling thread, has been applied
 * and published to [ActorManager.allActors], and all of their callbacks (`onAdded`, `dispose`, `onRemoved`) have
 * returned. It adds and then removes a sentinel actor and waits for the sentinel's `onRemoved`.
 *
 * Limits:
 * - Call it only between `start()` and `kubriko.dispose()`. Nothing is processed before `start()`, and a call
 *   during or after disposal fails on timeout.
 * - It covers [ActorManager.allActors] only, not necessarily the derived lists.
 * - The sentinel is briefly visible in [ActorManager.allActors].
 * - Never call it from an actor callback (the processor thread would wait on itself) or while a [Blocker] holds the
 *   processor. Either fails on timeout.
 */
fun ActorManager.awaitProcessed(timeoutInMilliseconds: Long = 5_000) {
    val sentinel = Sentinel()
    add(sentinel)
    remove(sentinel)
    if (!sentinel.removed.await(timeoutInMilliseconds, TimeUnit.MILLISECONDS)) {
        fail("The actor queue was not processed within $timeoutInMilliseconds ms.")
    }
}

private class Sentinel : Actor {
    val removed = CountDownLatch(1)

    override fun onRemoved() = removed.countDown()
}
