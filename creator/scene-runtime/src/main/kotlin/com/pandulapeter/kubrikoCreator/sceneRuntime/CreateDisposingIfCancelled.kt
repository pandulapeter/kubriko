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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs [create] on [dispatcher] (off the main thread, where what the project's code throws can't take the user
 * interface down with it), and if the caller was cancelled meanwhile, disposes what it created in the background
 * ([disposeInBackground]) rather than throwing it away undisposed.
 *
 * @throws Exception What [create] threw, and [LinkageError]s too (a project class that is gone or changed since).
 */
public suspend fun <T : Any> createDisposingIfCancelled(
    dispatcher: CoroutineDispatcher,
    create: () -> T,
    dispose: (T) -> Unit,
): T {
    var created: T? = null
    try {
        withContext(dispatcher) { created = create() }
    } catch (exception: CancellationException) {
        created?.let { disposeInBackground(dispatcher, listOf(it), dispose) }
        throw exception
    }
    return checkNotNull(created)
}

/**
 * Runs [create] for each index below [count] on [dispatcher], one after the other, stopping as soon as the caller is
 * cancelled. If it stops, or [create] throws, what it created by then is disposed in the background
 * ([disposeInBackground]); a [VirtualMachineError] leaves it as it is, since running more of the project's code could
 * only make that worse.
 *
 * @return What [create] made for each index, null where it made nothing.
 */
public suspend fun <T : Any> createEachDisposingIfCancelled(
    dispatcher: CoroutineDispatcher,
    count: Int,
    create: (index: Int) -> T?,
    dispose: (T) -> Unit,
): List<T?> {
    val created = ArrayList<T?>(count)
    try {
        withContext(dispatcher) {
            repeat(count) { index ->
                ensureActive()
                created += create(index)
            }
        }
    } catch (error: VirtualMachineError) {
        throw error
    } catch (throwable: Throwable) {
        disposeInBackground(dispatcher, created.filterNotNull(), dispose)
        throw throwable
    }
    return created
}

/**
 * Disposes [values] on [dispatcher] without waiting for it, each on its own: nothing waits for them any more, so what
 * the project's code throws while disposing them is ignored rather than taking the place of why they were left.
 */
public fun <T> disposeInBackground(dispatcher: CoroutineDispatcher, values: Collection<T>, dispose: (T) -> Unit) {
    if (values.isEmpty()) return
    CoroutineScope(dispatcher).launch {
        values.forEach { value ->
            try {
                dispose(value)
            } catch (_: Exception) {
            } catch (_: LinkageError) {
            }
        }
    }
}
