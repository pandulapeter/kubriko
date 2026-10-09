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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * An actor whose [onAdded] signals [entered] and then blocks the actor processor until [release] is counted down
 * (at most 5 s). Used to force several operations into one batch: add it, await [entered], issue the operations, then
 * release it.
 */
class Blocker : Actor {
    /** Counted down when [onAdded] starts. */
    val entered = CountDownLatch(1)

    /** Count it down to let [onAdded] return. */
    val release = CountDownLatch(1)

    override fun onAdded(kubriko: Kubriko) {
        entered.countDown()
        release.await(5, TimeUnit.SECONDS)
    }
}
