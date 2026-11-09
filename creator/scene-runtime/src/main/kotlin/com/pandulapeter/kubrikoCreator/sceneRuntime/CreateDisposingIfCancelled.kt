/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Runs [create] on [dispatcher] (off the main thread, where what the project's code throws can't take the user
 * interface down with it), and disposes what it created, on [dispatcher] too, if the caller was cancelled meanwhile,
 * which would otherwise throw it away undisposed. [create] can stop early by checking its scope ([ensureActive]), and
 * then has to dispose what it made so far itself.
 *
 * @throws Exception What [create] threw, and [LinkageError]s too (a project class that is gone or changed since).
 */
public suspend fun <T : Any> createDisposingIfCancelled(
    dispatcher: CoroutineDispatcher,
    create: CoroutineScope.() -> T,
    dispose: (T) -> Unit,
): T {
    var created: T? = null
    try {
        withContext(dispatcher) { created = create() }
    } catch (exception: CancellationException) {
        created?.let { withContext(NonCancellable + dispatcher) { dispose(it) } }
        throw exception
    }
    return checkNotNull(created)
}
