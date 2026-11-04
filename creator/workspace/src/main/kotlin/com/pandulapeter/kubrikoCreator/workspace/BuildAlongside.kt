/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Runs [build] while [ready] gets a device ready, returning what [ready] returned once both are done, or null as soon as
 * either fails ([build] returning false, [ready] null), cancelling the other. The build starts first, so a device found
 * missing at once still stops a build that began rather than one that never ran.
 */
internal suspend fun <T : Any> builtAlongside(build: suspend () -> Boolean, ready: suspend () -> T?): T? = coroutineScope {
    val building = async(start = CoroutineStart.UNDISPATCHED) { build() }
    val device = async { ready().also { if (it == null) building.cancel() } }
    val isBuilt = try {
        building.await()
    } catch (exception: CancellationException) {
        ensureActive()
        false
    }
    if (isBuilt) device.await() else null.also { device.cancel() }
}
