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
import com.pandulapeter.kubriko.helpers.ManualTickSource
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import kotlin.test.fail

/**
 * A [Kubriko] instance driven by a [ManualTickSource], for tests.
 */
class ManualKubriko(
    val kubriko: Kubriko,
    val tickSource: ManualTickSource,
) {
    /**
     * The instance's [ActorManager].
     */
    val actorManager: ActorManager get() = kubriko.get()

    /**
     * Emits [count] ticks, each [deltaTimeInMilliseconds] long. The delta comes first: `tick(16)` is one 16 ms tick.
     */
    fun tick(deltaTimeInMilliseconds: Int = 16, count: Int = 1) = repeat(count) {
        tickSource.tick(deltaTimeInMilliseconds)
    }

    /**
     * Ticks once, checks [condition], sleeps 1 ms and repeats until [condition] holds. Fails the test after
     * [timeoutInMilliseconds]. Meant for work that reaches a Manager asynchronously, such as a plugin's actor
     * registration.
     */
    fun tickUntil(
        timeoutInMilliseconds: Long = 2_000,
        deltaTimeInMilliseconds: Int = 16,
        condition: () -> Boolean,
    ) {
        val startTime = System.currentTimeMillis()
        while (true) {
            tick(deltaTimeInMilliseconds)
            if (condition()) return
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime > timeoutInMilliseconds) fail("Condition not met after $elapsedTime ms.")
            Thread.sleep(1)
        }
    }

    /**
     * Disposes the underlying [Kubriko] instance.
     */
    fun dispose() = kubriko.dispose()
}

/**
 * Creates a [ManualKubriko] with the provided [managers] and a [TickSource.manual], started unless [shouldStart] is
 * false.
 *
 * Without a `KubrikoViewport` the viewport has no size. Until the engine stops putting actors of a zero-sized
 * viewport to sleep, a test that relies on `Dynamic` updates must pass
 * `ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false)`. Engine tests that need a sized viewport use the
 * engine's own `newTestKubriko`.
 */
fun newManualKubriko(
    vararg managers: Manager,
    shouldStart: Boolean = true,
): ManualKubriko {
    val tickSource = TickSource.manual()
    val kubriko = Kubriko.newInstance(
        *managers,
        tickSource = tickSource,
    )
    if (shouldStart) {
        tickSource.start()
    }
    return ManualKubriko(kubriko, tickSource)
}
