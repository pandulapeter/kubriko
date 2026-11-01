/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Runs [create] on [dispatcher], where what it throws can't take the user interface down with it, and disposes what it
 * created if the caller was cancelled meanwhile, so nothing is left undisposed.
 *
 * @throws Exception What [create] threw, and [LinkageError]s too (a project class that is gone or changed since).
 */
internal suspend fun <T : Any> createOffMainThread(
    dispatcher: CoroutineDispatcher,
    create: () -> T,
    dispose: (T) -> Unit,
): T {
    var created: T? = null
    try {
        withContext(dispatcher) { created = create() }
    } catch (exception: CancellationException) {
        created?.let(dispose)
        throw exception
    }
    return checkNotNull(created)
}
