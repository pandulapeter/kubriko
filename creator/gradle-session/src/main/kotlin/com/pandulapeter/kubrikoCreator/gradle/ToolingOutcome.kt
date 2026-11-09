/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * The outcome of a Tooling API operation, which is handed this as its [ResultHandler]. Waiting for it, from a coroutine
 * or a thread, cancels the operation through [cancel] when the wait is cancelled or interrupted, and waits on until Gradle
 * stopped it, so nothing the operation does outlives the wait: for as long as it takes, or at most [stopTimeout].
 */
public class ToolingOutcome<T>(
    private val cancel: () -> Unit,
    private val stopTimeout: Duration = Duration.INFINITE,
) : ResultHandler<T> {
    private val outcome = CompletableFuture<T>()

    /**
     * Whether the operation ended, however it did.
     */
    public val hasEnded: Boolean get() = outcome.isDone

    override fun onComplete(result: T) {
        outcome.complete(result)
    }

    override fun onFailure(failure: GradleConnectionException) {
        outcome.completeExceptionally(failure)
    }

    /**
     * The operation's result, or what it failed with, thrown as it is.
     *
     * @throws CancellationException When the calling coroutine is cancelled, once Gradle stopped the operation.
     */
    public suspend fun await(): T = try {
        outcome.copy().await()
    } catch (exception: CancellationException) {
        cancel()
        withContext(NonCancellable) { withTimeoutOrNull(stopTimeout) { stopped().await() } }
        throw exception
    }

    /**
     * The operation's result, or what it failed with, thrown as it is, blocking the calling thread.
     *
     * @throws InterruptedException When the calling thread is interrupted, once Gradle stopped the operation.
     */
    public fun awaitBlocking(): T = try {
        outcome.get()
    } catch (exception: InterruptedException) {
        cancel()
        val stopped = stopped()
        if (stopTimeout.isInfinite()) stopped.join() else stopped.completeOnTimeout(Unit, stopTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS).join()
        throw exception
    } catch (exception: ExecutionException) {
        throw exception.cause ?: exception
    }

    /**
     * Completes once the operation ended, however it did, without affecting it.
     */
    private fun stopped() = outcome.handle { _, _ -> }
}
