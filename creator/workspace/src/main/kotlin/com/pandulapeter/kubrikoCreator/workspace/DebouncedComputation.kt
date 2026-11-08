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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * What [compute] returns, worked out on [computeDispatcher] at once when something starts collecting it, then
 * [debounceInMillis] after the last of [changes], and only while something collects it; null until it is first worked
 * out.
 *
 * @param changes Emits whenever the value may have changed, and never a current value (a StateFlow's first is dropped).
 * @param report Says that a computation failed; the previous value stays, and the next change computes again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T : Any> debouncedComputation(
    changes: Flow<Any?>,
    compute: suspend () -> T,
    scope: CoroutineScope,
    report: (Throwable) -> Unit,
    computeDispatcher: CoroutineDispatcher,
    debounceInMillis: Long,
): StateFlow<T?> = flow {
    var isFirst = true
    // A newer change cancels the delayed or running computation, which is the debounce; the first one isn't delayed, so
    // what shows the value shows it at once.
    emitAll(
        changes.onStart { emit(Unit) }.mapLatest {
            if (!isFirst) delay(debounceInMillis)
            isFirst = false
            try {
                withContext(computeDispatcher) { compute() }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                report(exception)
                null
            }
        }.filterNotNull(),
    )
}.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), null)
