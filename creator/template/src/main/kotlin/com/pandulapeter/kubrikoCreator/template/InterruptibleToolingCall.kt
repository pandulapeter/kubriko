/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * Waits for the Tooling API operation [start] begins with the handler it is handed. When the waiting thread is
 * interrupted, it [cancel]s the operation and waits on, uninterruptibly, until Gradle stopped it, so nothing the operation
 * does outlives the call, then throws the [InterruptedException]; what the operation fails with is thrown as it is.
 */
internal fun <T> awaitInterruptibly(cancel: () -> Unit, start: (ResultHandler<T>) -> Unit): T {
    val outcome = CompletableFuture<T>()
    start(
        object : ResultHandler<T> {
            override fun onComplete(result: T) {
                outcome.complete(result)
            }

            override fun onFailure(failure: GradleConnectionException) {
                outcome.completeExceptionally(failure)
            }
        },
    )
    try {
        return outcome.get()
    } catch (exception: InterruptedException) {
        cancel()
        outcome.handle { _, _ -> }.join()
        throw exception
    } catch (exception: ExecutionException) {
        throw exception.cause ?: exception
    }
}
