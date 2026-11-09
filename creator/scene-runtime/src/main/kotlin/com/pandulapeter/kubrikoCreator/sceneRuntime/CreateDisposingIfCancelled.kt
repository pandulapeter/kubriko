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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs [create] on [dispatcher] (off the main thread, where what the project's code throws can't take the user
 * interface down with it), and if the caller was cancelled meanwhile, disposes what it created before the cancellation
 * goes on, rather than throwing it away undisposed ([createEachDisposingIfCancelled] for one value).
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
 * cancelled. If it stops, or [create] throws, what it created by then is disposed ([disposeEach]): in [disposalScope]
 * without waiting for it, or before the failure goes on if that is null (in the caller's context, as the values would
 * have been disposed had they been handed over). A [VirtualMachineError] leaves them as they are, since running more of
 * the project's code could only make it worse.
 *
 * @return What [create] made for each index, null where it made nothing.
 */
public suspend fun <T : Any> createEachDisposingIfCancelled(
    dispatcher: CoroutineDispatcher,
    count: Int,
    create: (index: Int) -> T?,
    dispose: (T) -> Unit,
    disposalScope: CoroutineScope? = null,
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
        val leftOver = created.filterNotNull()
        if (disposalScope == null) {
            withContext(NonCancellable) { disposeEach(leftOver, dispose) }
        } else if (leftOver.isNotEmpty()) {
            disposalScope.launch { disposeEach(leftOver, dispose) }
        }
        throw throwable
    }
    return created
}

/**
 * Disposes each of [values], going on with the rest whatever one of them throws, since nothing waits for them any
 * more and what disposing them throws mustn't take the place of why they were left; only a [VirtualMachineError] stops it.
 */
public fun <T> disposeEach(values: List<T>, dispose: (T) -> Unit): Unit = values.forEach { value ->
    try {
        dispose(value)
    } catch (error: VirtualMachineError) {
        throw error
    } catch (_: Throwable) {
    }
}
