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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Runs [create] on [dispatcher] (off the main thread, where what the project's code throws can't take the user
 * interface down with it), and if the caller was cancelled meanwhile, disposes what it created on [dispatcher] before
 * the cancellation goes on, rather than throwing it away undisposed: what [createEachDisposingIfCancelled] does for a
 * single value.
 *
 * @throws Exception What [create] threw, and [LinkageError]s too (a project class that is gone or changed since).
 */
public suspend fun <T : Any> createDisposingIfCancelled(
    dispatcher: CoroutineDispatcher,
    create: () -> T,
    dispose: (T) -> Unit,
): T = checkNotNull(createEachDisposingIfCancelled(dispatcher, count = 1, create = { create() }, dispose = dispose).single())

/**
 * Runs [create] for each index below [count] on [dispatcher], one after the other, stopping as soon as the caller is
 * cancelled. If it stops, or [create] throws, what it created by then is disposed on [dispatcher] ([disposeEach])
 * before the failure goes on, so it is gone before anything the caller does next; a [VirtualMachineError] leaves it as
 * it is, since running more of the project's code could only make that worse.
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
        disposeLeftOver(throwable, dispatcher, created.filterNotNull(), dispose)
        throw throwable
    }
    return created
}

/**
 * Disposes [values], left over because of [cause], on [dispatcher] even though the caller may be cancelled
 * ([disposeEach]), waiting until it is done, so that they are gone before anything the caller does next. What stops
 * it from being done (a [dispatcher] that no longer takes work) is added to [cause] as suppressed, so it never takes
 * the place of why they were left.
 */
public suspend fun <T> disposeLeftOver(cause: Throwable, dispatcher: CoroutineDispatcher, values: List<T>, dispose: (T) -> Unit) {
    if (values.isEmpty()) return
    try {
        withContext(NonCancellable + dispatcher) { disposeEach(values, dispose) }
    } catch (error: VirtualMachineError) {
        throw error
    } catch (throwable: Throwable) {
        if (throwable !== cause) cause.addSuppressed(throwable)
    }
}

/**
 * Disposes each of [values], going on with the rest whatever one of them throws, since nothing uses them any more and
 * what disposing them throws mustn't take the place of why they were left. A [VirtualMachineError] stops it and is
 * thrown on, as running more of the project's code could only make it worse.
 */
public fun <T> disposeEach(values: List<T>, dispose: (T) -> Unit) {
    for (value in values) {
        try {
            dispose(value)
        } catch (error: VirtualMachineError) {
            throw error
        } catch (_: Throwable) {
        }
    }
}
