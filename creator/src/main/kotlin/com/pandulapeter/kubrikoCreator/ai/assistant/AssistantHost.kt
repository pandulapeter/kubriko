/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The one running agent ([T] is an [Assistant] outside tests), created on first use and held until it is replaced or
 * Kubriko Creator quits, and the one check of AI features: while they are off, none is created, and one whose creation
 * ends after they went off (or after [close]) is closed at once.
 *
 * Replacing it runs in one ordered sequence ([afterPending]), which [get] waits for, so that what is created always
 * follows the last change. Everything but [close] is confined to [scope]'s thread.
 *
 * @param scope Where the sequence runs: the main thread.
 * @param create Starts a new agent.
 */
internal class AssistantHost<T : AutoCloseable>(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val isAiEnabled: StateFlow<Boolean>,
    private val create: suspend () -> T,
) {
    private val lock = Mutex()
    private val _current = MutableStateFlow<T?>(null)

    /**
     * The running agent, if there is one.
     */
    val current = _current.asStateFlow()
    private var lifecycle: Job? = null

    @Volatile
    private var isClosed = false

    /**
     * Whether AI features are on.
     */
    val isOn get() = isAiEnabled.value

    /**
     * The running agent, created if there is none, once the sequence queued so far has run.
     *
     * @throws AgentClosedException If AI features went off, or the host was closed, while it was created.
     * @throws IllegalStateException If AI features are off.
     */
    suspend fun get(): T {
        awaitPending()
        return lock.withLock {
            if (isClosed) throw AgentClosedException()
            check(isAiEnabled.value) { "AI features are off." }
            _current.value ?: create().also { created ->
                _current.value = created
                if (!isAiEnabled.value || isClosed) {
                    if (_current.compareAndSet(created, null)) withContext(ioDispatcher) { created.close() }
                    throw AgentClosedException()
                }
            }
        }
    }

    /**
     * Closes the running agent (only if it is still [expected], when given), once one being created is done.
     */
    suspend fun replace(expected: T? = null) {
        val replaced = lock.withLock {
            _current.value?.takeIf { expected == null || it === expected }?.also { _current.value = null }
        } ?: return
        withContext(ioDispatcher) { replaced.close() }
    }

    /**
     * Runs [block] after the steps queued before it, so that they never interleave; a failing step doesn't stop the next.
     */
    fun afterPending(block: suspend () -> Unit) {
        val previous = lifecycle
        lifecycle = scope.launch {
            previous?.join()
            runIgnoringFailure { block() }
        }
    }

    /**
     * Waits until every step queued so far, and every one queued meanwhile, has run.
     */
    suspend fun awaitPending() {
        while (lifecycle?.isCompleted == false) lifecycle?.join()
    }

    /**
     * Closes the running agent and refuses to create another one, when Kubriko Creator quits; from any thread.
     */
    fun close() {
        isClosed = true
        _current.getAndUpdate { null }?.close()
    }
}
