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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

/**
 * A scope of a workspace's own, a child of [parent]'s that one failing coroutine doesn't cancel.
 */
internal fun workspaceScopeOf(parent: CoroutineScope) = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))

/**
 * What opening a workspace has started so far: its [scope], and the resources [track] was given, which a failed opening
 * closes.
 */
internal class WorkspaceOpening(parentScope: CoroutineScope) {
    val scope = workspaceScopeOf(parentScope)
    private val started = ArrayList<AutoCloseable>()

    @Synchronized
    fun <T : AutoCloseable> track(resource: T): T = resource.also(started::add)

    /**
     * Cancels [scope] and closes what was tracked, the last one first, each even when closing another throws.
     */
    fun abandon() {
        scope.cancel()
        val resources = synchronized(this) { started.reversed() }
        var failure: Exception? = null
        resources.forEach { resource ->
            try {
                resource.close()
            } catch (exception: Exception) {
                failure?.addSuppressed(exception) ?: run { failure = exception }
            }
        }
        failure?.let { throw it }
    }
}

/**
 * Opens a workspace with [open], which starts what it runs in the [WorkspaceOpening]'s scope and tracks what it has to
 * close. If opening throws or is cancelled, the scope is cancelled and everything tracked is closed before the exception
 * goes on, so nothing a failed opening started outlives it.
 */
internal suspend fun <T> openWorkspace(
    parentScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    open: suspend (WorkspaceOpening) -> T,
): T {
    val opening = WorkspaceOpening(parentScope)
    try {
        return open(opening)
    } catch (exception: Throwable) {
        try {
            withContext(NonCancellable + ioDispatcher) { opening.abandon() }
        } catch (closingFailure: Exception) {
            exception.addSuppressed(closingFailure)
        }
        throw exception
    }
}
